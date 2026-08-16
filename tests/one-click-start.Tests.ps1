$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$commonPath = Join-Path $projectRoot 'scripts/blog-dev-common.ps1'
if (Test-Path -LiteralPath $commonPath) {
    . $commonPath
}

Describe 'Signal Notes one-click launcher helpers' {
    It 'returns available for a free port' {
        Get-BlogPortDecision -Listening:$false -Healthy:$false -Recognized:$false | Should Be 'available'
    }

    It 'reuses a recognized healthy service' {
        Get-BlogPortDecision -Listening -Healthy -Recognized | Should Be 'reuse'
    }

    It 'reports a conflict for an unknown listener' {
        Get-BlogPortDecision -Listening -Healthy:$false -Recognized:$false | Should Be 'conflict'
    }

    It 'recognizes the Signal Notes frontend title' {
        Test-SignalNotesFrontendHtml '<title>脉冲笔记 | Signal Notes</title>' | Should Be $true
    }

    It 'rejects an unrelated frontend title' {
        Test-SignalNotesFrontendHtml '<title>Another project</title>' | Should Be $false
    }

    It 'reads quoted values from a dotenv file' {
        $dotenvPath = Join-Path $env:TEMP "signal-notes-dotenv-$([guid]::NewGuid().ToString('N')).env"
        @(
            'DB_NAME=signal_notes_test'
            'DB_PASSWORD="test password"'
            '# ignored comment'
        ) | Set-Content -LiteralPath $dotenvPath -Encoding UTF8

        try {
            $values = Get-BlogDotEnv -Path $dotenvPath
            $values.DB_NAME | Should Be 'signal_notes_test'
            $values.DB_PASSWORD | Should Be 'test password'
        } finally {
            Remove-Item -LiteralPath $dotenvPath -Force -ErrorAction SilentlyContinue
        }
    }

    It 'decodes UTF-8 response bytes as text' {
        $bytes = [Text.Encoding]::UTF8.GetBytes('{"status":"UP"}')
        ConvertTo-BlogText -Content $bytes | Should Be '{"status":"UP"}'
    }

    It 'prefers pnpm when both package managers are available' {
        $runner = Get-BlogFrontendCommand -PnpmPath 'C:\tools\pnpm.cmd' -NpmPath 'C:\tools\npm.cmd'
        $runner.Mode | Should Be 'pnpm'
        $runner.CommandPattern | Should Be 'pnpm'
        $runner.FilePath | Should Be 'C:\tools\pnpm.cmd'
        ($runner.ArgumentList -join ' ') | Should Be 'dev --host 127.0.0.1 --port 5174 --strictPort'
    }

    It 'falls back to npm when pnpm is unavailable' {
        $runner = Get-BlogFrontendCommand -PnpmPath '' -NpmPath 'C:\tools\npm.cmd'
        $runner.Mode | Should Be 'npm'
        $runner.CommandPattern | Should Be 'npm'
        $runner.FilePath | Should Be 'C:\tools\npm.cmd'
        ($runner.ArgumentList -join ' ') | Should Be 'run dev -- --host 127.0.0.1 --port 5174 --strictPort'
    }
}

Describe 'Signal Notes launcher files' {
    $scriptFiles = @(
        'scripts/blog-dev-common.ps1',
        'start-blog.ps1',
        'stop-blog.ps1',
        'start-blog.cmd',
        'stop-blog.cmd'
    )

    It 'contains <script>' {
        $scriptFiles | ForEach-Object {
            Test-Path -LiteralPath (Join-Path $projectRoot $_) | Should Be $true
        }
    }

    It 'contains valid PowerShell syntax' {
        foreach ($relativePath in @('scripts/blog-dev-common.ps1', 'start-blog.ps1', 'stop-blog.ps1')) {
            $path = Join-Path $projectRoot $relativePath
            if (Test-Path -LiteralPath $path) {
                $parseErrors = $null
                [System.Management.Automation.Language.Parser]::ParseFile(
                    $path,
                    [ref]$null,
                    [ref]$parseErrors
                ) | Out-Null
                $parseErrors.Count | Should Be 0
            } else {
                $false | Should Be $true
            }
        }
    }

    It 'references the matching PowerShell script from each CMD wrapper' {
        (Get-Content -Raw (Join-Path $projectRoot 'start-blog.cmd')) | Should Match 'start-blog\.ps1'
        (Get-Content -Raw (Join-Path $projectRoot 'stop-blog.cmd')) | Should Match 'stop-blog\.ps1'
    }
}
