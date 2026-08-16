Set-StrictMode -Version Latest

function Get-BlogPortListeners {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Port
    )

    try {
        return @(
            Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction Stop |
                Select-Object LocalAddress, LocalPort, OwningProcess
        )
    } catch {
        return @()
    }
}

function Get-BlogPortDecision {
    param(
        [switch]$Listening,
        [switch]$Healthy,
        [switch]$Recognized
    )

    if (-not $Listening.IsPresent) {
        return 'available'
    }

    if ($Healthy.IsPresent -and $Recognized.IsPresent) {
        return 'reuse'
    }

    return 'conflict'
}

function Test-SignalNotesFrontendHtml {
    param(
        [AllowEmptyString()]
        [string]$Html
    )

    return $Html -match '<title>\s*[^<]*Signal Notes\s*</title>'
}

function Get-BlogDotEnv {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    $values = @{}
    if (-not (Test-Path -LiteralPath $Path)) {
        return $values
    }

    foreach ($line in Get-Content -LiteralPath $Path) {
        if ($line -notmatch '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$') {
            continue
        }

        $name = $Matches[1]
        $value = $Matches[2]
        if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        $values[$name] = $value
    }

    return $values
}

function ConvertTo-BlogText {
    param(
        [AllowNull()]
        $Content
    )

    if ($null -eq $Content) {
        return ''
    }

    if ($Content -is [byte[]]) {
        return [Text.Encoding]::UTF8.GetString($Content)
    }

    return [string]$Content
}

function Get-BlogFrontendCommand {
    param(
        [AllowEmptyString()]
        [string]$PnpmPath,
        [AllowEmptyString()]
        [string]$NpmPath,
        [int]$Port = 5174
    )

    if (-not [string]::IsNullOrWhiteSpace($PnpmPath)) {
        return [pscustomobject]@{
            Mode         = 'pnpm'
            CommandPattern = 'pnpm'
            FilePath     = $PnpmPath
            ArgumentList = @('dev', '--host', '127.0.0.1', '--port', [string]$Port, '--strictPort')
        }
    }

    if (-not [string]::IsNullOrWhiteSpace($NpmPath)) {
        return [pscustomobject]@{
            Mode         = 'npm'
            CommandPattern = 'npm'
            FilePath     = $NpmPath
            ArgumentList = @('run', 'dev', '--', '--host', '127.0.0.1', '--port', [string]$Port, '--strictPort')
        }
    }

    throw 'No supported frontend package manager was found. Install pnpm or Node.js with npm.'
}

function Invoke-BlogHttpProbe {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Uri,
        [int]$TimeoutSec = 3
    )

    try {
        $response = Invoke-WebRequest -Uri $Uri -UseBasicParsing -TimeoutSec $TimeoutSec -ErrorAction Stop
        return [pscustomobject]@{
            Success    = $true
            StatusCode = [int]$response.StatusCode
            Content    = ConvertTo-BlogText -Content $response.Content
            Error      = $null
        }
    } catch {
        return [pscustomobject]@{
            Success    = $false
            StatusCode = 0
            Content    = ''
            Error      = $_.Exception.Message
        }
    }
}

function Wait-BlogCondition {
    param(
        [Parameter(Mandatory = $true)]
        [scriptblock]$Condition,
        [Parameter(Mandatory = $true)]
        [string]$Description,
        [int]$TimeoutSec = 60,
        [int]$IntervalSec = 1
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        try {
            if (& $Condition) {
                return $true
            }
        } catch {
            # The service can still be starting; the next probe may succeed.
        }

        Start-Sleep -Seconds $IntervalSec
    }

    Write-Warning "$Description timed out after $TimeoutSec seconds."
    return $false
}

