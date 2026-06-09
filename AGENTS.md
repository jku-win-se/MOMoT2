# MOMoT Agent Playbook

This file is the canonical operating guide for any AI agent working in this repository.
Read it fully before taking any action. It supersedes older inline comments.

---

## What This Repo Is

**MOMoT** (Model-Driven Optimization via Transformation) combines:
- **EMF/Ecore** — the metamodel layer (`.ecore`, `.xmi`)
- **Henshin** — graph-transformation rules (`.henshin`) used as search operators
- **MOEA Framework** — multi-objective evolutionary algorithms (NSGA-II, NSGA-III, Random)
- **MOMoT DSL** — a declarative search script (`.momot`) that wires metamodel + rules + algorithms + fitness

The system ships as a **Docker headless REST runner** plus an **MCP server** that bridges LLM tool calls to the runner. An agent's typical task is to produce valid `.ecore` + `.henshin` + `.momot` artifacts for a new optimization problem, execute the search, and interpret the Pareto front.

---

## Repository Layout (key paths)

```
mcp/                          MCP server (Node.js, stdio transport)
  server.js                   Tool definitions
  lib.js                      Core logic: generation, zip, REST, fixtures
  README.md                   Tool schema reference
headless-example/             REST-ready job payloads (zip-in / zip-out examples)
stack-example-minimal/        Canonical working example (stack load balancing)
test-suite/                   E2E benchmark suite — 4 verified test cases
  T01-stack-balancing/
  T02-cra/
  T03-tree-depth/
  T04-task-scheduling/
  RESULTS.md                  Latest pass/fail status for all tiers
doc/
  00-architecture-overview.md Full architecture with diagrams
  henshin/                    10-chapter Henshin knowledge base (00–09)
tools/
  henshin-validator/          Fast CLI validator (no Docker needed)
.github/prompts/              Reusable agent prompt templates
  henshin-agent.prompt.md     Henshin expert loop
  henshin-loop.prompt.md      Two-tier iterative Henshin fix loop
  e2e-test-suite-verify-and-fix.prompt.md  Full suite verification loop
.cursor/rules/
  henshin-expert.mdc          Auto-activated Cursor rule for .henshin files
```

---

## MCP Server

### Starting the MCP server

```bash
cd mcp
npm install
node server.js          # listens on stdio (JSON-RPC 2.0)
```

The server connects via **stdio transport** — no HTTP port is opened by the MCP server
itself. It relays requests to the MOMoT REST runner over HTTP.

### Starting the MOMoT REST runner (Docker)

```bash
docker build -t momot-headless -f Dockerfile.headless .
docker run -p 8080:8080 momot-headless
# Health check:
curl http://localhost:8080/health
```

Default REST base URL: `http://localhost:8080`  
(Some workspace configs use port 8081 — check `MOMOT_REST_BASE_URL` env var.)

---

## MCP Tools

### 1. `generate_artifacts_from_ecore`

Generates a `.henshin` skeleton and `.momot` search script from an Ecore metamodel.

| Input field | Type | Required | Notes |
|---|---|---|---|
| `ecoreContent` | string | one of these | Raw `.ecore` XML |
| `ecorePath` | string | one of these | Filesystem path to `.ecore` |
| `modelContent` / `modelPath` | string | recommended | XMI instance for execution |
| `problemDescription` | string | optional | Human description of the optimization goal |
| `objectiveHints` | string[] | optional | e.g. `["Minimize imbalance", "Minimize solution length"]` |
| `packageName` | string | optional | Java package for the generated script |
| `className` | string | optional | Class name prefix for generated files |

Key output fields: `success`, `summary`, `scriptPath`, `generatedFiles` (base64 map), `warnings`, `diagnostics`.

### 2. `execute_momot_job`

Executes a pre-built job ZIP against the REST runner.

| Input field | Type | Required | Notes |
|---|---|---|---|
| `scriptPath` | string | **required** | Path inside the ZIP, forward slashes only |
| `filesBase64` | `{path: base64}` | **required** | All files to include in the job ZIP |
| `restBaseUrl` | string | optional | Default `http://localhost:8080` |
| `requestTimeoutMs` | number | optional | Default 120 000 ms |
| `retries` | number | optional | Default 2 |
| `logTailLines` | number | optional | Lines of log to return |

Key output fields: `success`, `exitCode` (`"0"` = success), `logTail`, `outputs`, `diagnostics`.

### 3. `run_end_to_end`

Combines generation + execution in one call.

Set `knownGoodFixture: true` to run the built-in stack smoke test with no arguments —
use this first to verify the infrastructure is working before running custom problems.

### 4. `validate_henshin`

Wraps the CLI validator — callable without shell access.

| Input | Notes |
|---|---|
| `henshinPath` | Path to `.henshin` (required) |
| `mode` | `"structure"` (default) \| `"semantic"` \| `"apply"` |
| `metamodelPath` | Required for `semantic` and `apply` |
| `modelPath` | Required for `apply` |
| `ruleName` | Required for `apply` |
| `parameters` | String map of rule parameter values |

---

