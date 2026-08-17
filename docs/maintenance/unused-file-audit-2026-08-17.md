# Unused File Audit - 2026-08-17

This audit records evidence before removing files. Generated build directories,
runtime logs, uploads, database data, backups, deployment assets, and design QA
evidence are outside the cleanup scope.

## Decision Matrix

| Candidate | Source imports/references | Entry reachability | Route/controller reachability | Deployment references | Git state | Decision |
| --- | --- | --- | --- | --- | --- | --- |
| `frontend/src/data.js` | Only `formatDate` and `getAuthor` were imported by two views; the exported demo posts, categories, tags, and `林默` profile had no consumers. | Not imported by `main.js`; reached only through those two helper imports. | No router route or API controller used its demo data. Public content comes from `/api/posts`, `/api/categories`, `/api/tags`, and `/api/site`. | Not referenced by Docker, Compose, Nginx, or BaoTa files. | Tracked. | Move `formatDate` to `date.js`, replace `getAuthor` with live identity data, then delete. |
| `frontend/pnpm-lock.yaml` | No runtime source reference. | The launcher, README, frontend Dockerfile, and package scripts use npm. | None. | Docker uses `npm ci`; launcher uses npm and does not require pnpm. | Tracked. | Replace with npm's `package-lock.json`. |
| `frontend/pnpm-workspace.yaml` | No package or source reference. | The repository contains one frontend package and npm is the supported runner. | None. | No deployment manifest invokes pnpm workspaces. | Tracked. | Delete. |

## Required Scans

Before deletion:

- `rg -n "(?:from|import).*data(?:\\.js)?|林默" frontend/src frontend/tests backend/src`
  found only two helper imports in public views, the obsolete demo module, legacy
  migrations, and explicit regression assertions.
- `router.js` reaches the five live Vue views directly; `data.js` is not an entry.
- Spring controllers and repositories serve all public and administrative data;
  no Java source references the frontend demo module.
- `docker-compose.yml`, both Dockerfiles, Nginx, and BaoTa manifests contain no
  reference to `data.js` or pnpm workspaces.
- `git ls-files` confirmed all three candidates were tracked before removal.

After deletion, the acceptance scan is:

```text
rg -n "from ['\"](?:../|./)data(?:\\.js)?['\"]|frontend/src/data\\.js|pnpm-workspace|pnpm-lock" . --glob '!frontend/dist/**' --glob '!backend/target/**'
```

The scan may find historical planning text, but it must find no live source,
test, launcher, Docker, or deployment dependency.

## Preserved Material

The following were inspected and deliberately retained:

- `frontend/public/assets/*` and `frontend/src/assets/generated/*`, because live
  pages, seeded content, or the selected visual direction use them.
- `deploy/baota/*`, because both panel-only deployment and packaged server
  configuration still depend on these files.
- `backend/src/main/resources/db/migration/V1__*` through `V11__*`, because Flyway
  history is append-only and production upgrades require every migration.
- Ignored `.runtime`, `.superpowers`, `frontend/dist`, `backend/target`, uploads,
  backups, logs, archives, JARs, and SQL exports, because they are generated
  evidence or user/deployment data rather than tracked dead source.
