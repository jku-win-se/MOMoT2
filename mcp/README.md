# MOMoT MCP Server Usage

This MCP server exposes robust tools for artifact generation and REST execution through the existing MOMoT runner endpoint.

## Tools

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

With REST container running:

$env:RUN_INTEGRATION_TESTS='1'
$env:MOMOT_REST_BASE_URL='http://localhost:8080'
npm run test:integration
