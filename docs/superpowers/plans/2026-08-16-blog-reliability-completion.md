# Signal Notes Blog Reliability Completion Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task with verification checkpoints.

**Goal:** Make the Signal Notes blog usable end to end: content changes persist, public pages reflect them, administration workflows are discoverable, email/feedback/subscription states are explicit, and the interface remains readable and consistent.

**Architecture:** Preserve the existing Vue + Spring Boot + MySQL architecture. Fix state propagation at the API boundary, keep destructive operations explicit and auditable, and centralize public copy in `site_settings` with frontend reactive refresh after saves. Use browser-driven smoke checks plus backend integration tests for each user-facing workflow.

**Tech Stack:** Vue 3, Vite, lucide-vue-next, Spring Boot 3, Spring Data JPA, Flyway, MySQL, PowerShell/Pester, Browser plugin.

---

### Task 1: Reproduce and lock the content workflow failures

**Files:**
- Modify: `backend/src/test/java/com/signalnotes/blog/ApiIntegrationTests.java`
- Modify: `frontend/src/views/AdminView.vue`
- Test: `backend/src/test/java/com/signalnotes/blog/ApiIntegrationTests.java`

- [ ] Add an integration test that creates a published post through `/api/admin/posts`, confirms it is returned by `/api/posts`, moves it to trash, confirms it disappears, and permanently deletes it.
- [ ] Add a frontend regression guard for the editor autosave path: a publish operation must not be followed by a delayed draft save.
- [ ] Run the focused backend test and reproduce the current failure before changing implementation.
- [ ] Commit the focused reproduction and test additions.

### Task 2: Make editor publish state durable

**Files:**
- Modify: `frontend/src/views/AdminView.vue`
- Test: browser flow `/admin/posts/new` -> publish -> `/admin/posts` -> `/blog`

- [ ] Cancel pending autosave timers whenever an explicit save is requested.
- [ ] Suppress the deep editor watcher while applying the server response so publishing cannot immediately downgrade itself to `DRAFT`.
- [ ] Keep explicit draft saves and ordinary autosave behavior intact.
- [ ] Verify the post remains `PUBLISHED` after two seconds and appears on the public landing, blog list, and detail route.
- [ ] Commit the editor fix.

### Task 3: Make taxonomy public and manageable

**Files:**
- Modify: `backend/src/main/java/com/signalnotes/blog/controller/TaxonomyController.java`
- Modify: `frontend/src/views/BlogView.vue`
- Modify: `frontend/src/views/AdminView.vue`
- Test: `backend/src/test/java/com/signalnotes/blog/ApiIntegrationTests.java`

- [ ] Ensure category and tag public payloads are always JSON arrays with stable slug/name/count fields.
- [ ] Add a public tag index entry point from the blog navigation and expose newly created categories/tags after navigation without a hard reload.
- [ ] Keep deletion blocked when posts reference a category/tag and show the server reason in the admin UI.
- [ ] Verify create -> public category/tag index -> edit -> delete behavior with isolated data.
- [ ] Commit taxonomy changes.

### Task 4: Complete media and article deletion UX

**Files:**
- Modify: `frontend/src/views/AdminView.vue`
- Modify: `frontend/src/assets/styles.css`
- Modify: `backend/src/main/java/com/signalnotes/blog/controller/MediaController.java`
- Test: `backend/src/test/java/com/signalnotes/blog/ApiIntegrationTests.java`

- [ ] Keep article deletion as trash first, then permanent deletion only from the trash state, with confirmation text that names the action.
- [ ] Ensure media cards expose save, replace, and delete icon buttons with tooltips and clear success/error feedback.
- [ ] Preserve the server-side referenced-cover guard and return a useful error when deletion is blocked.
- [ ] Verify uploaded media can be edited, replaced, and deleted, and that the file is removed from disk.
- [ ] Commit media and deletion changes.

### Task 5: Make feedback, subscribers, and email configuration operational

**Files:**
- Modify: `frontend/src/views/AdminView.vue`
- Modify: `frontend/src/components/AdminAdvancedCopy.vue`
- Modify: `backend/src/main/java/com/signalnotes/blog/service/NotificationMailService.java`
- Modify: `backend/src/main/java/com/signalnotes/blog/controller/ContactController.java`
- Modify: `backend/src/main/java/com/signalnotes/blog/controller/SubscriptionController.java`
- Test: `backend/src/test/java/com/signalnotes/blog/ApiIntegrationTests.java`

- [ ] Add visible unread counts for feedback and subscribers in admin navigation and show clear empty/loading/error states.
- [ ] Keep every submission in MySQL when SMTP is disabled and state that no email was sent.
- [ ] Validate SMTP host, port, sender, notification recipient, auth, STARTTLS, and masked password behavior; make the test-email result distinguish unconfigured, sent, and failed.
- [ ] Verify feedback appears in the inbox and subscription appears in the subscriber list without a full page reload.
- [ ] Commit communication workflow changes.

### Task 6: Make settings reactive and remove remaining public hardcoded copy

**Files:**
- Modify: `frontend/src/site.js`
- Modify: `frontend/src/views/AdminView.vue`
- Modify: `backend/src/main/java/com/signalnotes/blog/controller/SiteController.java`
- Modify: `backend/src/main/resources/db/migration/V9__complete_site_copy_defaults.sql`
- Test: `backend/src/test/java/com/signalnotes/blog/ApiIntegrationTests.java`

- [ ] Seed every public string used by landing, blog, about, contact, privacy, archive, status, footer, and subscription pages.
- [ ] Make `/api/site` read the DB values through one defaults map rather than duplicating a second hardcoded public copy set.
- [ ] After saving settings, update the shared reactive site object so navigation in the same SPA immediately reflects changes.
- [ ] Verify changed site name, about copy, footer copyright, privacy text, and subscription copy across desktop and mobile routes.
- [ ] Commit settings changes.

### Task 7: Improve readability and control consistency

**Files:**
- Modify: `frontend/src/assets/styles.css`
- Modify: `frontend/src/views/AdminView.vue`
- Modify: `frontend/src/views/BlogView.vue`
- Modify: `frontend/src/views/LandingView.vue`

- [ ] Establish readable body, metadata, form label, table, and empty-state minimums without changing the visual hierarchy of display headings.
- [ ] Use icon-only controls only for familiar compact actions and provide title/ARIA labels; use text buttons for consequential actions such as publish, save, and delete confirmation.
- [ ] Fix mobile overflow, clipped table controls, and focus visibility in admin and public forms.
- [ ] Commit the visual and accessibility pass.

### Task 8: Full verification and handoff

**Files:**
- Test: `backend/src/test/java/com/signalnotes/blog/ApiIntegrationTests.java`
- Test: `tests/one-click-start.Tests.ps1`

- [ ] Run frontend build, backend full tests, launcher tests, and an isolated real-API regression that cleans only its own QA records.
- [ ] Browser-check landing, blog list, article detail, taxonomy, media, users, inbox, subscribers, settings, desktop, and mobile; collect DOM, screenshot, and console evidence.
- [ ] Confirm `git status` is clean and each phase has a commit.
- [ ] Send the final Feishu completion notification only after every requirement has evidence.
