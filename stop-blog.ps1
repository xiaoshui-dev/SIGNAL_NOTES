<#!
.SYNOPSIS
Stops Signal Notes frontend and backend processes started by start-blog.ps1.

.DESCRIPTION
Only verified PID records created by the launcher are stopped. MySQL, Docker
volumes, and unrelated services such as Traefik on port 8080 are preserved.
#>
[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = [IO.Path]::GetFullPath($PSScriptRoot)
. (Join-Path $projectRoot 'scripts/blog-dev-common.ps1')

$pidRoot = Join-Path $projectRoot '.runtime/pids'
$records = @(
    [pscustomobject]@{ Name = 'frontend'; Path = (Join-Path $pidRoot 'frontend.json') }
    [pscustomobject]@{ Name = 'backend'; Path = (Join-Path $pidRoot 'backend.json') }
)

Write-Host 'Stopping Signal Notes development processes...' -ForegroundColor Cyan
foreach ($item in $records) {
    $result = Stop-BlogManagedProcess -RecordPath $item.Path
    switch ($result) {
        'stopped' { Write-Host "$($item.Name): stopped" -ForegroundColor Green }
        'external' { Write-Host "$($item.Name): record did not match its original process; left process untouched" -ForegroundColor Yellow }
        'stale' { Write-Host "$($item.Name): stale record removed" -ForegroundColor DarkGray }
    }
}

Write-Host 'MySQL container and its data were preserved.' -ForegroundColor Gray
Write-Host 'Port 8080 and unrelated processes were not touched.' -ForegroundColor Gray
