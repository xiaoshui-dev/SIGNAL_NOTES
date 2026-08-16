# Signal Notes One-Click Dev Start Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Windows double-click scripts that start and stop the Signal Notes local development stack on fixed, conflict-safe ports.

**Architecture:** Root CMD files provide the double-click UX. Root PowerShell scripts orchestrate dependencies and services, while `scripts/blog-dev-common.ps1` owns reusable health, PID-record, port, and process-tree helpers. Pester tests cover the pure decision logic and script syntax; a real smoke test covers Docker, Spring Boot, Vite, restart reuse, and scoped shutdown.

**Tech Stack:** Windows PowerShell 5.1+, Pester 3.4, Docker Compose, Maven/Spring Boot, pnpm/npm with Vite, Git.

---

## File map

- Create `scripts/blog-dev-common.ps1`: reusable endpoint, port, PID-record, log-tail, and managed-process helpers.
- Create `start-blog.ps1`: dependency checks and MySQL/backend/frontend orchestration.
- Create `stop-blog.ps1`: safely stop only PID-recorded frontend/backend process trees.
- Create `start-blog.cmd`: double-click wrapper for startup.
- Create `stop-blog.cmd`: double-click wrapper for shutdown.
- Create `tests/one-click-start.Tests.ps1`: Pester coverage for helpers, wrappers, and PowerShell syntax.
- Modify `.gitignore`: exclude `.runtime/` process records and logs.
- Modify `README.md`: document one-click development workflow.

### Task 1: Define launcher behavior with tests

**Files:**
- Create: `tests/one-click-start.Tests.ps1`
- Test: `tests/one-click-start.Tests.ps1`

- [x] **Step 1: Write failing tests for service-state decisions**

Create Pester tests that dot-source `scripts/blog-dev-common.ps1` and assert:

```powershell
Describe 'Get-BlogPortDecision' {
    It 'returns available for a free port' {
        Get-BlogPortDecision -Listening:$false -Healthy:$false -Recognized:$false | Should Be 'available'
    }

    It 'reuses a recognized healthy service' {
        Get-BlogPortDecision -Listening -Healthy -Recognized | Should Be 'reuse'
    }

    It 'reports a conflict for an unknown listener' {
        Get-BlogPortDecision -Listening -Healthy:$false -Recognized:$false | Should Be 'conflict'
    }
}
```

Also test that Signal Notes HTML is recognized only when it contains the expected title, stale PID records do not validate, all four scripts parse without PowerShell syntax errors, and CMD wrappers reference the matching PowerShell filenames.

- [x] **Step 2: Run the focused tests and verify they fail**

Run:

```powershell
powershell -NoProfile -Command "Invoke-Pester -Path '.\tests\one-click-start.Tests.ps1'"
```

Expected: FAIL because `scripts/blog-dev-common.ps1` and launcher files do not exist.

### Task 2: Implement shared runtime helpers

**Files:**
- Create: `scripts/blog-dev-common.ps1`
- Test: `tests/one-click-start.Tests.ps1`

- [x] **Step 1: Implement the tested helper interface**

Provide these focused functions:

```powershell
function Get-BlogPortDecision {
    param([bool]$Listening, [bool]$Healthy, [bool]$Recognized)
    if (-not $Listening) { return 'available' }
    if ($Healthy -and $Recognized) { return 'reuse' }
    return 'conflict'
}

function Test-SignalNotesFrontendHtml {
    param([AllowEmptyString()][string]$Html)
    return $Html -match '<title>\s*脉冲笔记\s*\|\s*Signal Notes\s*</title>'
}
```

Add helpers for locating listeners, probing HTTP with timeouts, waiting with progress messages, reading/writing JSON PID records, verifying process ID plus exact UTC start time, stopping a verified process tree, and showing the tail of error logs.

- [x] **Step 2: Run tests and verify helper tests pass**

Run the Pester command from Task 1.

Expected: helper tests PASS; launcher-file tests still FAIL until Task 3.

### Task 3: Implement startup and shutdown scripts

