# Pixel Signal Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild Signal Notes as a polished, responsive pixel-signal technology blog while preserving every public and administrative workflow, replacing demo author identity with the authenticated account, and delivering verified production artifacts.

**Architecture:** Keep the existing Vue 3 single-page application and Spring Boot API boundaries. Introduce a small frontend identity module, shared visual tokens and reusable display components; extend the current user/post model with an optional avatar and author relation through a new Flyway migration, while retaining the author-name snapshot for compatibility. Execute eight independently testable phases, commit each phase, and finish with browser comparison QA, build/package verification, push, Feishu notification, and shutdown.

**Tech Stack:** Vue 3, Vue Router 4, Vite 6, Node test runner, Lucide Vue, local `@fontsource/fusion-pixel-12px-proportional-sc@5.3.0`, local `@dicebear/core@9.4.2`, local `@dicebear/pixel-art@9.4.2`, Spring Boot 3.5, Spring Security, Spring Data JPA, Flyway, MySQL 5.7, H2 tests.

---

## File Structure

- `frontend/src/assets/styles.css`: global semantic color/type/spacing tokens, shared public/admin responsive rules, motion and reduced-motion rules.
- `frontend/src/assets/generated/hero-circuit-dither.png`: real raster dither treatment for the landing hero.
- `frontend/src/authorIdentity.js`: pure author-name, initials, avatar precedence, and local DiceBear generation helpers.
- `frontend/src/components/PixelAvatar.vue`: one accessible avatar renderer shared by public and admin views.
- `frontend/src/components/SignalIndex.vue`: fixed-width numbered section/progress index used by landing and article views.
- `frontend/src/components/BlogHeader.vue`, `BlogFooter.vue`, `BrandLogo.vue`: shared pixel-signal public chrome.
- `frontend/src/components/SharePoster.vue`: two poster sizes, identity-aware author row, scannable QR, and download/share state.
- `frontend/src/views/LandingView.vue`: full-bleed introduction with terminal status bar, raster-resolution interaction, and section index.
- `frontend/src/views/BlogView.vue`: all public list/index/search/about/contact/privacy/author route variants.
- `frontend/src/views/ArticleView.vue`: readable article surface, progress index, author identity, code controls, comments, and recommendations.
- `frontend/src/views/StatusView.vue`: signal-language 403/404/500/503 states.
- `frontend/src/views/AdminView.vue`: dense command-shell admin, account-name edit, avatar upload/selection, and all current admin workflows.
- `backend/src/main/resources/db/migration/V11__account_author_identity.sql`: MySQL 5.7-safe avatar/author schema and legacy author migration.
- `backend/src/main/java/com/signalnotes/blog/domain/SiteUser.java`: avatar URL property.
- `backend/src/main/java/com/signalnotes/blog/domain/Post.java`: nullable author relation plus existing name snapshot.
- `backend/src/main/java/com/signalnotes/blog/service/PostService.java`: authenticated-author binding and public identity projection.
- `backend/src/main/java/com/signalnotes/blog/controller/AdminController.java`: current-account identity response.
- `backend/src/main/java/com/signalnotes/blog/controller/AdminAccountController.java`: authenticated name/avatar update endpoint.
- `backend/src/main/java/com/signalnotes/blog/controller/MediaController.java`: avatar reference protection.
- `frontend/tests/*.test.js`, `backend/src/test/java/com/signalnotes/blog/*.java`: regression coverage.
- `design-qa.md`: source-versus-implementation evidence and final gate.

### Task 1: Pixel Visual Foundation

**Files:**
- Modify: `frontend/package.json`
- Modify: `frontend/pnpm-lock.yaml`
- Modify: `frontend/src/main.js`
- Modify: `frontend/src/assets/styles.css`
- Modify: `frontend/src/components/BrandLogo.vue`
- Modify: `frontend/src/components/BlogHeader.vue`
- Modify: `frontend/src/components/BlogFooter.vue`
- Create: `frontend/src/components/SignalIndex.vue`
- Test: `frontend/tests/visual-contract.test.js`

- [ ] **Step 1: Write the failing visual-contract test**

Create a Node test that reads the source files and asserts the design primitives exist before any page is changed:

