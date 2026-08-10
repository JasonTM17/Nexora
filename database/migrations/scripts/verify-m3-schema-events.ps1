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
    if ($migrationFiles.Count -ne 17) {
        throw "Expected exactly seventeen ordered migrations through scoped M3-DB01; found $($migrationFiles.Count)"
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
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'nexora_migrator') THEN
    CREATE ROLE nexora_migrator NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION NOBYPASSRLS;
  END IF;
END
$$;

CREATE SCHEMA auth;
CREATE SCHEMA realtime;

CREATE FUNCTION auth.jwt()
RETURNS jsonb
LANGUAGE sql
STABLE
AS $$
  SELECT COALESCE(NULLIF(current_setting('request.jwt.claims', true), '')::jsonb, '{}'::jsonb)
$$;

CREATE FUNCTION auth.uid()
RETURNS uuid
LANGUAGE sql
STABLE
AS $$
  SELECT NULLIF(auth.jwt() ->> 'sub', '')::uuid
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
  extension text NOT NULL CHECK (extension IN ('broadcast', 'presence')),
  payload jsonb NOT NULL DEFAULT '{}'::jsonb
);

-- Preload rows before RLS is enabled. They simulate distinct private channel
-- rows only; no migration owns this disposable provider stand-in.
INSERT INTO realtime.messages (id, topic, extension, payload) VALUES
  ('60000000-0000-4000-8000-000000000001', 'tenant:10000000-0000-4000-8000-000000000001:publication', 'broadcast', '{"eventId":"50000000-0000-4000-8000-000000000001"}'::jsonb),
  ('60000000-0000-4000-8000-000000000002', 'tenant:10000000-0000-4000-8000-000000000001:workflow', 'broadcast', '{"eventId":"50000000-0000-4000-8000-000000000002"}'::jsonb),
  ('60000000-0000-4000-8000-000000000003', 'tenant:10000000-0000-4000-8000-000000000002:publication', 'broadcast', '{"eventId":"50000000-0000-4000-8000-000000000003"}'::jsonb),
  ('60000000-0000-4000-8000-000000000004', 'resource:70000000-0000-4000-8000-000000000001:presence', 'presence', '{"state":"viewing"}'::jsonb),
  ('60000000-0000-4000-8000-000000000005', 'resource:50000000-0000-4000-8000-000000000010:job-progress', 'broadcast', '{"eventId":"50000000-0000-4000-8000-000000000002"}'::jsonb);
ALTER TABLE realtime.messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE realtime.messages FORCE ROW LEVEL SECURITY;

REVOKE ALL ON SCHEMA auth, realtime FROM PUBLIC;
REVOKE ALL ON ALL FUNCTIONS IN SCHEMA auth, realtime FROM PUBLIC;
REVOKE ALL ON TABLE realtime.messages FROM PUBLIC;
GRANT USAGE ON SCHEMA auth, realtime TO nexora_migrator;
GRANT EXECUTE ON FUNCTION auth.jwt(), auth.uid(), realtime.topic() TO nexora_migrator;
GRANT USAGE ON SCHEMA auth, realtime TO authenticated;
GRANT EXECUTE ON FUNCTION auth.jwt(), auth.uid(), realtime.topic() TO authenticated;
GRANT SELECT, INSERT ON realtime.messages TO authenticated;
'@

    $m3Migrations = @($migrationFiles | Where-Object Name -in @(
        'V014__outbox_events_and_private_realtime_policy.sql',
        'V015__scoped_realtime_channel_authorization.sql',
        'V016__realtime_presence_resource_projection.sql',
        'V017__realtime_projection_trigger_privileges.sql'
    ))
    $preM3Migrations = @($migrationFiles | Where-Object Name -notin @(
        'V014__outbox_events_and_private_realtime_policy.sql',
        'V015__scoped_realtime_channel_authorization.sql',
        'V016__realtime_presence_resource_projection.sql',
        'V017__realtime_projection_trigger_privileges.sql'
    ))
    if ($m3Migrations.Count -ne 4) {
        throw 'Expected ordered V014 through V017 M3-DB01 migrations.'
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

    foreach ($migration in $m3Migrations) {
        Write-Output "Applying $($migration.Name)"
        Invoke-PsqlText (Get-Content -LiteralPath $migration.FullName -Raw)
    }
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