function Write-BlogProcessRecord {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [Parameter(Mandatory = $true)]
        [string]$Service,
        [Parameter(Mandatory = $true)]
        [System.Diagnostics.Process]$Process,
        [Parameter(Mandatory = $true)]
        [string]$WorkingDirectory,
        [Parameter(Mandatory = $true)]
        [int]$Port,
        [Parameter(Mandatory = $true)]
        [string]$CommandPattern
    )

    $record = [pscustomobject]@{
        Service          = $Service
        ProcessId        = $Process.Id
        StartTimeUtc     = $Process.StartTime.ToUniversalTime().ToString('o')
        WorkingDirectory = $WorkingDirectory
        Port             = $Port
        CommandPattern   = $CommandPattern
    }

    $record | ConvertTo-Json | Set-Content -LiteralPath $Path -Encoding UTF8
    return $record
}

function Read-BlogProcessRecord {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        return $null
    }

    try {
        return Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json
    } catch {
        return $null
    }
}

function Test-BlogManagedProcess {
    param(
        [Parameter(Mandatory = $true)]
        $Record
    )

    if ($null -eq $Record -or $null -eq $Record.ProcessId) {
        return $false
    }

    try {
        $process = Get-Process -Id ([int]$Record.ProcessId) -ErrorAction Stop
        $recordStart = [DateTime]::Parse([string]$Record.StartTimeUtc).ToUniversalTime()
        $actualStart = $process.StartTime.ToUniversalTime()
        if ([Math]::Abs(($actualStart - $recordStart).TotalSeconds) -gt 2) {
            return $false
        }

        if ($Record.CommandPattern) {
            $command = Get-CimInstance Win32_Process -Filter "ProcessId = $($process.Id)" -ErrorAction SilentlyContinue
            if ($command -and $command.CommandLine -and $command.CommandLine -notmatch [regex]::Escape([string]$Record.CommandPattern)) {
                return $false
            }
        }

        return $true
    } catch {
        return $false
    }
}

function Remove-BlogProcessRecord {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if (Test-Path -LiteralPath $Path) {
        Remove-Item -LiteralPath $Path -Force
    }
}

function Stop-BlogManagedProcess {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RecordPath
    )

    $record = Read-BlogProcessRecord -Path $RecordPath
    if ($null -eq $record) {
        Remove-BlogProcessRecord -Path $RecordPath
        return 'stale'
    }

    if (-not (Test-BlogManagedProcess -Record $record)) {
        Remove-BlogProcessRecord -Path $RecordPath
        return 'external'
    }

    & taskkill.exe /PID ([int]$record.ProcessId) /T /F | Out-Null
    Remove-BlogProcessRecord -Path $RecordPath
    return 'stopped'
}

function Start-BlogChildProcess {
    param(
        [Parameter(Mandatory = $true)]
        [string]$FilePath,
        [string[]]$ArgumentList = @(),
        [Parameter(Mandatory = $true)]
        [string]$WorkingDirectory,
        [Parameter(Mandatory = $true)]
        [string]$StandardOutputPath,
        [Parameter(Mandatory = $true)]
        [string]$StandardErrorPath,
        [hashtable]$Environment = @{}
    )

    $previousValues = @{}
    try {
        foreach ($name in $Environment.Keys) {
            $previousValues[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
            Set-Item -Path "Env:$name" -Value ([string]$Environment[$name])
        }

        return Start-Process -FilePath $FilePath -ArgumentList $ArgumentList -WorkingDirectory $WorkingDirectory -RedirectStandardOutput $StandardOutputPath -RedirectStandardError $StandardErrorPath -WindowStyle Hidden -PassThru
    } finally {
        foreach ($name in $Environment.Keys) {
            if ($null -eq $previousValues[$name]) {
                Remove-Item -Path "Env:$name" -ErrorAction SilentlyContinue
            } else {
                Set-Item -Path "Env:$name" -Value $previousValues[$name]
            }
        }
    }
}

function Show-BlogLogTail {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [int]$Lines = 20
    )

    if (Test-Path -LiteralPath $Path) {
        Write-Host "--- $Path (last $Lines lines) ---" -ForegroundColor DarkGray
        Get-Content -LiteralPath $Path -Tail $Lines
    }
}
