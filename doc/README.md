# MOMoT Documentation Index

This directory contains practical documentation for creating, packaging, and executing MOMoT search scripts in this repository, with emphasis on the standalone/headless Docker flow.

## Who this is for

- Engineers writing new `.momot` scripts.
- Engineers debugging headless REST executions (`/run` endpoint).
- Engineers preparing reproducible job bundles for CI/CD.

## Reading order

1. [01-package-and-entrypoints.md](01-package-and-entrypoints.md)
2. [02-inputs-and-model-paths.md](02-inputs-and-model-paths.md)
3. [03-imports-and-henshin-modules.md](03-imports-and-henshin-modules.md)
4. [04-objectives-and-fitness.md](04-objectives-and-fitness.md)
5. [05-search-and-experiment.md](05-search-and-experiment.md)
6. [06-results-and-output-layout.md](06-results-and-output-layout.md)
7. [07-finalization-and-logging.md](07-finalization-and-logging.md)
8. [08-validation-and-runbook.md](08-validation-and-runbook.md)
9. [09-minimal-test-case.md](09-minimal-test-case.md)

## Companion notes

The [modelserver](modelserver/README.md) subfolder contains standalone/OSGi and Dockerization notes that are useful when resolving dependency and classpath issues in Eclipse-based stacks.

## Scope and assumptions

- The runtime executes inside a Linux container.
- Jobs are uploaded as zip files and extracted into a working directory.
- Relative file paths inside scripts are expected and recommended.
- Response artifacts are collected into a returned zip bundle.

## Quick checklist

- Script has a valid `package` and stable top-level script name.
- Model and module paths are relative and included in the zip payload.
- `/run` requests upload zip as raw binary (`application/zip`), not multipart form-data.
- `script=<relative-path>` exactly matches the extracted path inside the uploaded archive.
- Interactive API docs are available at `/docs` (OpenAPI JSON at `/openapi.json`).
- Minimal end-to-end fixture can be run via `scripts/run-minimal-rest-test.ps1` or `scripts/run-minimal-rest-test.sh`.
- Search algorithm and experiment settings are syntactically valid.
- Output writes to an expected subfolder (for example `out/`).
- A dry-run validation is completed before posting to `/run`.
