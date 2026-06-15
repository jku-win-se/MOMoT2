# MOMoT MCP Server Usage

This MCP server exposes tools for artifact generation and REST execution through the existing MOMoT runner endpoint.

## Release status (v2.0.0-alpha.1)

**Note:** This is a v2.0.0-alpha.1 proof-of-concept. The server exposes six tools, of which **three form the validated functional subset** covered by automated tests (unit, integration, and MCP stdio protocol). The remaining three are **backward-compatible aliases** exposed for experimentation and are **not covered by automated tests**. Their behavior may change, or they may be removed in future releases, without prior deprecation notice.

### Tool surface overview

| Tool | Group | Tested |
|------|-------|--------|
| `generate_artifacts_from_ecore` | Validated | Unit + integration + stdio |
| `execute_momot_job` | Validated | Unit + integration + stdio |
| `run_end_to_end` | Validated | Unit + integration + stdio |
| `validate_henshin` | Validated | No |
| `validate_momot` | Validated | No |
| `momot_generate` | PoC alias | No |
| `momot_validate` | PoC alias (superseded by `validate_momot`) | No |
| `momot_run` | PoC alias | No |

## Tools (validated functional subset)

### generate_artifacts_from_ecore
Input schema highlights:
- ecoreContent or ecorePath (required)
- modelContent or modelPath (recommended for execution)
- problemDescription, objectiveHints (optional)
- packageName, className, scriptPath, henshinPath (optional)
- includeJavaHelper (optional)

Output envelope:
- success
- summary
- scriptPath
- generatedFiles (base64 map)
- warnings
- diagnostics