```js
test('pixel signal foundation is local, semantic, and motion-safe', () => {
  const css = readFileSync(new URL('../src/assets/styles.css', import.meta.url), 'utf8');
  const pkg = JSON.parse(readFileSync(new URL('../package.json', import.meta.url), 'utf8'));
  assert.equal(pkg.dependencies['@fontsource/fusion-pixel-12px-proportional-sc'], '5.3.0');
  for (const token of ['--pixel-black', '--cold-paper', '--acid-signal', '--signal-orange', '--digital-blue']) {
    assert.match(css, new RegExp(token));
  }
  assert.match(css, /prefers-reduced-motion:\s*reduce/);
});
```

- [ ] **Step 2: Verify RED**

Run: `pnpm --dir frontend test`

Expected: FAIL because the pinned font dependency and semantic pixel tokens do not exist.

- [ ] **Step 3: Install pinned local dependencies**

Run: `corepack pnpm --dir frontend add @fontsource/fusion-pixel-12px-proportional-sc@5.3.0 @dicebear/core@9.4.2 @dicebear/pixel-art@9.4.2`

Expected: `package.json` and `pnpm-lock.yaml` contain exact resolved packages without a runtime font/avatar CDN.

- [ ] **Step 4: Implement shared tokens and chrome**

Import the pixel font in `main.js`; define the eight fixed palette tokens, 4/8px spacing, 0-4px radii, focus ring, pressed state, image-resolution transition, and `prefers-reduced-motion` reset. Update header/footer/logo to use the semantic tokens and create `SignalIndex.vue` with this interface:

```vue
<SignalIndex :items="[{ id: 'intro', label: 'INTRO' }]" :active-id="activeSection" orientation="vertical" />
```

The component must render anchor buttons with `aria-current="location"` on the active item and fixed two-digit indices.

- [ ] **Step 5: Verify GREEN and build**

Run: `pnpm --dir frontend test && pnpm --dir frontend build`

Expected: all Node tests PASS and Vite exits 0 with locally bundled font/avatar code.

- [ ] **Step 6: Commit phase 1**

```powershell
git add frontend/package.json frontend/pnpm-lock.yaml frontend/src/main.js frontend/src/assets/styles.css frontend/src/components frontend/tests/visual-contract.test.js
git commit -m "feat: establish pixel signal design system"
```

### Task 2: Landing Page and Public Indexes

**Files:**
- Create: `frontend/src/assets/generated/hero-circuit-dither.png`
- Modify: `frontend/src/views/LandingView.vue`
- Modify: `frontend/src/views/BlogView.vue`
- Modify: `frontend/src/assets/styles.css`
- Test: `frontend/tests/public-view-contract.test.js`

- [ ] **Step 1: Write failing public-view contract tests**

Assert that the landing page provides terminal status, a five-item `SignalIndex`, clear/dither media layers, and that `BlogView` retains every routed mode while exposing a numbered scan list. The test must explicitly check existing route strings for `/blog/categories`, `/blog/tags`, `/blog/archives`, `/blog/search`, `/blog/authors`, `/blog/about`, `/blog/contact`, and `/blog/privacy`.

- [ ] **Step 2: Verify RED**

Run: `pnpm --dir frontend test`

Expected: FAIL on missing dither layer, shared index, and numbered scan-list markers.

- [ ] **Step 3: Create the real hero raster asset**

Generate a black/white acid-green dithered treatment from `frontend/public/assets/hero-circuit.jpg`, inspect the raster output, and save it as `frontend/src/assets/generated/hero-circuit-dither.png`. It must preserve the motherboard subject and be sized for a 16:9 hero crop; do not replace it with CSS art or SVG.

- [ ] **Step 4: Implement landing page**

Build a first viewport with a terminal status bar, pixel headline no longer than three lines, two CTAs, clear/dither image layers controlled by pointer position and scroll, and a 01-05 section index. Keep the hero full-bleed and sized so the next band is visible at 1440x900, 390x844, and 360x800. Make topics, featured, author/about, and footer full-width bands with working links and existing live API content.

- [ ] **Step 5: Implement public list/index modes**

Refactor the shared list presentation to `number / image / metadata / title`, retain working category/tag/search/archive pagination and error/empty states, and use a horizontally scrollable terminal command strip on narrow screens. Each image must remain clear by default and reveal its raster treatment only for hover/focus-capable interaction.

- [ ] **Step 6: Verify GREEN and build**

