<#!
.SYNOPSIS
Starts the Signal Notes local development stack.

.DESCRIPTION
Starts or reuses MySQL, Spring Boot, and Vite on the fixed development ports.
Use -NoBrowser when running from automation or a remote shell.
#>
[CmdletBinding()]
param(
    [switch]$NoBrowser,
    [switch]$SkipMysql
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = [IO.Path]::GetFullPath($PSScriptRoot)
$commonPath = Join-Path $projectRoot 'scripts/blog-dev-common.ps1'
. $commonPath

$runtimeRoot = Join-Path $projectRoot '.runtime'
$logRoot = Join-Path $runtimeRoot 'logs'
$pidRoot = Join-Path $runtimeRoot 'pids'
$backendRoot = Join-Path $projectRoot 'backend'
$frontendRoot = Join-Path $projectRoot 'frontend'
$composePath = Join-Path $projectRoot 'docker-compose.yml'
$dotenvPath = Join-Path $projectRoot '.env'

$backendPort = 8081
$frontendPort = 5174
$mysqlPort = 3307
$frontendUrl = "http://127.0.0.1:$frontendPort/"
$backendHealthUrl = "http://127.0.0.1:$backendPort/actuator/health"

$null = New-Item -ItemType Directory -Force -Path $logRoot, $pidRoot
$startedRecords = New-Object System.Collections.ArrayList

function Write-BlogStatus {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Message,
        [ConsoleColor]$Color = [ConsoleColor]::Gray
    )

    Write-Host $Message -ForegroundColor $Color
}

function Assert-BlogCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,
        [Parameter(Mandatory = $true)]
        [string]$InstallHint
    )

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Missing dependency '$Name'. Install or add it to PATH: $InstallHint"
    }
}

function Get-BlogListenerDescription {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Port
    )

    $listeners = @(Get-BlogPortListeners -Port $Port)
    if ($listeners.Count -eq 0) {
        return "port $Port"
    }

    $parts = foreach ($listener in $listeners) {
        $name = 'unknown'
        try {
            $name = (Get-Process -Id ([int]$listener.OwningProcess) -ErrorAction Stop).ProcessName
        } catch {
        }
        "$name (PID $($listener.OwningProcess), $($listener.LocalAddress))"
    }
    return ($parts -join '; ')
}

function Remove-StaleBlogRecords {
    foreach ($path in @(
        (Join-Path $pidRoot 'backend.json'),
        (Join-Path $pidRoot 'frontend.json')
    )) {
        $record = Read-BlogProcessRecord -Path $path
        if ($record -and -not (Test-BlogManagedProcess -Record $record)) {
            Remove-BlogProcessRecord -Path $path
        }
    }
}

function Stop-NewBlogProcesses {
    for ($index = $startedRecords.Count - 1; $index -ge 0; $index--) {
        $path = $startedRecords[$index]
        try {
            $result = Stop-BlogManagedProcess -RecordPath $path
            Write-BlogStatus "Cleanup $([IO.Path]::GetFileNameWithoutExtension($path)): $result" DarkGray
        } catch {
            Write-BlogStatus "Could not clean up ${path}: $($_.Exception.Message)" Yellow
        }
    }
}

