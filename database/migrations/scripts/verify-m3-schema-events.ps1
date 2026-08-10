[CmdletBinding()]
param(
    [ValidateRange(1024, 65535)]
    [int]$PostgresPort = 15434,
    [ValidateRange(1024, 65535)]
    [int]$NatsClientPort = 14334,
    [ValidateRange(1024, 65535)]
    [int]$NatsMonitorPort = 18334,
    [switch]$KeepOnFailure
)

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
$composeFile = Join-Path $repoRoot 'compose.yaml'
$migrationDirectory = Join-Path $repoRoot 'database\migrations'
$foundationFixture = Join-Path $migrationDirectory 'fixtures\verify-foundation.sql'
$m2Fixture = Join-Path $migrationDirectory 'fixtures\verify-m2-schema-auth.sql'
$cmsFixture = Join-Path $migrationDirectory 'fixtures\verify-m2-cms.sql'
$m3Fixture = Join-Path $migrationDirectory 'fixtures\verify-m3-schema-events.sql'
$projectName = 'nexora-m3-db01-draft-verify'
$cleanupRequired = $false
$succeeded = $false

function Invoke-Compose {
    param([Parameter(Mandatory)][string[]]$Arguments)

    & docker compose --project-name $projectName -f $composeFile @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose failed: $($Arguments -join ' ')"
    }
}

function Invoke-PsqlText {
    param([Parameter(Mandatory)][string]$Sql)

    $Sql | & docker compose --project-name $projectName -f $composeFile exec -T postgres `
        psql -X -v ON_ERROR_STOP=1 -U nexora -d nexora
    if ($LASTEXITCODE -ne 0) {
        throw 'psql command failed'
    }
}

try {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw 'Docker is required for the local PostgreSQL 17.5 migration verification.'
    }

    foreach ($requiredFile in @(
        $composeFile,
        $foundationFixture,
        $m2Fixture,
        $cmsFixture,
        $m3Fixture
    )) {
        if (-not (Test-Path -LiteralPath $requiredFile -PathType Leaf)) {
            throw "Missing verification input: $requiredFile"
        }
    }

    $migrationFiles = Get-ChildItem -LiteralPath $migrationDirectory -File -Filter 'V*__*.sql' |
        Sort-Object Name
    if ($migrationFiles.Count -ne 14) {
        throw "Expected exactly fourteen ordered migrations through draft M3-DB01; found $($migrationFiles.Count)"
    }

    $env:NEXORA_POSTGRES_PORT = $PostgresPort
    $env:NEXORA_NATS_CLIENT_PORT = $NatsClientPort
    $env:NEXORA_NATS_MONITOR_PORT = $NatsMonitorPort
    $cleanupRequired = $true
    Invoke-Compose -Arguments @('up', '--detach', '--wait')

    # Local-only stand-ins exercise the documented Supabase policy expression.
    # They are disposable test inputs, never migration-owned managed objects and
    # never evidence of a hosted provider call.
    Invoke-PsqlText @'
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
    CREATE ROLE anon NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION NOBYPASSRLS;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
    CREATE ROLE authenticated NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION NOBYPASSRLS;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'service_role') THEN
    CREATE ROLE service_role NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION NOBYPASSRLS;
  END IF;
END
$$;

CREATE SCHEMA auth;
CREATE SCHEMA realtime;

CREATE FUNCTION auth.uid()
RETURNS uuid
LANGUAGE sql
STABLE
AS $$
  SELECT NULLIF(current_setting('request.jwt.claim.sub', true), '')::uuid
$$;

CREATE FUNCTION realtime.topic()
RETURNS text
LANGUAGE sql
STABLE
AS $$
  SELECT NULLIF(current_setting('realtime.topic', true), '')
$$;

CREATE TABLE realtime.messages (
  id uuid PRIMARY KEY,
  topic text NOT NULL,
  payload jsonb NOT NULL DEFAULT '{}'::jsonb
);
ALTER TABLE realtime.messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE realtime.messages FORCE ROW LEVEL SECURITY;

REVOKE ALL ON SCHEMA auth, realtime FROM PUBLIC;
REVOKE ALL ON ALL FUNCTIONS IN SCHEMA auth, realtime FROM PUBLIC;
REVOKE ALL ON TABLE realtime.messages FROM PUBLIC;
GRANT USAGE ON SCHEMA auth, realtime TO authenticated;
GRANT EXECUTE ON FUNCTION auth.uid(), realtime.topic() TO authenticated;
GRANT SELECT, INSERT ON realtime.messages TO authenticated;
'@

    $m3Migration = $migrationFiles | Where-Object Name -EQ 'V014__outbox_events_and_private_realtime_policy.sql'
    $preM3Migrations = $migrationFiles | Where-Object Name -NE 'V014__outbox_events_and_private_realtime_policy.sql'
    if (@($m3Migration).Count -ne 1) {
        throw 'Expected exactly one V014 M3-DB01 migration.'
    }

    foreach ($migration in $preM3Migrations) {
        Write-Output "Applying $($migration.Name)"
        Invoke-PsqlText (Get-Content -LiteralPath $migration.FullName -Raw)
    }

    Write-Output 'Running M1 role/schema boundary fixture'
    Invoke-PsqlText (Get-Content -LiteralPath $foundationFixture -Raw)
    Write-Output 'Running M2 tenant/RBAC/RLS fixture'
    Invoke-PsqlText (Get-Content -LiteralPath $m2Fixture -Raw)
    Write-Output 'Running M2 CMS/immutable-history/RLS fixture'
    Invoke-PsqlText (Get-Content -LiteralPath $cmsFixture -Raw)

    Write-Output "Applying $($m3Migration.Name)"
    Invoke-PsqlText (Get-Content -LiteralPath $m3Migration.FullName -Raw)
    Write-Output 'Running M3 outbox/private-Realtime fixture'
    Invoke-PsqlText (Get-Content -LiteralPath $m3Fixture -Raw)

    $succeeded = $true
    Write-Output "M3-DB01 isolated draft verification passed on PostgreSQL 17.5 via Compose project $projectName."
}
finally {
    if ($cleanupRequired -and ($succeeded -or -not $KeepOnFailure)) {
        try {
            Invoke-Compose -Arguments @('down', '--volumes', '--remove-orphans')
            Write-Output "Removed disposable Compose project $projectName."
        }
        catch {
            if ($succeeded) {
                throw "Disposable Compose cleanup failed after successful verification: $($_.Exception.Message)"
            }
            Write-Warning "Disposable Compose cleanup also failed: $($_.Exception.Message)"
        }
    }
    elseif ($cleanupRequired) {
        Write-Warning "Verification failed; preserving $projectName for inspection. Run: docker compose --project-name $projectName -f `"$composeFile`" down --volumes --remove-orphans"
    }
}