Run: `pnpm --dir frontend test && pnpm --dir frontend build`

Expected: all tests PASS; all current route modes remain present; build exits 0.

- [ ] **Step 7: Commit phase 2**

```powershell
git add frontend/src/assets/generated frontend/src/views/LandingView.vue frontend/src/views/BlogView.vue frontend/src/assets/styles.css frontend/tests/public-view-contract.test.js
git commit -m "feat: redesign public site as signal terminal"
```

### Task 3: Article, Sharing, and Status Experience

**Files:**
- Modify: `frontend/src/views/ArticleView.vue`
- Modify: `frontend/src/views/StatusView.vue`
- Modify: `frontend/src/components/SharePoster.vue`
- Modify: `frontend/src/shareUrl.js`
- Modify: `frontend/src/assets/styles.css`
- Test: `frontend/tests/share-url.test.js`
- Test: `frontend/tests/article-view-contract.test.js`

- [ ] **Step 1: Add failing interaction tests**

Add URL tests for canonical query/hash stripping and source-contract tests for article progress, accessible code-copy controls, poster author/avatar data, two poster dimensions, QR label, download status, and status-page recovery links.

```js
test('canonical share URL removes tracking state', () => {
  assert.equal(canonicalShareUrl('https://sheldon.top/blog/posts/a?utm_source=x#share'), 'https://sheldon.top/blog/posts/a');
});
```

- [ ] **Step 2: Verify RED**

Run: `pnpm --dir frontend test`

Expected: FAIL because the new article/poster contracts are absent or incomplete.

- [ ] **Step 3: Implement article reading system**

Keep the article body on `Cold Paper` with a readable serif width; build pixel metadata, desktop chapter index, mobile reading-progress bar, dark code blocks, an icon copy button with tooltip/result text, author strip, comments, and recommendations separated by lines rather than nested cards.

- [ ] **Step 4: Implement premium share posters**

Render 1200x630 and 1080x1440 raster posters with site mark, category, title, bounded summary, author identity, publication date, accent blocks, and a high-contrast quiet-zone QR. Preserve copy-link, Web Share, and download behavior; disable controls while canvas generation is active and show inline success/error text.

- [ ] **Step 5: Implement consistent status pages**

Use the same signal index/status language for 403/404/500/503, with one primary recovery action and one secondary blog link; no empty decorative card.

- [ ] **Step 6: Verify GREEN and build**

Run: `pnpm --dir frontend test && pnpm --dir frontend build`

Expected: all tests PASS and build exits 0.

- [ ] **Step 7: Commit phase 3**

```powershell
git add frontend/src/views/ArticleView.vue frontend/src/views/StatusView.vue frontend/src/components/SharePoster.vue frontend/src/shareUrl.js frontend/src/assets/styles.css frontend/tests
git commit -m "feat: refine reading sharing and status flows"
```

### Task 4: Admin Command Shell

**Files:**
- Modify: `frontend/src/views/AdminView.vue`
- Modify: `frontend/src/components/AdminAdvancedCopy.vue`
- Modify: `frontend/src/assets/styles.css`
- Test: `frontend/tests/admin-view-contract.test.js`

- [ ] **Step 1: Write failing admin contract tests**

Assert that the top bar renders the reactive current account rather than literal demo copy, the sidebar has selected-state semantics, save/publish/upload/mail tests expose inline busy/result state, and account settings include name/avatar controls.

- [ ] **Step 2: Verify RED**

Run: `pnpm --dir frontend test`

Expected: FAIL on hardcoded identity and missing account-avatar controls.

- [ ] **Step 3: Implement dense admin layout**

Convert the sidebar to a dark command rail with acid active row; use divided statistic grids instead of floating cards; preserve every current dashboard, editor, taxonomy, media, comment, feedback, subscriber, user, settings, audit, and backup action. Keep tables horizontally scrollable below their minimum useful width, not clipped.

- [ ] **Step 4: Implement clear operational states**

Give form controls persistent labels, validation text, disabled state, `aria-busy`, saving/publishing/uploading/test-mail progress, and explicit success/failure status. The editor retains its main writing area and fixed-width settings inspector at desktop widths, then stacks on mobile.

- [ ] **Step 5: Verify GREEN and build**

Run: `pnpm --dir frontend test && pnpm --dir frontend build`

