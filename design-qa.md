# Signal Notes Design QA

## Evidence

- Source visual truth: `.superpowers/product-design/references/textmode-overlay-fold.png`, `.superpowers/product-design/current/admin.png`, and `docs/superpowers/specs/2026-08-17-pixel-signal-redesign.md`.
- Rendered implementation: `http://127.0.0.1:5174/`.
- Primary screenshots: `.superpowers/product-design/final/landing-1440-fold.png`, `.superpowers/product-design/final/blog-1440.png`, `.superpowers/product-design/final/article-1440.png`, `.superpowers/product-design/final/admin-dashboard-1440.png`, `.superpowers/product-design/final/admin-settings-1440.png`, `.superpowers/product-design/final/share-landscape-1440.png`, and `.superpowers/product-design/final/share-portrait-1440.png`.
- Responsive screenshots: `.superpowers/product-design/final/landing-1024.png`, `.superpowers/product-design/final/landing-768.png`, `.superpowers/product-design/final/landing-390.png`, `.superpowers/product-design/final/landing-360.png`, `.superpowers/product-design/final/article-390.png`, and `.superpowers/product-design/final/admin-dashboard-390.png`.
- Full-view comparison evidence: `.superpowers/product-design/final/comparison-landing.png` and `.superpowers/product-design/final/comparison-admin.png`.
- Viewports: 1440x1000, 1024x768, 768x1024, 390x844, and 360x800.
- State: live local MySQL data, one published article, authenticated administrator, light and dark public themes, completed reveal animation.

The Landing Love reference is a stylistic target rather than a screen to clone. Its capture was normalized to the same comparison height as the implementation; exact element coordinates are intentionally different because Signal Notes uses a real circuit-board image, Chinese copy, and project-specific navigation.

## Findings

No actionable P0, P1, or P2 visual findings remain.

- Typography: the pixel display face is limited to terminal labels and hero-scale text; Chinese reading content uses the configured serif/sans stacks at readable weights and line heights. No clipped headings, negative letter spacing, or unintended truncation was found.
- Spacing and layout rhythm: the public shell, article surface, admin sidebar, tables, panels, and responsive stacks remain aligned at all five breakpoints. No horizontal page overflow was detected.
- Colors and visual tokens: acid green remains an action/status accent rather than the whole palette. Signal orange, cold paper, black, imagery, and semantic states provide sufficient separation in both themes.
- Image quality and asset fidelity: the real circuit-board photograph and generated dither bitmap render sharply. DiceBear Pixel Art is rendered as an image asset with uploaded media taking precedence. No missing or broken images were found.
- Copy and content: visible copy comes from site settings or live content. Category, tag, search, status, author, and article views displayed the expected database-backed content.

## Focused Region Evidence

- The landing comparison checks the pixel typography, right-side numbered navigation, monochrome terminal framing, image treatment, CTA contrast, and first-viewport section hint against the source direction.
- The admin comparison checks navigation density, active state, typography hierarchy, data panels, and the move from the earlier pale sidebar to the accepted dark operational shell.
- The share modal screenshots verify the 1200x630 landscape and 1080x1440 portrait outputs. Both include title, excerpt, cover, account identity, publication date, QR code, and save/share actions.
- Focused screenshots were sufficient because the important typography, controls, icons, avatar, and QR regions are readable at captured scale; no additional crop was required.

## Comparison History

1. Initial browser pass found a P1 identity mismatch: the existing article and generated share image still showed the legacy author name `林默` while the account profile showed `Sheldon`.
2. Root cause: the public post DTO returned the stored author snapshot, and databases that had already run V11 could retain an unbound legacy post.
3. Fix: bound legacy `林默`/`站点作者` records through the MySQL 5.7-compatible V12 migration and made linked posts read the current account name.
4. Post-fix evidence: `.superpowers/product-design/final/article-1440.png` and `.superpowers/product-design/final/article-390.png` show `Sheldon`; the API returns `authorId: 1`, and both article avatar alt texts resolve to `Sheldon的头像`.

## Interactions Tested

- Landing primary CTA, mobile menu, theme toggle, and blog navigation.
- Blog search, category filtering, category route, tag route, archives/about/contact/privacy routes, and empty/error status surfaces.
- Article reading surface, responsive progress layout, share modal, landscape/portrait poster switch, QR output, and account-backed author identity.
- Admin dashboard, post list/editor, taxonomy, media, users, settings, logs, mobile drawer, account profile save feedback, and avatar controls.
- A fresh browser tab visited landing, blog, article, and admin settings. Console output contained only Vite connection messages and no runtime errors.

## Follow-up Polish

- The settings page is intentionally long because all requested copy, mail, account, sharing, and advanced controls are exposed. Collapsible groups could reduce mobile scrolling later, but no control is obscured or unusable.

final result: passed