**Files:**
- Create: `start-blog.ps1`
- Create: `stop-blog.ps1`
- Create: `start-blog.cmd`
- Create: `stop-blog.cmd`
- Modify: `.gitignore`
- Test: `tests/one-click-start.Tests.ps1`

- [x] **Step 1: Implement `start-blog.ps1`**

The script must:

```powershell
[CmdletBinding()]
param(
    [switch]$NoBrowser,
    [switch]$SkipMysql
)
```

Resolve all paths from `$PSScriptRoot`; create `.runtime/logs` and `.runtime/pids`; verify `docker`, `java`, `mvn`, `node`, and npm, with pnpm as an optional fallback; prefer `npm run dev` and use pnpm only when npm is unavailable; force Compose `MYSQL_PORT=3307` only for the current command; wait for `signal-notes-mysql` to become healthy; start Maven with `DB_PORT=3307` and `SERVER_PORT=8081`; pass `--host 127.0.0.1 --port 5174 --strictPort`; write process records with the actual command pattern; wait for the backend health JSON and Signal Notes HTML; open the browser unless `-NoBrowser` is present.

If a target port is already serving the expected service, reuse it. If it belongs to another service, report the listener PID/name and exit without killing it. On a failed launch, stop only frontend/backend processes created by the current invocation and keep MySQL running.

- [x] **Step 2: Implement `stop-blog.ps1`**

For `frontend.json` and `backend.json`, validate process ID and start time before running:

```powershell
taskkill.exe /PID $record.pid /T /F
```

Remove stale PID records without stopping an unrelated reused PID. Never stop MySQL, Docker Compose, or port 8080.

- [x] **Step 3: Implement CMD wrappers**

Use a root-relative wrapper so paths containing spaces work:

```bat
@echo off
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-blog.ps1"
if errorlevel 1 pause
```

The stop wrapper mirrors this with `stop-blog.ps1` and keeps the final result visible.

- [x] **Step 4: Ignore runtime state**

Add this exact entry to `.gitignore`:

```gitignore
.runtime/
```

- [x] **Step 5: Run Pester and syntax checks**

Run:

```powershell
powershell -NoProfile -Command "Invoke-Pester -Path '.\tests\one-click-start.Tests.ps1'"
git diff --check
```

Expected: all tests PASS and no whitespace errors.

- [x] **Step 6: Commit the launcher implementation**

```powershell
git add -- scripts/blog-dev-common.ps1 start-blog.ps1 stop-blog.ps1 start-blog.cmd stop-blog.cmd tests/one-click-start.Tests.ps1 .gitignore
git commit -m "feat: add one-click dev launcher"
```

### Task 4: Document and smoke-test the real workflow

**Files:**
- Modify: `README.md`

- [x] **Step 1: Update README**

Document double-click and terminal usage, fixed ports `3307`, `8081`, and `5174`, `-NoBrowser`, `-SkipMysql`, `stop-blog.cmd`, `.runtime/logs/`, dependency requirements, and the rule that stop preserves MySQL data and port 8080.

- [x] **Step 2: Run a cold-start smoke test**

Run:

```powershell
.\stop-blog.ps1
.\start-blog.ps1 -NoBrowser
```

Expected: MySQL healthy, backend health `UP`, frontend title recognized, and both PID records exist.

- [x] **Step 3: Run an idempotency smoke test**

Run startup again with `-NoBrowser` and compare recorded PIDs before/after.

Expected: PIDs are unchanged and output reports service reuse.

- [x] **Step 4: Verify scoped shutdown**

Capture listeners on `8080` and MySQL container status, run `stop-blog.ps1`, then verify ports `8081` and `5174` are free while `8080` still has the same owner and `signal-notes-mysql` remains healthy.

- [x] **Step 5: Run project regression checks**

Run:

```powershell
pnpm --dir frontend build
mvn -f backend/pom.xml test
docker compose config --quiet
```

Expected: frontend build succeeds, backend tests pass, and Compose configuration validates.

- [x] **Step 6: Commit documentation and verification evidence**

```powershell
git add -- README.md docs/superpowers/plans/2026-08-16-one-click-dev-start.md
git commit -m "docs: document one-click dev workflow"
```
