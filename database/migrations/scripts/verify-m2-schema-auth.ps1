[CmdletBinding()]
param(
    [ValidateRange(1024, 65535)]
    [int]$PostgresPort = 15433,
    [ValidateRange(1024, 65535)]
    [int]$NatsClientPort = 14333,
    [ValidateRange(1024, 65535)]
    [int]$NatsMonitorPort = 18333,
    [switch]$KeepOnFailure
)

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
$composeFile = Join-Path $repoRoot 'compose.yaml'
$migrationDirectory = Join-Path $repoRoot 'database\migrations'
$foundationFixture = Join-Path $migrationDirectory 'fixtures\verify-foundation.sql'
$m2Fixture = Join-Path $migrationDirectory 'fixtures\verify-m2-schema-auth.sql'
$cmsFixture = Join-Path $migrationDirectory 'fixtures\verify-m2-cms.sql'
$projectName = 'nexora-m2-db02-verify'
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

    foreach ($requiredFile in @($composeFile, $foundationFixture, $m2Fixture, $cmsFixture)) {
        if (-not (Test-Path -LiteralPath $requiredFile -PathType Leaf)) {
            throw "Missing verification input: $requiredFile"
        }
    }

    $migrationFiles = Get-ChildItem -LiteralPath $migrationDirectory -File -Filter 'V*__*.sql' |
        Sort-Object Name
    if ($migrationFiles.Count -ne 10) {
        throw "Expected exactly ten ordered migrations through M2-DB02; found $($migrationFiles.Count)"
    }

    $env:NEXORA_POSTGRES_PORT = $PostgresPort
    $env:NEXORA_NATS_CLIENT_PORT = $NatsClientPort
    $env:NEXORA_NATS_MONITOR_PORT = $NatsMonitorPort
    $cleanupRequired = $true
    Invoke-Compose -Arguments @('up', '--detach', '--wait')

    # Local-only stand-ins prove explicit revokes for the platform role names.
    # No provider endpoint, hosted project, credential, or Supabase CLI is used.
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
'@

    foreach ($migration in $migrationFiles) {
        Write-Output "Applying $($migration.Name)"
        Invoke-PsqlText (Get-Content -LiteralPath $migration.FullName -Raw)
    }

    Write-Output 'Running M1 role/schema boundary fixture'
    Invoke-PsqlText (Get-Content -LiteralPath $foundationFixture -Raw)
    Write-Output 'Running M2 tenant/RBAC/RLS fixture'
    Invoke-PsqlText (Get-Content -LiteralPath $m2Fixture -Raw)
    Write-Output 'Running M2 CMS/immutable-history/RLS fixture'
    Invoke-PsqlText (Get-Content -LiteralPath $cmsFixture -Raw)

    $succeeded = $true
    Write-Output "M2-DB02 local verification passed on PostgreSQL 17.5 via Compose project $projectName."
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