Expected: all tests PASS; no hardcoded `林默 · 管理员` remains in admin source; build exits 0.

- [ ] **Step 6: Commit phase 4**

```powershell
git add frontend/src/views/AdminView.vue frontend/src/components/AdminAdvancedCopy.vue frontend/src/assets/styles.css frontend/tests/admin-view-contract.test.js
git commit -m "feat: redesign admin as operational command shell"
```

### Task 5: Account Name, Local Pixel Avatar, and Author Binding

**Files:**
- Create: `frontend/src/authorIdentity.js`
- Create: `frontend/src/components/PixelAvatar.vue`
- Modify: `frontend/src/site.js`
- Modify: `frontend/src/seo.js`
- Modify: `frontend/src/views/AdminView.vue`
- Modify: `frontend/src/views/ArticleView.vue`
- Modify: `frontend/src/views/BlogView.vue`
- Modify: `backend/src/main/java/com/signalnotes/blog/domain/SiteUser.java`
- Modify: `backend/src/main/java/com/signalnotes/blog/domain/Post.java`
- Modify: `backend/src/main/java/com/signalnotes/blog/service/PostService.java`
- Modify: `backend/src/main/java/com/signalnotes/blog/controller/AdminController.java`
- Modify: `backend/src/main/java/com/signalnotes/blog/controller/AdminAccountController.java`
- Modify: `backend/src/main/java/com/signalnotes/blog/repository/UserRepository.java`
- Test: `frontend/tests/author-identity.test.js`
- Test: `backend/src/test/java/com/signalnotes/blog/PostWorkflowTests.java`
- Test: `backend/src/test/java/com/signalnotes/blog/ApiIntegrationTests.java`

- [ ] **Step 1: Run the already-written frontend author tests for RED**

Run: `pnpm --dir frontend test`

Expected: FAIL with `ERR_MODULE_NOT_FOUND` for `frontend/src/authorIdentity.js`.

- [ ] **Step 2: Implement pure local identity helpers**

Create these exports with trimmed account-name precedence and deterministic local DiceBear SVG data URLs:

```js
export const DEFAULT_AUTHOR_NAME = '站点作者';
export function resolveAuthorName(accountName, siteName) { /* account, site, default */ }
export function createPostAuthor({ userName, siteAuthorName }) { /* same precedence */ }
export function displayAdminIdentity({ name, loginName }) { /* name, login, default */ }
export function authorInitials(name) { /* two Latin initials or first two CJK chars */ }
export function resolveAvatarUrl({ uploadedAvatarUrl, name }) { /* uploaded URL, then local DiceBear */ }
```

Use `createAvatar(pixelArt, { seed: [normalizedName], size: 96, backgroundType: ['solid'] }).toDataUri()`; never request `api.dicebear.com`.

- [ ] **Step 3: Verify frontend GREEN**

Run: `pnpm --dir frontend test`

Expected: author identity tests PASS, including stable seed and uploaded-avatar priority cases.

- [ ] **Step 4: Run backend author tests for RED**

Run: `./mvnw -f backend/pom.xml -Dtest=PostWorkflowTests,ApiIntegrationTests test` on POSIX or `backend\mvnw.cmd` if a wrapper exists; otherwise use the repository's configured Maven executable.

Expected: FAIL because `Post` has no author relation/avatar projection and account update response lacks avatar/name behavior.

- [ ] **Step 5: Implement backend identity contract**

Add nullable `SiteUser author` to `Post`, `avatarUrl` to `SiteUser`, and keep `authorName` as a snapshot. Resolve the authenticated user in create/update operations; use their trimmed name as the snapshot; include `authorId`, `authorName`, and `authorAvatarUrl` in public/admin post DTOs. Extend `/api/admin/me` and account update payloads with `id`, `loginName`, `name`, `avatarUrl`, and role. Never accept an arbitrary author id from article-edit input.

- [ ] **Step 6: Connect frontend identity**

Use `PixelAvatar` in the admin top bar/account panel, article header/author strip, author route, and share poster. Account settings can update name and choose an uploaded media URL; uploaded avatar wins over generated fallback. Remove demo-name fallbacks from `site.js`, `seo.js`, and post creation.

- [ ] **Step 7: Verify frontend and backend GREEN**

Run: `pnpm --dir frontend test && pnpm --dir frontend build`

