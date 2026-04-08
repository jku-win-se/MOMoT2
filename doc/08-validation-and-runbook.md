# Validation and Runbook

This runbook validates the full zip-in/zip-out REST execution path and helps isolate failures quickly.

## Pre-flight checks

1. Script parses and references only included files.
2. Model and transformation files are semantically valid for the target metamodel.
3. Docker image builds successfully.
4. Health endpoint responds before test submissions.

## Canonical job zip structure

```text
job.zip
|- src/
|  `- .../YourSearch.momot
`- model/
	|- input/.../*.xmi
	|- *.ecore
	`- *.henshin
```

Notes:

1. Keep paths in the archive Linux-friendly (`/` separators).
2. Upload the zip as a raw binary body (`application/zip`), not multipart form-data.
3. The `script` query parameter must match the extracted relative path exactly.

## Smoke-test flow

1. Build image:

```powershell
docker build -t momot-rest-test -f Dockerfile .
```

2. Run container:

```powershell
docker run --rm -p 8080:8080 momot-rest-test
```

3. Health check:

```powershell
Invoke-WebRequest http://localhost:8080/health | Select-Object -ExpandProperty Content
```

Swagger/OpenAPI docs:

```text
http://localhost:8080/docs
http://localhost:8080/openapi.json
```

4. Submit run request (binary zip body):

```powershell
curl.exe -sS -X POST "http://localhost:8080/run?script=src/at/ac/tuwien/big/momot/examples/stack/StackSearchExample.momot" -H "Content-Type: application/zip" --data-binary "@headless-example/job-minimal.zip" --output headless-example/response-minimal.zip
```

5. Inspect response contents:

- `runner/runner.log`
- `runner/exit_code.txt`
- `runner/request.json`
- expected compile and `out/` artifacts

## Reproducible stack-minimal fixture

Use this when validating the current repository's known-good REST path.

### One-command test (recommended)

PowerShell (Windows):

```powershell
./scripts/run-minimal-rest-test.ps1
```

Bash (Linux/macOS):

```bash
./scripts/run-minimal-rest-test.sh
```

Useful options:

1. `Port`/`PORT` (default `8081`) to avoid local `8080` conflicts.
2. `SkipBuild`/`SKIP_BUILD=1` to reuse an existing image.
3. `KeepContainer`/`KEEP_CONTAINER=1` to keep the container running after test.
4. `KeepArtifacts`/`KEEP_ARTIFACTS=1` to retain payload/response artifacts for debugging.

### Manual fallback steps

1. Build deterministic payload workspace:

```powershell
$root='headless-example/job-minimal'
if(Test-Path $root){ Remove-Item -Recurse -Force $root }
New-Item -ItemType Directory -Force -Path "$root/src/at/ac/tuwien/big/momot/examples/stack" | Out-Null
New-Item -ItemType Directory -Force -Path "$root/model/input/model" | Out-Null
Copy-Item "stack-example-minimal/src/at/ac/tuwien/big/momot/examples/stack/StackSearchExample.momot" "$root/src/at/ac/tuwien/big/momot/examples/stack/StackSearchExample.momot"
Copy-Item "stack-example-minimal/model/stack.henshin" "$root/model/stack.henshin"
Copy-Item "stack-example-minimal/model/stack.ecore" "$root/model/stack.ecore"
Copy-Item "stack-example-minimal/model/input/model/model_five_stacks.xmi" "$root/model/input/model/model_five_stacks.xmi"
```

2. Build zip with stable entry names:

```powershell
Push-Location headless-example/job-minimal
if(Test-Path ../job-minimal.zip){ Remove-Item -Force ../job-minimal.zip }
jar --create --file ../job-minimal.zip model/stack.ecore model/stack.henshin model/input/model/model_five_stacks.xmi src/at/ac/tuwien/big/momot/examples/stack/StackSearchExample.momot
Pop-Location
```

3. Run container and verify health:

```powershell
docker build -t momot-rest-test -f Dockerfile .
docker run --rm -p 8080:8080 momot-rest-test
Invoke-WebRequest -UseBasicParsing http://localhost:8080/health | Select-Object -ExpandProperty Content
```

4. Execute and verify success:

```powershell
curl.exe -sS -X POST "http://localhost:8080/run?script=src/at/ac/tuwien/big/momot/examples/stack/StackSearchExample.momot" -H "Content-Type: application/zip" --data-binary "@headless-example/job-minimal.zip" --output headless-example/response-minimal.zip
```

Expected:

1. `runner/exit_code.txt` is `0`.
2. `runner/request.json` includes `mainClass`, `jar`, and `script`.
3. `runner/runner.log` contains Search, Analysis, and Results sections.

## Triage decision tree

- Health fails: container or bootstrapping issue.
- Compile fails: script syntax/type/generation issue.
- Runtime fails: model/package/transformations/data issue.
- Exit code non-zero with compile success: semantic input or runtime environment issue.

## Known high-signal error classes

- Missing class (`NoClassDefFoundError`): runtime classpath packaging gap.
- Package not found (`EPackage` URI errors): model/metamodel registration mismatch.
- Empty model root (`IndexOutOfBounds` on contents): invalid model payload.
- Generated Java compile errors: script expression incompatibility.
- `Script not found in uploaded archive`: wrong `script=` path, wrong zip entry names, or multipart upload.
- `InaccessibleObjectException` (`java.util`): Java 21 module access; pass `--add-opens java.base/java.util=ALL-UNNAMED` when launching runner JVM.
- `ClassNotFoundException: lpg.runtime.RuleAction`: missing `lpg.runtime.java` bundle in runtime image.
- `defaultEngine is null` / `cannot find JavaScript engine`: missing Nashorn runtime jars on Java 21.

## CI-friendly assertions

1. `/health` returns success.
2. Response zip is returned for `/run`.
3. `runner/exit_code.txt` equals `0` for known-good fixtures.
4. `out/` contains expected result artifacts.

## MCP flow validation

Use the MCP server tooling in `mcp/server.js` for reproducible zip generation and execution:

1. `generate_artifacts_from_ecore`
2. `execute_momot_job`
3. `run_end_to_end`

Recommended smoke flow:

1. Start the REST container and verify `/health`.
2. Call `run_end_to_end` with `knownGoodFixture=true`.
3. Confirm response contains:
	- `success: true`
	- `exitCode: 0`
	- `outputs` list is present (may be empty depending on script result blocks)

If `exitCode` is non-zero, inspect:

1. `diagnostics.rootCauseHint`
2. `logTail`
3. `runner/runner.log` from the response zip