try {
    Write-BlogStatus 'Signal Notes development startup' Cyan
    Assert-BlogCommand -Name 'docker' -InstallHint 'Docker Desktop'
    Assert-BlogCommand -Name 'java' -InstallHint 'JDK 21 or newer'
    Assert-BlogCommand -Name 'mvn' -InstallHint 'Apache Maven'
    Assert-BlogCommand -Name 'node' -InstallHint 'Node.js (includes npm)'

    $npmCommand = Get-Command npm.cmd -ErrorAction SilentlyContinue
    if (-not $npmCommand) {
        $npmCommand = Get-Command npm -ErrorAction SilentlyContinue
    }
    $npmPath = if ($npmCommand) { [string]$npmCommand.Source } else { '' }
    if ([string]::IsNullOrWhiteSpace($npmPath) -and $npmCommand) {
        $npmPath = [string]$npmCommand.Path
    }
    $frontendCommand = Get-BlogFrontendCommand -NpmPath $npmPath -Port $frontendPort
    Write-BlogStatus "Frontend runner: $($frontendCommand.Mode)" DarkGray

    foreach ($requiredPath in @($composePath, $backendRoot, $frontendRoot)) {
        if (-not (Test-Path -LiteralPath $requiredPath)) {
            throw "Required project path is missing: $requiredPath"
        }
    }

    $composeVersion = & docker compose version 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Docker Compose is unavailable: $composeVersion"
    }

    Remove-StaleBlogRecords

    if ($SkipMysql) {
        Write-BlogStatus "Skipping Compose MySQL startup; checking 127.0.0.1:$mysqlPort" DarkYellow
        $mysqlReady = Wait-BlogCondition -Description 'MySQL port' -TimeoutSec 15 -Condition {
            Test-NetConnection -ComputerName '127.0.0.1' -Port $mysqlPort -InformationLevel Quiet -WarningAction SilentlyContinue
        }
        if (-not $mysqlReady) {
            throw "MySQL is not reachable on 127.0.0.1:$mysqlPort. Remove -SkipMysql or start MySQL first."
        }
    } else {
        Write-BlogStatus 'Starting or reusing MySQL container...' Gray
        $oldMysqlPort = [Environment]::GetEnvironmentVariable('MYSQL_PORT', 'Process')
        try {
            $env:MYSQL_PORT = [string]$mysqlPort
            $savedErrorPreference = $ErrorActionPreference
            $ErrorActionPreference = 'Continue'
            try {
                $composeOutput = @(& docker compose up -d mysql 2>&1)
                $composeExitCode = $LASTEXITCODE
            } finally {
                $ErrorActionPreference = $savedErrorPreference
            }
            if ($composeExitCode -ne 0) {
                $composeOutput | ForEach-Object { Write-Host $_ }
                throw 'Docker Compose could not start MySQL.'
            }
        } finally {
            if ($null -eq $oldMysqlPort) {
                Remove-Item Env:MYSQL_PORT -ErrorAction SilentlyContinue
            } else {
                $env:MYSQL_PORT = $oldMysqlPort
            }
        }

        $mysqlReady = Wait-BlogCondition -Description 'MySQL container health' -TimeoutSec 90 -Condition {
            $health = (& docker inspect --format '{{.State.Health.Status}}' signal-notes-mysql 2>$null | Out-String).Trim()
            $health -eq 'healthy'
        }
        if (-not $mysqlReady) {
            & docker compose logs --tail 40 mysql 2>&1 | ForEach-Object { Write-Host $_ }
            throw 'MySQL did not become healthy.'
        }
    }
    Write-BlogStatus "MySQL ready on 127.0.0.1:$mysqlPort" Green

    $dotenv = Get-BlogDotEnv -Path $dotenvPath
    $backendEnvironment = @{
        DB_HOST     = '127.0.0.1'
        DB_PORT     = [string]$mysqlPort
        SERVER_PORT = [string]$backendPort
    }
    foreach ($name in @('DB_NAME', 'DB_USERNAME', 'DB_PASSWORD', 'ADMIN_USERNAME', 'ADMIN_PASSWORD')) {
        if ($dotenv.ContainsKey($name)) {
            $backendEnvironment[$name] = [string]$dotenv[$name]
        }
    }
    if ($dotenv.ContainsKey('CORS_ORIGINS')) {
        $corsOrigins = [string]$dotenv.CORS_ORIGINS
        if ($corsOrigins -notmatch '127\.0\.0\.1:5174') {
            $corsOrigins = "$corsOrigins,http://127.0.0.1:5174,http://localhost:5174"
        }
        $backendEnvironment.CORS_ORIGINS = $corsOrigins
    }

    $backendPidPath = Join-Path $pidRoot 'backend.json'
    $backendRecord = Read-BlogProcessRecord -Path $backendPidPath
    $backendProbe = Invoke-BlogHttpProbe -Uri $backendHealthUrl
    $backendHealthy = $backendProbe.Success -and ($backendProbe.Content -match '"status"\s*:\s*"UP"')
    $backendListeners = @(Get-BlogPortListeners -Port $backendPort)
    $backendDecision = Get-BlogPortDecision -Listening:($backendListeners.Count -gt 0) -Healthy:$backendHealthy -Recognized:$backendHealthy

    if ($backendDecision -eq 'reuse') {
        Write-BlogStatus "Backend already ready on 127.0.0.1:$backendPort" Green
    } else {
        if ($backendDecision -eq 'conflict') {
            throw "Backend port $backendPort is occupied by $(Get-BlogListenerDescription -Port $backendPort). Stop that service or free the port."
        }

        Write-BlogStatus "Starting Spring Boot backend on 127.0.0.1:$backendPort..." Gray
        $mvnCommand = (Get-Command mvn -ErrorAction Stop).Source
        $backendProcess = Start-BlogChildProcess -FilePath $mvnCommand -ArgumentList @('spring-boot:run') -WorkingDirectory $backendRoot -StandardOutputPath (Join-Path $logRoot 'backend.log') -StandardErrorPath (Join-Path $logRoot 'backend-error.log') -Environment $backendEnvironment
        $null = $startedRecords.Add($backendPidPath)
        Write-BlogProcessRecord -Path $backendPidPath -Service 'backend' -Process $backendProcess -WorkingDirectory $backendRoot -Port $backendPort -CommandPattern 'mvn' | Out-Null

        $backendReady = Wait-BlogCondition -Description 'Spring Boot health endpoint' -TimeoutSec 90 -Condition {
            $probe = Invoke-BlogHttpProbe -Uri $backendHealthUrl
            $probe.Success -and ($probe.Content -match '"status"\s*:\s*"UP"')
        }
        if (-not $backendReady) {
            Show-BlogLogTail -Path (Join-Path $logRoot 'backend-error.log')
            Show-BlogLogTail -Path (Join-Path $logRoot 'backend.log')
            throw "Backend did not become healthy on port $backendPort."
        }
        Write-BlogStatus "Backend ready on 127.0.0.1:$backendPort" Green
    }

    $frontendPidPath = Join-Path $pidRoot 'frontend.json'
    $frontendProbe = Invoke-BlogHttpProbe -Uri $frontendUrl
    $frontendHealthy = $frontendProbe.Success -and (Test-SignalNotesFrontendHtml -Html $frontendProbe.Content)
    $frontendListeners = @(Get-BlogPortListeners -Port $frontendPort)
    $frontendDecision = Get-BlogPortDecision -Listening:($frontendListeners.Count -gt 0) -Healthy:$frontendHealthy -Recognized:$frontendHealthy

    if ($frontendDecision -eq 'reuse') {
        Write-BlogStatus "Frontend already ready at $frontendUrl" Green
    } else {
        if ($frontendDecision -eq 'conflict') {
            throw "Frontend port $frontendPort is occupied by $(Get-BlogListenerDescription -Port $frontendPort). Stop that service or free the port."
        }

        Write-BlogStatus "Starting Vite frontend at $frontendUrl..." Gray
        $frontendProcess = Start-BlogChildProcess -FilePath $frontendCommand.FilePath -ArgumentList $frontendCommand.ArgumentList -WorkingDirectory $frontendRoot -StandardOutputPath (Join-Path $logRoot 'frontend.log') -StandardErrorPath (Join-Path $logRoot 'frontend-error.log')
        $null = $startedRecords.Add($frontendPidPath)
        Write-BlogProcessRecord -Path $frontendPidPath -Service 'frontend' -Process $frontendProcess -WorkingDirectory $frontendRoot -Port $frontendPort -CommandPattern $frontendCommand.CommandPattern | Out-Null

        $frontendReady = Wait-BlogCondition -Description 'Vite frontend' -TimeoutSec 60 -Condition {
            $probe = Invoke-BlogHttpProbe -Uri $frontendUrl
            $probe.Success -and (Test-SignalNotesFrontendHtml -Html $probe.Content)
        }
        if (-not $frontendReady) {
            Show-BlogLogTail -Path (Join-Path $logRoot 'frontend-error.log')
            Show-BlogLogTail -Path (Join-Path $logRoot 'frontend.log')
            throw "Frontend did not become ready on port $frontendPort."
        }
        Write-BlogStatus "Frontend ready at $frontendUrl" Green
    }

    Write-Host ''
    Write-BlogStatus 'Signal Notes is ready.' Green
    Write-Host "Blog:    $frontendUrl"
    Write-Host "Admin:   ${frontendUrl}admin"
    Write-Host "Health:  $backendHealthUrl"
    Write-Host "Logs:    $logRoot"
    Write-Host 'Stop:    .\stop-blog.cmd'

    if (-not $NoBrowser) {
        try {
            Start-Process $frontendUrl | Out-Null
        } catch {
            Write-BlogStatus "Could not open the browser automatically. Use $frontendUrl" Yellow
        }
    }
} catch {
    Write-BlogStatus "Startup failed: $($_.Exception.Message)" Red
    Stop-NewBlogProcesses
    exit 1
}
