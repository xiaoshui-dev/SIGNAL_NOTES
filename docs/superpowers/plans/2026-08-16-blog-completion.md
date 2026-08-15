# Signal Notes Blog Completion Implementation Plan

> **For agentic workers:** execute this plan task-by-task with verification and a Git commit at every completed stage.

**Goal:** Bring the existing Signal Notes blog to the P0/P1 requirements in `BLOG_REQUIREMENTS.md`, with working public reading flows, operational admin workflows, reliable backend state, and evidence-based visual QA.

**Architecture:** Extend the existing Vue 3/Vite frontend and Spring Boot 3/JPA backend in small vertical slices. Flyway owns schema changes; REST responses expose explicit loading/error/success states; local browser fallbacks remain only where the public experience can still degrade gracefully.

**Tech Stack:** Vue 3, Vite, Vue Router, Lucide, Marked, DOMPurify, QRCode; Spring Boot 3.5, Spring Security, JPA, Flyway, MySQL 8.4, Docker Compose, Nginx.

---

## Stage 9: Coverage and backend content model

- Add a traceability matrix mapping every P0/P1 requirement to a route, API, migration, test, or documented intentional limitation.
- Add revisions, soft-delete/recycle-bin fields, SEO fields, scheduling fields, tags and share configuration tables through Flyway.
- Add service/controller tests first for post status transitions, revision creation, categories/tags, search validation, and backup task state.
- Commit: `feat: complete content workflow data model`.

## Stage 10: Public routes and reading enhancements

- Add category/tag/archive/author detail routes and dedicated error pages for 404/403/500/maintenance states.
- Add dynamic metadata/canonical/Article JSON-LD, search validation/highlighting/pagination, contextual return state, reading-progress memory, code-copy, image lightbox, related and previous/next navigation.
- Add a contact form API with validation, idempotency and a user-visible ticket result; keep the existing privacy/about content.
- Add frontend tests/build and browser checks at desktop and 375px.
- Commit: `feat: complete public discovery and reading flows`.

## Stage 11: Admin content operations

- Add category/tag management, post preview, revision history/diff/restore, autosave, publish checklist, schedule/publish/offline/recycle-bin/restore, and batch post operations.
- Add real admin API calls for user role/status changes, comment reply/report actions, and media metadata updates.
- Commit: `feat: complete admin editorial workflows`.

## Stage 12: Operations, security, and observability

- Add dashboard trends and date-range selection, real logs/tasks/backups with verification metadata, and a recoverable backup command.
- Add password change/lockout/session-expiry behavior, security response headers/CSP, rate limiting for public mutation endpoints, and clearer API error envelopes.
- Commit: `feat: add operational controls and security safeguards`.

## Stage 13: Verification and handoff

- Run backend tests, package build, frontend build, Docker Compose config, MySQL migrations, health/API smoke checks, and browser visual audit using Product Design audit guidance.
- Inspect desktop/mobile screenshots, keyboard/focus states, console logs, no-overflow state, share poster dimensions/QR payload, and error routes.
- Ensure `git status` is clean, record the final commit, and POST a concise completion notification to the supplied Feishu webhook only after all checks pass.
- Commit: `chore: verify blog release readiness`.