Run: configured Maven test command for `PostWorkflowTests,ApiIntegrationTests`, then the full backend suite.

Expected: all tests PASS; public post JSON has new fields while existing fields remain; build exits 0.

- [ ] **Step 8: Commit phase 5**

```powershell
git add frontend backend/src/main/java backend/src/test
git commit -m "feat: bind posts to account identity and pixel avatar"
```

### Task 6: MySQL 5.7 Migration and Media Reference Protection

**Files:**
- Create: `backend/src/main/resources/db/migration/V11__account_author_identity.sql`
- Modify: `backend/src/main/java/com/signalnotes/blog/controller/MediaController.java`
- Modify: `backend/src/main/java/com/signalnotes/blog/repository/UserRepository.java`
- Test: `backend/src/test/java/com/signalnotes/blog/ApiIntegrationTests.java`
- Test: `backend/src/test/java/com/signalnotes/blog/PostWorkflowTests.java`

- [ ] **Step 1: Write failing migration/protection tests**

Add a test that assigns a media URL to the current user's avatar, attempts deletion, expects `409`, clears the avatar, and then expects successful deletion. Add a persistence test proving a new post stores the authenticated `author_id` and copied `author_name`.

- [ ] **Step 2: Verify RED**

Run the targeted Maven test command.

Expected: FAIL because user avatar references are not checked and schema fields do not exist.

- [ ] **Step 3: Add MySQL 5.7-safe migration**

Use `utf8mb4_unicode_ci`, not MySQL 8-only collations or window/JSON-table features:

```sql
ALTER TABLE site_users ADD COLUMN avatar_url VARCHAR(500) NULL AFTER name;
ALTER TABLE posts ADD COLUMN author_id BIGINT NULL AFTER author_name;
ALTER TABLE posts ADD CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES site_users(id) ON DELETE SET NULL;
UPDATE posts p JOIN site_users u ON u.role = 'ADMIN' SET p.author_id = u.id, p.author_name = u.name WHERE p.author_name = '林默' AND p.author_id IS NULL;
```

The update must select the configured/current administrator deterministically if multiple admins can exist; H2 test setup must stay compatible.

- [ ] **Step 4: Protect avatar media references**

Before deleting media, reject deletion when `UserRepository.existsByAvatarUrl(asset.getUrl())` is true and return a specific user-facing conflict message alongside existing article-cover protection.

- [ ] **Step 5: Verify GREEN and full backend suite**

Run targeted tests, then the full configured Maven suite.

Expected: tests PASS, Flyway starts cleanly against H2 test config, and no migration file before V11 changed.

- [ ] **Step 6: Commit phase 6**

```powershell
git add backend/src/main/resources/db/migration/V11__account_author_identity.sql backend/src/main/java backend/src/test
git commit -m "feat: migrate author identity and protect avatars"
```

### Task 7: Evidence-Based Unused File Cleanup

**Files:**
- Delete only files proven unused by the audit
- Modify: `.gitignore` if generated design/session files are not already excluded
- Create: `docs/maintenance/unused-file-audit-2026-08-17.md`

- [ ] **Step 1: Record the five-source audit**

For every candidate, record: source import/reference search, Vite/Spring entry reachability, router/controller reachability, Docker/BaoTa deployment manifest reference, and Git tracking/status. Likely candidate `frontend/src/data.js` may be deleted only after `rg -n "from ['\"](?:\.\./|\./)data" frontend/src frontend/tests` returns no live imports.

- [ ] **Step 2: Run baseline verification**

Run frontend tests/build and full backend tests before deletion.

Expected: PASS, providing a baseline for comparison.

- [ ] **Step 3: Delete only confirmed files with `apply_patch`**

Do not delete deployment archives, user uploads, databases, logs, or design evidence. Keep all existing public assets referenced by live or seeded content.

- [ ] **Step 4: Re-run reference scan and full verification**

Run: `rg -n "deleted-file-stem" . --glob '!frontend/dist/**' --glob '!backend/target/**'`

Run frontend tests/build and full backend tests.

Expected: no live references; all verification remains PASS.

- [ ] **Step 5: Commit phase 7**

```powershell
git add -A
git commit -m "chore: remove verified unused project files"
```

### Task 8: Browser QA, Packaging, Delivery, and Shutdown

