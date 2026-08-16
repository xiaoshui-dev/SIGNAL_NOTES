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

    It 'uses npm to start the frontend' {
        $runner = Get-BlogFrontendCommand -NpmPath 'C:\tools\npm.cmd'
        $runner.Mode | Should Be 'npm'
        $runner.CommandPattern | Should Be 'npm'
        $runner.FilePath | Should Be 'C:\tools\npm.cmd'
        ($runner.ArgumentList -join ' ') | Should Be 'run dev -- --host 127.0.0.1 --port 5174 --strictPort'
    }

    It 'reports that npm is required when it is unavailable' {
        $message = ''
        try {
            Get-BlogFrontendCommand -NpmPath '' | Out-Null
        } catch {
            $message = $_.Exception.Message
        }
        $message | Should Match 'npm'
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

    It 'does not require pnpm in the startup path' {
        (Get-Content -Raw (Join-Path $projectRoot 'start-blog.ps1')) | Should Not Match 'pnpm'
        (Get-Content -Raw (Join-Path $projectRoot 'scripts/blog-dev-common.ps1')) | Should Not Match 'pnpm'
        (Get-Content -Raw (Join-Path $projectRoot 'start-blog.cmd')) | Should Not Match 'pnpm'
    }

    It 'uses and identifies the launcher directory from the CMD wrapper' {
        $wrapper = Get-Content -Raw (Join-Path $projectRoot 'start-blog.cmd')
        $wrapper | Should Match '%~dp0start-blog\.ps1'
        $wrapper | Should Match 'cd /d "%~dp0"'
        $wrapper | Should Match 'Launcher script: %~dp0start-blog\.ps1'
    }

    It 'does not require pnpm in the container frontend build' {
        $dockerfile = Get-Content -Raw (Join-Path $projectRoot 'frontend/Dockerfile')
        $dockerfile | Should Not Match '(?i)pnpm'
        $dockerfile | Should Match '(?i)npm install'
        $dockerfile | Should Match '(?i)npm run build'
    }

    It 'does not expose database implementation status in the public blog intro' {
        $blogView = Get-Content -Raw (Join-Path $projectRoot 'frontend/src/views/BlogView.vue')
        $blogView | Should Not Match 'class="api-mode"'
    }

    It 'guards the editor autosave watcher during explicit saves' {
        $adminView = Get-Content -Raw (Join-Path $projectRoot 'frontend/src/views/AdminView.vue')
        $adminView | Should Match 'clearTimeout\(autosaveTimer\)'
        $adminView | Should Match 'autosaveSuppressed'
        $adminView | Should Match 'saving'
        $adminView | Should Match ':disabled="saving'
    }

    It 'exposes live categories and a public tag index' {
        $landingView = Get-Content -Raw (Join-Path $projectRoot 'frontend/src/views/LandingView.vue')
        $blogHeader = Get-Content -Raw (Join-Path $projectRoot 'frontend/src/components/BlogHeader.vue')
        $blogView = Get-Content -Raw (Join-Path $projectRoot 'frontend/src/views/BlogView.vue')
        $landingView | Should Match 'validCategories\.map'
        $landingView | Should Not Match 'configured\.length\) return configured'
        $landingView | Should Match ':key="topic\.name"'
        $landingView | Should Not Match 'saved\.number'
        $blogHeader | Should Match 'to="/blog/tags"'
        $blogView | Should Match 'TAG INDEX / 008'
    }

    It 'makes destructive article and media actions explicit' {
        $adminView = Get-Content -Raw (Join-Path $projectRoot 'frontend/src/views/AdminView.vue')
        $adminView | Should Match '永久删除文章.*post\.title'
        $adminView | Should Match 'replaceMedia\(item, \$event\)'
        $adminView | Should Match 'item\.referenceCount'
        $adminView | Should Match ':disabled="!item\.deletable"'
        $adminView | Should Match 'mediaPreviewUrl\(item\)'
    }

    It 'shows actionable communication states in the admin console' {
        $adminView = Get-Content -Raw (Join-Path $projectRoot 'frontend/src/views/AdminView.vue')
        $styles = Get-Content -Raw (Join-Path $projectRoot 'frontend/src/assets/styles.css')
        $adminView | Should Match 'function navBadge\(path\)'
        $adminView | Should Match 'adminLoading'
        $adminView | Should Match 'adminLoadError'
        $adminView | Should Match 'flashError'
        $adminView | Should Match 'mailConfigurationState'
        $adminView | Should Match 'mailConfigurationState\.label'
        $adminView | Should Match 'mailConfigurationState\.detail'
        $adminView | Should Match 'mailConfigurationState\.tone'
        $styles | Should Match '\.admin-notice\.is-error'
        $styles | Should Match '\.admin-error-state'
        $styles | Should Match '\.mail-status\.is-ready'
        $styles | Should Match '\.mail-status\.is-error'
    }

    It 'keeps communication notices and sanitized mail settings in sync' {
        $adminView = Get-Content -Raw (Join-Path $projectRoot 'frontend/src/views/AdminView.vue')
        $adminView | Should Match "async function loadAdminData\(\) \{\s+const settingsVersionAtLoad = settingsMutationVersion;\s+saved\.value = '';"
        $adminView | Should Match "const savedSettings = await adminRequest\('/admin/settings'"
        $adminView | Should Match 'settings\.value = \{ \.\.\.settings\.value, \.\.\.savedSettings \};'
        $adminView | Should Match "const tone = result\.sent \? 'success' : result\.configured \? 'error' : 'info';"
        $adminView | Should Match "flash\(result\.message \|\| '测试邮件已处理', tone\);"
    }

    It 'gates settings writes on a successful settings load' {
        $adminView = Get-Content -Raw (Join-Path $projectRoot 'frontend/src/views/AdminView.vue')
        $adminView | Should Match "async function saveSettings\(\) \{ if \(adminLoading\.value \|\| adminLoadError\.value\) return;"
        $adminView | Should Match ':disabled="adminLoading \|\| Boolean\(adminLoadError\)( \|\| settingsSaving)?"'
        $adminView | Should Match '正在加载站点设置'
        $adminView | Should Match '站点设置加载失败'
        $adminView | Should Match 'const settingsVersionAtLoad = settingsMutationVersion;'
        $adminView | Should Match 'if \(settingsVersionAtLoad === settingsMutationVersion\) settings\.value ='
        $adminView | Should Match 'settingsMutationVersion \+= 1;'
    }

    It 'guards concurrent communication updates and duplicate settings saves' {
        $adminView = Get-Content -Raw (Join-Path $projectRoot 'frontend/src/views/AdminView.vue')
        $adminView | Should Match 'const settingsSaving = ref\(false\)'
        $adminView | Should Match 'if \(settingsSaving\.value\) return;'
        $adminView | Should Match ':disabled="adminLoading \|\| Boolean\(adminLoadError\) \|\| settingsSaving"'
        $adminView | Should Match 'settingsSaving \? .保存中'
        $adminView | Should Match 'const contactUpdateGenerations = new Map\(\)'
        $adminView | Should Match 'const subscriptionUpdateGenerations = new Map\(\)'
        $adminView | Should Match 'const pendingContactUpdates = reactive\(new Set\(\)\)'
        $adminView | Should Match 'const pendingSubscriptionUpdates = reactive\(new Set\(\)\)'
        $adminView | Should Match 'contactUpdateGenerations\.get\(item\.id\) === generation'
        $adminView | Should Match 'subscriptionUpdateGenerations\.get\(item\.id\) === generation'
        $adminView | Should Match ':disabled="pendingContactUpdates\.has\(item\.id\)"'
        $adminView | Should Match ':disabled="pendingSubscriptionUpdates\.has\(item\.id\)"'
    }

    It 'keeps public copy database-backed and live in the current SPA' {
        $siteController = Get-Content -Raw (Join-Path $projectRoot 'backend/src/main/java/com/signalnotes/blog/controller/SiteController.java')
        $migration = Get-Content -Raw (Join-Path $projectRoot 'backend/src/main/resources/db/migration/V9__complete_site_copy_defaults.sql')
        $adminView = Get-Content -Raw (Join-Path $projectRoot 'frontend/src/views/AdminView.vue')
        $blogView = Get-Content -Raw (Join-Path $projectRoot 'frontend/src/views/BlogView.vue')
        $statusView = Get-Content -Raw (Join-Path $projectRoot 'frontend/src/views/StatusView.vue')
        $siteController | Should Not Match 'result\.put\("siteName"'
        $siteController | Should Match 'SiteSettingPolicy\.PUBLIC_KEYS\.contains'
        $adminController = Get-Content -Raw (Join-Path $projectRoot 'backend/src/main/java/com/signalnotes/blog/controller/AdminController.java')
        $policy = Get-Content -Raw (Join-Path $projectRoot 'backend/src/main/java/com/signalnotes/blog/service/SiteSettingPolicy.java')
        $siteModule = Get-Content -Raw (Join-Path $projectRoot 'frontend/src/site.js')
        $migration | Should Match 'ON DUPLICATE KEY UPDATE'
        foreach ($key in @('tagsTitle', 'archiveIntro', 'categoryRouteIntro', 'status403Title', 'statusDefaultDescription', 'noResultsTitle', 'contactConsentLabel', 'contactNameLabel', 'contactEmailLabel', 'contactSubjectLabel', 'contactMessageLabel', 'noNotesLabel', 'articleNotFoundTitle', 'commentsTitle', 'commentsSubmitLabel', 'articleCopyLinkLabel', 'privacyRightsHeading', 'landingRecentLabel')) {
            $migration | Should Match "\('$key',"
        }
        $adminController | Should Match 'SiteSettingPolicy\.validateAndNormalize'
        $policy | Should Match '"shareTemplate"'
        $policy | Should Match 'ADMIN_ONLY_KEYS = Set\.of\(\)'
        $policy | Should Match 'MAX_KEYS = 200'
        $siteModule | Should Match 'siteRequestGeneration'
        $siteModule | Should Match 'export function applySite'
        $siteModule | Should Match 'requestGeneration === siteRequestGeneration'
        $siteModule | Should Match 'const trackedRequest = request\.finally'
        $siteModule | Should Match 'sitePromise === trackedRequest'
        $adminView | Should Match "!key\.startsWith\('mail\.'\)"
        $blogView | Should Match 'site\.archiveIntro \|\|'
        $blogView | Should Match 'site\.siteShortName \|\|'
        $blogView | Should Match 'site\.contactConsentLabel'
        $blogView | Should Match 'site\.noResultsTitle'
        $blogView | Should Match 'site\.searchInputPlaceholder'
        $blogView | Should Match 'site\.contactSubmitLabel'
        $blogView | Should Match 'site\.contactNameLabel'
        $blogView | Should Match 'site\.noNotesLabel'
        $articleView = Get-Content -Raw (Join-Path $projectRoot 'frontend/src/views/ArticleView.vue')
        $articleView | Should Match 'site\.articleNotFoundTitle'
        $articleView | Should Match 'site\.commentsSubmitLabel'
        $sharePoster = Get-Content -Raw (Join-Path $projectRoot 'frontend/src/components/SharePoster.vue')
        $sharePoster | Should Match 'site\.shareTemplate'
        $sharePoster | Should Match 'site\.sharePosterTitle'
        $advancedCopy = Get-Content -Raw (Join-Path $projectRoot 'frontend/src/components/AdminAdvancedCopy.vue')
        foreach ($key in @('siteTagline', 'landingTopics', 'landingLoadingLabel', 'blogFilterAllLabel', 'blogReadMoreLabel', 'searchResultSummary', 'shareArticleLabel', 'sharePosterTitle', 'shareCopyLinkLabel')) {
            $advancedCopy | Should Match ("settings\." + [regex]::Escape($key))
        }
        $landingView = Get-Content -Raw (Join-Path $projectRoot 'frontend/src/views/LandingView.vue')
        $landingView | Should Match 'site\.heroEnterBlog'
        $landingView | Should Match 'site\.landingNavPosts'
        $landingView | Should Match 'configuredItems'
        $landingView | Should Match 'typeof item\.name === .string'
        $landingView | Should Match 'typeof category\.description === .string'
        $statusView | Should Match 'site\.statusRetryLabel'
        $statusView | Should Match 'site\[`\$\{prefix\}Title`\]'
    }

    It 'keeps frontend copy defaults aligned with the backend settings contract' {
        $siteModule = Get-Content -Raw (Join-Path $projectRoot 'frontend/src/site.js')
        $policy = Get-Content -Raw (Join-Path $projectRoot 'backend/src/main/java/com/signalnotes/blog/service/SiteSettingPolicy.java')
        $migration = Get-Content -Raw (Join-Path $projectRoot 'backend/src/main/resources/db/migration/V9__complete_site_copy_defaults.sql')
        $siteKeys = [regex]::Matches($siteModule, '(?<![\w])([A-Za-z][A-Za-z0-9]*)\s*:') | ForEach-Object { $_.Groups[1].Value } | Select-Object -Unique
        foreach ($key in $siteKeys) {
            $policy | Should Match ('"' + [regex]::Escape($key) + '"')
            $migration | Should Match ("\('" + [regex]::Escape($key) + "',")
        }
    }

    It 'records the actual service listener processes after startup' {
        $startScript = Get-Content -Raw (Join-Path $projectRoot 'start-blog.ps1')
        $startScript | Should Match 'Write-BlogListenerProcessRecord -Path \$backendPidPath'
        $startScript | Should Match 'Write-BlogListenerProcessRecord -Path \$frontendPidPath'
    }
}