### execute_momot_job
Input schema highlights:
- scriptPath (required)
- filesBase64 (required)
- restBaseUrl (optional, default http://localhost:8080)
- requestTimeoutMs, retries, retryDelayMs, logTailLines (optional)

Output envelope:
- success
- exitCode
- scriptPath
- generatedFiles
- warnings
- summary
- logTail
- outputs
- diagnostics

### validate_henshin
Wraps `tools/henshin-validator/validate.mjs` for local Henshin rule validation.

Input schema highlights:
- henshinPath (required)
- mode: `structure` | `semantic` | `apply` (default `structure`)
- metamodelPath (required for `semantic` and `apply`)
- modelPath, ruleName (required for `apply`)
- parameters (optional string map for rule parameters)

### validate_momot
Wraps `tools/momot-validator/validate.mjs` for local `.momot` script validation.

Input schema highlights:
- momotPath (required)
- mode: `structure` | `semantic` | `compile` (default `structure`)
- projectRoot (recommended for `semantic` and `compile` when script paths are job-relative)

Output envelope:
- success
- exitCode
- result (parsed JSON from validator stdout)
- stderr (optional)

### run_end_to_end
Combined generation and execution flow.

Input schema highlights:
- Same generation inputs
- Same REST execution controls
- knownGoodFixture=true for deterministic stack fixture smoke run

## Example MCP Request Payloads

Generation:

{
  "ecorePath": "stack-example-minimal/model/stack.ecore",
  "modelPath": "stack-example-minimal/model/input/model/model_five_stacks.xmi",
  "problemDescription": "Balance stack load with short transformation length",
  "objectiveHints": ["Minimize imbalance", "Minimize solution length"],
  "packageName": "generated.stack",
  "className": "GeneratedStackSearch"
}

Execution:

{
  "restBaseUrl": "http://localhost:8080",
  "scriptPath": "src/at/ac/tuwien/big/momot/examples/stack/StackSearchExample.momot",
  "filesBase64": {
    "src/at/ac/tuwien/big/momot/examples/stack/StackSearchExample.momot": "<base64>",
    "model/stack.ecore": "<base64>",
    "model/stack.henshin": "<base64>",
    "model/input/model/model_five_stacks.xmi": "<base64>"
  }
}

End-to-end known-good smoke:

{
  "knownGoodFixture": true,
  "restBaseUrl": "http://localhost:8080"
}

## Example Successful Response Shape

{
  "success": true,
  "exitCode": 0,
  "scriptPath": "src/at/ac/tuwien/big/momot/examples/stack/StackSearchExample.momot",
  "generatedFiles": [
    "model/input/model/model_five_stacks.xmi",
    "model/stack.ecore",
    "model/stack.henshin",
    "src/at/ac/tuwien/big/momot/examples/stack/StackSearchExample.momot"
  ],
  "warnings": [],
  "summary": "Execution succeeded with 18 output artifact(s).",
  "logTail": "...",
  "outputs": [],
  "diagnostics": {
    "health": { "ok": true, "statusCode": 200 },
    "requestUrl": "http://localhost:8080/run?script=...",
    "statusCode": 200,
    "request": { "script": "..." },
    "rootCauseHint": "Execution succeeded."
  }
}

## Troubleshooting

- REST unavailable:
  - Verify container is running.
  - Check http://localhost:8080/health returns ok.
- Script not found in archive:
  - Ensure scriptPath exactly matches zip entry path.
  - Use forward slashes only.
- Compile failures:
  - Inspect diagnostics.logTail for compile section.
  - Validate generated script imports and objective blocks.
- Model/metamodel mismatches:
  - Check Ecore nsURI and model root compatibility.
- Non-zero exit code:
  - Use diagnostics.rootCauseHint and logTail for triage.

Note:
- outputs is always returned as a list and may be empty when the executed MOMoT script does not emit artifacts under out/.

## Verification Commands

From mcp directory:

npm install
npm test

With REST container running (integration tests):

$env:RUN_INTEGRATION_TESTS='1'
$env:MOMOT_REST_BASE_URL='http://localhost:8080'
npm run test:integration

With REST container running (MCP stdio protocol tests):

$env:RUN_MCP_STDIO_TESTS='1'
$env:MOMOT_REST_BASE_URL='http://localhost:8080'
npm run test:stdio

## Backward-compatible aliases (PoC / unvalidated)

The following three tools are exposed for experimentation and backward compatibility with earlier prototypes. They are **not covered by automated tests** and their behavior is **not guaranteed**. They may be removed in future releases without prior deprecation notice. If you want reliable MDE functionality, use the validated tools above.

### momot_generate

Generates a minimal `.momot` search scaffolding from a template. Produces approximately 10 lines of hand-written `.momot` syntax interpolated with input parameters. Does not parse any Ecore, does not generate Henshin rules, does not use `generateArtifactsFromEcore` internally.

Input schema highlights:
- prompt (optional): embedded as comment
- packageName (optional, default `momot.search`)
- className (optional): embedded as comment
- modelPath (required): embedded in the generated `model.file` field
- henshinModules (optional, default `[]`): embedded as string list

Output: envelope with a single `text` content item containing the generated script.

**Not equivalent to `generate_artifacts_from_ecore`.** That tool produces a full set of artifacts (script + Henshin rules + normalized Ecore + optional Java helper). `momot_generate` produces only the script skeleton.

### momot_validate

**Superseded by `validate_momot`.** This PoC stub returns `{valid: true}` for any non-empty string and does not parse the script. Use `validate_momot` for real validation.

### momot_run

Thin ergonomic wrapper around `execute_momot_job`. Accepts `scriptContent` as an inline string (rather than base64-encoded inside `filesBase64`), encodes it internally, and delegates to `executeMomotJob` with a default script path of `job.momot` if none is provided.

Input schema:
- scriptPath (optional, default `job.momot`)
- scriptContent (required)
- filesBase64 (optional, default `{}`): additional files to include in the zip
- restBaseUrl (optional)

Output: same envelope as `execute_momot_job`.

**Overlaps with `execute_momot_job`** with a slightly different input shape. Use `execute_momot_job` when you already have a base64-encoded script; use `momot_run` when you want to submit an inline script string.