**Files:**
- Create: `design-qa.md`
- Create/Modify: browser screenshots under `.superpowers/product-design/final/` (ignored evidence)
- Modify: panel-only deployment documentation only when packaged paths or environment keys changed
- Update: production archive/folder already used by this repository

- [ ] **Step 1: Read the browser and design-QA instructions**

Use only the in-app browser surface. Keep backend/frontend dev servers running, use the repository one-click launcher where applicable, and do not substitute HTTP success for rendered verification.

- [ ] **Step 2: Run automated verification from a clean build**

Run frontend tests/build and the full backend test/package command. Check `git diff --check` and confirm no secrets, credentials, webhook URL, build output, user uploads, or Product Design session files are tracked.

Expected: all commands exit 0; `git diff --check` has no output.

- [ ] **Step 3: Test key routes and states in browser**

At 1440, 1024, 768, 390, and 360 widths, verify introduction, blog, category, tag, search, article, share poster, status, admin dashboard, editor, media, users, and settings. Test navigation, filters, search, code copy, poster switching/download, theme/menu, account name/avatar, save/publish/upload/mail state, and console errors. Confirm no overlap, clipping, horizontal page scroll, blank media, unreadable text, or lost focus indicator.

- [ ] **Step 4: Perform blocking visual comparison**

At matching viewport/state, combine the selected Textmode/Price Adapter/Peter Oravec reference capture with the implementation screenshot in one comparison artifact. Create `design-qa.md` with source truth path, implementation path, viewport/state, full/focused comparisons, the five required fidelity surfaces, interaction/console checks, comparison history, and `final result: blocked` until every P0/P1/P2 finding is fixed and recaptured.

- [ ] **Step 5: Pass design QA**

Repeat fix/capture/compare until `design-qa.md` ends with exactly:

```text
final result: passed
```

P3 polish may remain only when explicitly listed and not visible as a quality defect in the core routes.

- [ ] **Step 6: Build final BaoTa artifact**

Rebuild the frontend distribution and backend JAR, update the existing D-drive deployment folder without adding command-line steps to the panel-only guide, and verify the artifact contains frontend files, backend JAR, environment template, uploads directory guidance, MySQL 5.7 migration, and panel-only instructions.

- [ ] **Step 7: Commit final evidence and artifact metadata**

```powershell
git add design-qa.md docs frontend backend
git commit -m "test: complete pixel signal release qa"
```

- [ ] **Step 8: Final self-review**

Review the entire diff and recent phase commits for regressions, unreachable controls, stale demo identity, old unused files, responsive problems, unescaped secrets, and unexpected generated files. Run all tests/builds once more after any review fix and create a separate fix commit if necessary.

- [ ] **Step 9: Integrate and push**

Confirm the current feature branch contains all phase commits. Fast-forward or merge it into `main` without rewriting user history, push `main` to `origin`, and verify the remote tip equals local `main`.

- [ ] **Step 10: Send completion notification**

POST a concise success card to the user-supplied Feishu bot webhook from runtime only. Do not print it, save it, add it to shell history where avoidable, or commit it. The card must include project name, final commit, test/build status, design-QA pass, deployment artifact location, and push result. Verify the Feishu API reports success.

- [ ] **Step 11: Schedule shutdown last**

Only after steps 1-10 have evidence of success, schedule Windows shutdown with a short delay so the final response can be delivered. Never shut down on partial failure, blocked QA, failed push, or failed notification.

## Plan Self-Review

- Spec coverage: tasks map to all public routes, article/share/status, every admin area, account name/avatar, author binding, MySQL 5.7 migration, media protection, local assets, responsiveness, accessibility, cleanup, packaging, notification, push, and shutdown.
- Placeholder scan: every task names concrete files, behavior, commands, and expected outcomes; no deferred implementation markers remain.
- Type consistency: frontend uses `authorId`, `authorName`, `authorAvatarUrl`; account uses `id`, `loginName`, `name`, `avatarUrl`, and role throughout. `Post.author` is nullable and `Post.authorName` remains the compatibility snapshot.
- Scope control: routes and information architecture stay unchanged; visual work reuses current assets and live content; no unrelated backend redesign or deployment workflow change is included.
- Safety: old migration files remain untouched, cleanups require five-source evidence, credentials/webhook remain out of Git, and shutdown is last and conditional.
