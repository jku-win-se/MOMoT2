# Minimal Test Case (Automated)

This document defines the canonical minimal end-to-end Docker REST test for MOMoT.

## Goal

Validate the zip-in/zip-out execution path with a deterministic fixture from `stack-example-minimal`:

1. Build image
2. Start container
3. Build payload zip
4. Execute `/run`
5. Verify `runner/exit_code.txt`
6. Print logs and key metadata

## Canonical scripts

Use one of these scripts from repository root:

- PowerShell (Windows): `scripts/run-minimal-rest-test.ps1`
- Bash (Linux/macOS): `scripts/run-minimal-rest-test.sh`

Both scripts perform the same test steps and fail fast on non-zero exit code.

## Quick start

### Windows (PowerShell)

```powershell
./scripts/run-minimal-rest-test.ps1
```

### Linux/macOS (Bash)

```bash
./scripts/run-minimal-rest-test.sh
```

## Common options

### Port override

Use a non-default host port if `8080` is occupied.

PowerShell:

```powershell
./scripts/run-minimal-rest-test.ps1 -Port 8081
```

Bash:

```bash
PORT=8081 ./scripts/run-minimal-rest-test.sh
```

### Skip image rebuild

PowerShell:

```powershell
./scripts/run-minimal-rest-test.ps1 -SkipBuild
```

Bash:

```bash
SKIP_BUILD=1 ./scripts/run-minimal-rest-test.sh
```

### Keep debugging artifacts

PowerShell:

```powershell
./scripts/run-minimal-rest-test.ps1 -KeepArtifacts -KeepContainer
```

Bash:

```bash
KEEP_ARTIFACTS=1 KEEP_CONTAINER=1 ./scripts/run-minimal-rest-test.sh
```

## Expected artifacts

If `KeepArtifacts` / `KEEP_ARTIFACTS=1` is enabled, scripts keep:

- `headless-example/job-minimal.zip`
- `headless-example/response-minimal.zip`
- `headless-example/response-minimal/runner/exit_code.txt`
- `headless-example/response-minimal/runner/request.json`
- `headless-example/response-minimal/runner/runner.log`

## Success criteria

1. HTTP health endpoint responds (`/health`).
2. `/run` returns a zip.
3. `runner/exit_code.txt` equals `0`.
4. `runner/request.json` and `runner/runner.log` exist.

## Troubleshooting hints

- `Script not found in uploaded archive`: zip path mismatch; ensure script path in query matches zip entry.
- `NoClassDefFoundError`: runtime classpath packaging issue.
- `Package with uri ... not found`: metamodel/package registration mismatch.
- `defaultEngine is null`: JavaScript engine/runtime dependency issue.
- Compile errors in generated Java: script-to-Java generation compatibility issue.
