[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $repoRoot

$requiredFiles = @(
    '.editorconfig', '.gitattributes', '.nvmrc', '.java-version', '.go-version',
    '.tool-versions', 'LICENSE', 'NOTICE', 'Makefile', 'compose.yaml',
    'THIRD-PARTY-NOTICES.md', 'README.md'
)
$requiredFiles | ForEach-Object {
    if (-not (Test-Path -LiteralPath $_ -PathType Leaf)) { throw "Missing foundation file: $_" }
}

$requiredDirs = @('apps/web', 'apps/platform-api', 'services', 'packages', 'database', 'infrastructure', 'observability', 'docs')
$requiredDirs | ForEach-Object {
    if (-not (Test-Path -LiteralPath $_ -PathType Container)) { throw "Missing skeleton directory: $_" }
}

foreach ($ignoredPath in @('.env.local', 'engineer/', '.worktrees/', '.agentkit/state/')) {
    & git check-ignore --quiet --no-index -- $ignoredPath
    if ($LASTEXITCODE -ne 0) { throw "Required ignored path is not ignored: $ignoredPath" }
}

$forbidden = @('package.json', 'pnpm-workspace.yaml', 'pnpm-lock.yaml', '.npmrc')
foreach ($path in $forbidden) {
    if (Test-Path -LiteralPath $path) { throw "M1-T01 must not create Node dependency control: $path" }
}

$secretPattern = '(?i)sk-[a-z0-9]{20,}|ghp_[a-z0-9]{36}|github_pat_[a-z0-9_]{20,}|AKIA[0-9A-Z]{16}|-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----|postgres(?:ql)?://[^/\s:@]+:[^@\s]+@'
$secretFindings = foreach ($trackedPath in (& git ls-files)) {
    $content = Get-Content -LiteralPath $trackedPath -Raw
    if ($content -match $secretPattern) { $trackedPath }
}
if ($secretFindings) { throw "Potential credential material found in tracked files: $($secretFindings -join ', ')" }

& docker compose -f compose.yaml config --quiet
if ($LASTEXITCODE -ne 0) { throw 'Compose configuration did not render' }

Write-Output 'Nexora M1-T01 foundation validation passed (static/configuration checks only).'
