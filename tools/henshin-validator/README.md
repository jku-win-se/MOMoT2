# Henshin Validator CLI

A lightweight tool for validating and testing Henshin transformation rules locally —
no Docker, no REST server, no full MOMoT build required.

JARs are pre-downloaded to `lib/` and `HenshinValidator.class` is pre-compiled.
On a fresh clone, run `npm install && npm run setup` to restore them.

## Setup
```bash
npm install       # installs node-fetch
npm run setup     # downloads Henshin JARs and compiles HenshinValidator.java
```

## Modes

### 1. Structure Validation (no metamodel needed)
Loads the `.henshin` XMI and verifies it is a well-formed Henshin module.
Type references remain as unresolved proxies — this is expected in structure mode.
```bash
node validate.mjs --validate-structure <file.henshin>
```

### 2. Semantic Validation
Registers the metamodel and resolves all type references in the `.henshin` file.
Use this to catch `EPackage not found` and unresolved proxy errors before running MOMoT.
```bash
node validate.mjs --validate-semantic <file.henshin> --metamodel <file.ecore>
```

### 3. Apply Rule
Applies a named rule to a model instance and writes the result to `out_result.xmi`.
```bash
node validate.mjs --apply <file.henshin> --metamodel <file.ecore> --model <file.xmi> --rule <ruleName>
# Optional rule parameters:
node validate.mjs --apply <file.henshin> --metamodel <file.ecore> --model <file.xmi> --rule <ruleName> -PparamName=value
```

## Output
All modes emit a single JSON line to stdout:
```json
{ "valid": true, "mode": "structure", "rules": ["createStack", "shiftLeft"] }
{ "valid": true, "mode": "semantic",  "rules": ["createStack", "shiftLeft"] }
{ "applied": true, "result": "out_result.xmi" }
{ "applied": false, "reason": "Rule not applicable" }
```
Exit code is `0` on success, `1` on error (error details on stderr).

## Also available via MCP
The `validate_henshin` MCP tool wraps this CLI — use it when working through the MCP server.