## Preferred Agent Workflow

```
1. Smoke test      run_end_to_end(knownGoodFixture=true)
                   → confirms REST runner is live

2. Generate        generate_artifacts_from_ecore(ecorePath, modelPath, ...)
                   → produces .henshin skeleton + .momot script

3. Validate Tier 1 validate_henshin(mode="structure")
                   validate_henshin(mode="semantic", metamodelPath=...)
                   validate_henshin(mode="apply",    ruleName=..., modelPath=...)

4. Execute Tier 2  execute_momot_job(scriptPath, filesBase64)
                   → check exitCode="0", out/objectives/ present

5. Verify Tier 3   compare out/objectives/overall_objectives.pf
                   against expected/pareto-front.json (ε-dominance check)

6. Repair loop     If any tier fails, apply the relevant fix from the repair
                   playbook below and restart from that tier.
```

If Docker is unavailable, skip steps 4–5 and document as SKIP.

---

## Writing `.momot` Scripts

### File structure

```
package <java.package.name>

import <ClassName>           // only MOMoT/MOEA classes on the classpath

search = {
   model          = { file = "<model/input/model.xmi>" }
   solutionLength = <N>

   transformations = {
      modules       = [ "<model/module.henshin>" ]
      ignoreUnits   = [ "<Module::Rule>" ]
      ignoreParameters = [ "<Module::Rule::param>" ]
      parameterValues = {
         "<Module::Rule::param>" : new RandomListValue(#["v1", "v2"])
         "<Module::Rule::param>" : new RandomIntegerValue(min, max)
      }
   }

   fitness = {
      objectives = {
         <ObjectiveName> : minimize "<OCL expression>"
         SolutionLength  : minimize new TransformationLengthDimension
      }
      solutionRepairer = new TransformationPlaceholderRepairer
   }

   algorithms = {
      Random   : moea.createRandomSearch()
      NSGA_II  : moea.createNSGAII(new TournamentSelection(2), new OnePointCrossover(1.0),
                   new TransformationPlaceholderMutation(0.15),
                   new TransformationParameterMutation(0.1, orchestration.moduleManager))
      NSGA_III : moea.createNSGAIII(4, ...)
   }
}

experiment = { populationSize = 100  maxEvaluations = 2000  nrRuns = 5
               progressListeners = [ new SeedRuntimePrintListener ] }

analysis  = { indicators = [ hypervolume additiveEpsilonIndicator ]  printOutput ... }
results   = { objectives = { outputFile = "out/objectives/overall_objectives.pf" printOutput }
              solutions  = { outputDirectory = "out/solutions/all/" }
              models     = { outputDirectory = "out/models/all/" printOutput } }
```

### Objective expressions — OCL string syntax

Write the fitness as an OCL string evaluated from the **model root element**:

| Problem | Expression |
|---|---|
| Stack load range | `"stacks.load->max() - stacks.load->min()"` |
| Tree max depth | `"nodes.depth->max()"` |
| Makespan (tasks on machines) | `"machines->collect(m \| tasks->select(t \| t.assignedTo = m).duration->sum())->max()"` |
| CRA NegCRAIndex | See `test-suite/T02-cra/src/.../CRASearchExample.momot` for the full two-part expression using `oclIsKindOf`/`oclAsType` |

Rules:
- `minimize "expr"` → standard OCL from the root; must return a number.
- `minimize new TransformationLengthDimension` → built-in; counts non-placeholder steps.
- `minimize { 0.0 }` → **placeholder** — search runs but objective is meaningless; always replace before Tier 3.
- Use `oclIsKindOf(ClassName)` / `oclAsType(ClassName)` to navigate polymorphic collections.
- Use `->sum()`, `->max()`, `->min()`, `->size()`, `->collect()`, `->select()`, `->reject()`.

### Parameter value classes

| Class | Use when |
|---|---|
| `RandomListValue(#["a","b","c"])` | Fixed enumeration of string values |
| `RandomIntegerValue(min, max)` | Integer range |
| `RandomStringValue("a","b","c")` | Deprecated — use `RandomListValue` |

### Module/rule name format in scripts

- `ignoreUnits` / `ignoreParameters` path: `"ModuleName::RuleName"` or `"ModuleName::RuleName::paramName"`
- The `ModuleName` is the `name` attribute of `<henshin:Module>` in the `.henshin` file.
- All paths are **case-sensitive**.

---

## Writing Henshin Rules

Full reference: `doc/henshin/` (chapters 00–09).  
Common XMI templates: `doc/henshin/07-common-patterns.md`.  
Debugging runbook: `doc/henshin/09-debugging-runbook.md`.

### Checklist before every commit

