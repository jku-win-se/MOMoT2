# MOMoT

MOMoT combines model transformation (EMF/Henshin) with search-based optimization to solve complex model-driven engineering tasks.

This repository contains:

- Eclipse/Tycho plugins for MOMoT core and language tooling
- Headless runner components
- Dockerized REST execution flow (zip-in/zip-out)
- Minimal reproducible stack example for validation

## Repository Structure

Key directories:

- `plugins/`: MOMoT, MOEA, language, runner, and UI plugins
- `tooling/`: target platform and build tooling
- `headless/`: headless runtime modules
- `mcp/`: MCP server integration
- `stack-example-minimal/`: minimal fixture for deterministic testing
- `doc/`: runbook and implementation docs

## Build

Build the headless REST Docker image:

```powershell
docker build -t momot-rest-test -f Dockerfile .
```

## Run REST Server (Docker)

Run the container (example with host port `8081`):

```powershell
docker run --rm -p 8081:8080 momot-rest-test
```

Health endpoint:

```text
http://localhost:8081/health
```

Swagger/OpenAPI:

```text
http://localhost:8081/docs
http://localhost:8081/openapi.json
```

## Reproducible Minimal Test (Recommended)

Use the automation scripts to run a deterministic end-to-end test with `stack-example-minimal`.

Windows PowerShell:

```powershell
./scripts/run-minimal-rest-test.ps1
```

Linux/macOS:

```bash
./scripts/run-minimal-rest-test.sh
```

Both scripts:

1. Build image (unless skipped)
2. Start container
3. Build deterministic payload zip
4. POST to `/run`
5. Validate `runner/exit_code.txt == 0`

Useful options:

- Port override (`-Port 8081` or `PORT=8081`)
- Skip rebuild (`-SkipBuild` or `SKIP_BUILD=1`)
- Keep container/artifacts for debugging

## REST API Contract (Summary)

- `GET /health`: readiness check
- `POST /run?script=<relative/path.momot>`: execute job

Important:

1. Request body for `/run` must be raw `application/zip`.
2. `script` query parameter must exactly match path inside uploaded zip.
3. Response is a zip containing `runner/` diagnostics and `out/` outputs.

## Documentation

Start here:

- `doc/README.md`
- `doc/08-validation-and-runbook.md`
- `doc/09-minimal-test-case.md`

## Project Page

Background and case-study information:

- http://martin-fleck.github.io/momot/