Describe 'Signal Notes public reading experience guards' {
    It 'reloads article data when the route slug changes and guards stale requests' {
        $article = Get-Content -Raw (Join-Path $projectRoot 'frontend/src/views/ArticleView.vue')
        $article | Should Match 'async function loadArticle\(slug\)'
        $article | Should Match 'watch\(\(\) => route\.params\.slug'
        $article | Should Match 'loadGeneration'
        $article | Should Match 'apiRequest\(`/comments\?postSlug='
        $article | Should Match 'removeArticleInteractionListeners'
    }

    It 'keeps public taxonomy/search/archive states actionable on connection failure' {
        $blog = Get-Content -Raw (Join-Path $projectRoot 'frontend/src/views/BlogView.vue')
        $landing = Get-Content -Raw (Join-Path $projectRoot 'frontend/src/views/LandingView.vue')
        $blog | Should Match 'contentStatus = ref\("loading"\)'
        $blog | Should Match 'Promise\.allSettled'
        $blog | Should Match 'hasUsableContent'
        $blog | Should Match 'isConnectionError'
        $blog | Should Match 'site\.noConnectionTitle'
        $blog | Should Match '@click="refreshPosts"'
        $landing | Should Match 'async function refreshLanding'
        $landing | Should Match '@click="refreshLanding"'
    }

    It 'binds configurable public navigation and footer labels' {
        $header = Get-Content -Raw (Join-Path $projectRoot 'frontend/src/components/BlogHeader.vue')
        $footer = Get-Content -Raw (Join-Path $projectRoot 'frontend/src/components/BlogFooter.vue')
        $site = Get-Content -Raw (Join-Path $projectRoot 'frontend/src/site.js')
        $header | Should Match 'site\.blogNavPostsLabel'
        $header | Should Match 'site\.blogNavSearchPlaceholder'
        $footer | Should Match 'site\.blogNavRssLabel'
        $site | Should Match 'blogNavCategoriesLabel'
        $admin = Get-Content -Raw (Join-Path $projectRoot 'frontend/src/views/AdminView.vue')
        $admin | Should Match "key !== 'mail\.passwordConfigured'"
    }

    It 'refreshes open share posters and falls back when system share rejects' {
        $poster = Get-Content -Raw (Join-Path $projectRoot 'frontend/src/components/SharePoster.vue')
        $poster | Should Match 'watch\(\[open, variant'
        $poster | Should Match 'site\.siteShortName'
        $poster | Should Match 'async function systemShare'
        $poster | Should Match 'catch \{ await copy\(\); \}'
    }

    It 'keeps supporting text readable and focusable' {
        $styles = Get-Content -Raw (Join-Path $projectRoot 'frontend/src/assets/styles.css')
        $styles | Should Match '(?s)\.blog-shell \.empty-state.*?font-size: 13px'
        $styles | Should Match '(?s)\.admin-shell \.admin-table.*?font-size: 13px'
        $styles | Should Match ':focus-visible'
        $styles | Should Match 'min-width: 40px; min-height: 40px'
        $styles | Should Match '\.inline-success\.is-error'
    }
}