- [ ] `xmlns:xsi` declared on `<henshin:Module>` root element
- [ ] `<imports href="<nsURI>#/"/>` nsURI matches `.ecore` exactly
- [ ] Every `<type href="..."/>` resolves: `<nsURI>#//<ClassName>` for nodes, `<nsURI>#//<Class>/<feature>` for edges/attributes
- [ ] Every attribute `<type>` points to an `EAttribute`, not an `EReference`
- [ ] Every LHS node xmi:id has a matching `<mappings origin="..." image="..."/>` entry (or is deleted in RHS)
- [ ] NAC `<mappings>` inside `<child xsi:type="henshin:NestedCondition">` map LHS nodes to NAC conclusion nodes
- [ ] Structural validation passes: `node tools/henshin-validator/validate.mjs --validate-structure <file>`
- [ ] Semantic validation passes: `node tools/henshin-validator/validate.mjs --validate-semantic <file> --metamodel <ecore>`

### CLI validator commands

```bash
# Tier 1a — XMI structure (no metamodel)
node tools/henshin-validator/validate.mjs --validate-structure <file.henshin>

# Tier 1b — type reference resolution
node tools/henshin-validator/validate.mjs --validate-semantic <file.henshin> --metamodel <file.ecore>

# Tier 1c — rule application (produces out_result.xmi)
node tools/henshin-validator/validate.mjs --apply <file.henshin> --metamodel <file.ecore> \
  --model <file.xmi> --rule <ruleName>
```

All modes return a single JSON line to stdout. Exit code `0` = success.

---

## E2E Test Suite

`test-suite/` contains 4 verified benchmark problems. Each is a self-contained job
(zip-ready for the REST API) with an expected Pareto front.

| ID | Problem | Primary rule | Objectives |
|---|---|---|---|
| T01 | Stack Load Balancing | shiftLeft / shiftRight | LoadRange ↓, SolutionLength ↓ |
| T02 | Class-Responsibility Assignment | assignFeature | NegCRAIndex ↓, SolutionLength ↓ |
| T03 | Tree Depth Reduction | reparentNode | MaxDepth ↓, SolutionLength ↓ |
| T04 | Task–Machine Scheduling | reassignTask | Makespan ↓, SolutionLength ↓ |

Current status: **all 4 pass all 3 tiers** — see `test-suite/RESULTS.md`.

### Running a test case

```bash
cd test-suite/T01-stack-balancing
zip -r ../../T01.zip model/ src/
# then call execute_momot_job with scriptPath and the zip contents as filesBase64
```

### Pass criteria per tier

| Tier | What | Pass condition |
|---|---|---|
| 1 | Henshin CLI validation | Exit 0 for structure + semantic + apply |
| 2 | MOMoT REST execution | `exitCode="0"`, `out/objectives/overall_objectives.pf` present |
| 3 | Pareto front | Every `reference_front` point in `expected/pareto-front.json` has an ε-dominating solution in the found front |

To re-run the full verification + repair loop, apply the prompt:
`.github/prompts/e2e-test-suite-verify-and-fix.prompt.md`

---

## Output Interpretation

| Field | Meaning |
|---|---|
| `success=true` + `exitCode="0"` | Job completed successfully |
| `exitCode != "0"` | Execution failed (check `logTail`) |
| `diagnostics.rootCauseHint` | First triage target — read before anything else |
| `logTail` | Trailing lines of `runner/runner.log`; contains compile errors and stack traces |
| `outputs` | Output artifacts emitted to `out/`; may be empty for short runs |
| `diagnostics.health.ok=false` | REST runner is down — start/restart Docker container |

### Common failures

| Symptom | Root cause | Fix |
|---|---|---|
| `exitCode="1"`, log shows "script not found" | `scriptPath` doesn't match a ZIP entry | Check forward slashes, exact path |
| `exitCode="1"`, log shows model loading error | Model `.xmi` root type or nsURI mismatch | Check `xsi:schemaLocation` in the XMI |
| `exitCode="1"`, compile error in log | Invalid `.momot` syntax or unresolvable import | Check import names; remove non-classpath Java class imports |
| `exitCode="1"`, log shows Henshin engine error | Rule parameters mismatched or NAC cycle | Run Tier 1 CLI validator; consult `doc/henshin/09-debugging-runbook.md` |
| Pareto front all zeros | Objective is `{ 0.0 }` placeholder | Replace with OCL string expression |
| `RandomStringValue` not found | Deprecated class | Replace with `RandomListValue(#["v1","v2"])` |

---

## Commit and Branching Convention

- Active development branch: `standalone`
- Push target: `origin/standalone`
- Do **not** force-push `master` or `main`.
- Commit messages follow Conventional Commits: `feat:`, `fix:`, `docs:`, `chore:`, `test:`.

---

## Key Reference Files

| Resource | Path |
|---|---|
| Architecture overview | `doc/00-architecture-overview.md` |
| MCP tool schema | `mcp/README.md` |
| Henshin knowledge base index | `doc/henshin/README.md` |
| Debugging runbook | `doc/henshin/09-debugging-runbook.md` |
| Common Henshin patterns | `doc/henshin/07-common-patterns.md` |
| Canonical working example | `stack-example-minimal/` |
| E2E test suite | `test-suite/` |
| E2E results | `test-suite/RESULTS.md` |
| Verify-and-fix prompt | `.github/prompts/e2e-test-suite-verify-and-fix.prompt.md` |
| Henshin agent prompt | `.github/prompts/henshin-agent.prompt.md` |
