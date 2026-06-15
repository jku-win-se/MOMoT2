# MOMoT Validator CLI

A local tool for validating `.momot` scripts before running the full MOMoT REST job.
It reuses the same Xtext language stack and compiler pipeline as the headless runner.

On a fresh clone, run `npm run setup` once to build the required plugin JARs into `lib/`.

## Setup

```bash
npm run setup
```

Setup runs a Maven build and assembles the plugin classpath into `lib/`.

**JDK 17+** is enough for local setup and validation. With Java 17, setup builds plugins in
compatibility mode (`-Dmaven.compiler.release=17`). JDK 21 is used when available.

If you prefer a production-identical JDK 21 build, use Docker:

```bash
# Default: local build (Java 17 compat mode on JDK 17, standard on JDK 21+)
npm run setup

# Force Docker-based setup (JDK 21 inside container; requires Docker Desktop)
node validate.mjs --setup --docker
```

## Quick test (T01 stack balancing)

From `tools/momot-validator/` (after `npm run setup`):

```bash
node validate.mjs --validate-structure "../../test-suite/T01-stack-balancing/src/at/ac/tuwien/big/momot/examples/stack/StackSearchExample.momot"

node validate.mjs --validate-semantic "../../test-suite/T01-stack-balancing/src/at/ac/tuwien/big/momot/examples/stack/StackSearchExample.momot" --project-root "../../test-suite/T01-stack-balancing"

node validate.mjs --compile "../../test-suite/T01-stack-balancing/src/at/ac/tuwien/big/momot/examples/stack/StackSearchExample.momot" --project-root "../../test-suite/T01-stack-balancing"
```

From the repository root:

```bash
node tools/momot-validator/validate.mjs --validate-structure test-suite/T01-stack-balancing/src/at/ac/tuwien/big/momot/examples/stack/StackSearchExample.momot

node tools/momot-validator/validate.mjs --validate-semantic test-suite/T01-stack-balancing/src/at/ac/tuwien/big/momot/examples/stack/StackSearchExample.momot --project-root test-suite/T01-stack-balancing

node tools/momot-validator/validate.mjs --compile test-suite/T01-stack-balancing/src/at/ac/tuwien/big/momot/examples/stack/StackSearchExample.momot --project-root test-suite/T01-stack-balancing
```

Use the full script path — do not type literal `...` placeholders.

## Modes

### 1. Structure Validation

Parses the `.momot` script and reports syntax errors.

```bash
node validate.mjs --validate-structure <file.momot>
```

### 2. Semantic Validation

Runs full Xtext validation: types, file paths, OCL objectives, duplicate names, and experiment warnings.

```bash
node validate.mjs --validate-semantic <file.momot> --project-root <job-root>
```

Use `--project-root` when paths in the script are relative to a job directory
(for example `model/stack.henshin` in the test-suite fixtures).

### 3. Compile

Generates Java from the script and compiles it with `javac` (no search execution).

```bash
node validate.mjs --compile <file.momot> --project-root <job-root>
```

## Output

All modes emit a single JSON line to stdout:

```json
{ "valid": true, "mode": "structure", "issues": [] }
{ "valid": true, "mode": "semantic", "issues": [] }
{ "valid": true, "mode": "compile", "mainClass": "at.ac.tuwien.big.momot.examples.stack.StackSearchExample" }
{ "valid": false, "mode": "semantic", "issues": [{ "severity": "ERROR", "message": "...", "line": 42, "column": 8 }] }
```

Exit code is `0` on success, `1` on error (details on stderr).

## Also available via MCP

The `validate_momot` MCP tool wraps this CLI — use it when working through the MCP server.
