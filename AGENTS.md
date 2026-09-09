# MOMoT Agent Playbook

This file is the canonical operating guide for any AI agent working in this repository.
Read it fully before taking any action. It supersedes older inline comments.

---

## What This Repo Is

**MOMoT** (Model-Driven Optimization via Transformation) combines:
- **EMF/Ecore** — the metamodel layer (`.ecore`, `.xmi`)
- **Henshin** — graph-transformation rules (`.henshin`) used as search operators
- **MOEA Framework 5.1** — multi-objective evolutionary algorithms (NSGA-II, NSGA-III, Random). Scripts must import `org.moeaframework.core.selection.TournamentSelection` (not `core.operator`).
- **MOMoT DSL** — a declarative search script (`.momot`) that wires metamodel + rules + algorithms + fitness

The system ships as a **Docker headless REST runner** plus an **MCP server** that bridges LLM tool calls to the runner. An agent's typical task is to produce valid `.ecore` + `.henshin` + `.momot` artifacts for a new optimization problem, execute the search, and interpret the Pareto front.

This branch uses **MOEA Framework 5.1** (upgraded from 2.12). Typed objectives, `SearchExecutor`/`SearchAnalyzer`, and algorithm constructors (`populationSize` on NSGA-II/III and Random Search) follow the 5.x APIs. Do not emit 2.12 imports such as `org.moeaframework.core.operator.TournamentSelection` or `org.moeaframework.Executor`.

---

## Repository Layout (key paths)

```
mcp/                          MCP server (Node.js, stdio transport)
  server.js                   Tool definitions
  lib.js                      Core logic: generation, zip, REST, fixtures
  README.md                   Tool schema reference
headless-example/             REST-ready job payloads (zip-in / zip-out examples)
stack-example-minimal/        Canonical working example (stack load balancing)
test-suite/                   E2E benchmark suite — 5 verified test cases
  T01-stack-balancing/
  T02-cra/
  T03-tree-depth/
  T04-task-scheduling/
  T05-vehicle-routing/
  RESULTS.md                  Latest pass/fail status for all tiers
docs/
  00-architecture-overview.md Full architecture with diagrams
  henshin/                    10-chapter Henshin knowledge base (00–09)
tools/
  henshin-validator/          Fast CLI validator for .henshin (no Docker needed)
  momot-validator/            Fast CLI validator for .momot (Maven setup required)
  ecore-validator/            Fast CLI validator for .ecore (no Docker needed)
  xmi-validator/              Fast CLI validator for .xmi (no Docker needed)
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

### 5. `validate_momot`

Wraps the MOMoT CLI validator — callable without shell access.

| Input | Notes |
|---|---|
| `momotPath` | Path to `.momot` (required) |
| `mode` | `"structure"` (default) \| `"semantic"` \| `"compile"` |
| `projectRoot` | Job root for resolving relative paths (recommended for `semantic` and `compile`) |

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

3b.Validate Tier 1b validate_momot(mode="structure")
                    validate_momot(mode="semantic", projectRoot=...)
                    validate_momot(mode="compile",   projectRoot=...)

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

For detailed instructions, syntax guidelines, parameter value classes, and the 25-point pre-flight checklist for MOMoT scripts, refer directly to the **MOMoT Wiki and supplements**:
- **MOMoT Wiki Index**: [docs/momot/README.md](docs/momot/README.md)
- **10. Worked OCL Objective Examples**: [docs/momot/10-ocl-expressions.md](docs/momot/10-ocl-expressions.md)
- **11. Rule Parameter Value Injection**: [docs/momot/11-parameter-injection.md](docs/momot/11-parameter-injection.md)
- **12. Java Helper Integration**: [docs/momot/12-java-helper-integration.md](docs/momot/12-java-helper-integration.md)
- **13. Pre-Flight Checklist**: [docs/momot/13-generation-checklist.md](docs/momot/13-generation-checklist.md)

---

## Writing Henshin Rules

For detailed instructions, rule anatomy, binding to metamodels, common patterns, and debugging runbooks for Henshin transformations, refer directly to the **Henshin Expert Wiki**:
- **Henshin Wiki Index**: [docs/henshin/README.md](docs/henshin/README.md)
- **Common Henshin Patterns**: [docs/henshin/07-common-patterns.md](docs/henshin/07-common-patterns.md)
- **Henshin Debugging Runbook**: [docs/henshin/09-debugging-runbook.md](docs/henshin/09-debugging-runbook.md)
- **Sub-Agent Standalone Wiki**: [henshin-agent/wiki/](henshin-agent/wiki/)

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

Current status: **all 4 pass all 3 tiers** — see `test-suite/RESULTS.md`. (T05 is also added for from-scratch verification).

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
| `exitCode="1"`, log shows Henshin engine error | Rule parameters mismatched or NAC cycle | Run Tier 1 CLI validator; consult `docs/henshin/09-debugging-runbook.md` |
| Pareto front all zeros | Objective is `{ 0.0 }` placeholder | Replace with OCL string expression |
| `RandomStringValue` not found | Deprecated class | Replace with `RandomListValue(#["v1","v2"])` |

---

## Commit and Branching Convention

- Active development branch: `main`
- Push target: `origin/main`
- Do **not** force-push the main branch.
- Commit messages follow Conventional Commits: `feat:`, `fix:`, `docs:`, `chore:`, `test:`.

---

## Key Reference Files

| Resource | Path |
|---|---|
| Architecture overview | `docs/00-architecture-overview.md` |
| MCP tool schema | `mcp/README.md` |
| Henshin knowledge base index | `docs/henshin/README.md` |
| Debugging runbook | `docs/henshin/09-debugging-runbook.md` |
| Common Henshin patterns | `docs/henshin/07-common-patterns.md` |
| Canonical working example | `stack-example-minimal/` |
| E2E test suite | `test-suite/` |
| E2E results | `test-suite/RESULTS.md` |
| Verify-and-fix prompt | `.github/prompts/e2e-test-suite-verify-and-fix.prompt.md` |
| Henshin agent prompt | `.github/prompts/henshin-agent.prompt.md` |

---

## Smart Agent Entry Point

The system supports a complete **Smart Agent** flow designed to bootstrap optimization problems from plain English. The **Coordinator Agent** serves as the single entry point.

### Sub-Agents and Triggers

- **Coordinator Agent (`agents/prompts/coordinator.prompt.md`)**: Activates globally. Manages plan-detection, delegation, sequential verification, and final Docker execution.
- **Artifact Detector (`agents/prompts/artifact-detector.prompt.md`)**: Analyzes workspace contents and prompt keywords to create the generation plan.
- **Ecore Agent (`agents/prompts/ecore-agent.prompt.md`)**: Activates on `*.ecore`. Generates and semantic-validates EMF metamodels.
- **XMI Agent (`agents/prompts/xmi-agent.prompt.md`)**: Activates on `*.xmi`. Instantiates initial bad-start baseline model states.
- **Henshin Sub-Agent (`agents/prompts/henshin-subagent.prompt.md`)**: Activates on `*.henshin`. Generates, semantic-validates, and runs dry-run rules.
- **MOMoT Agent (`agents/prompts/momot-agent.prompt.md`)**: Activates on `*.momot`. Author's OCL objectives and compiles Java execution setups.
- **Java-Helper Agent (`agents/prompts/java-helpers-agent.prompt.md`)**: Activates on `*Helper.java`/`*Fitness.java`. Writes custom Java metric logic.

### Human-in-the-Loop (HITL) Gate Protocol

The Smart Agent enforces 8 interactive check gates (G0–G7) where the user must inspect, edit, or approve artifacts before the pipeline moves forward:
1. **G0**: Generation Plan approval.
2. **G1**: Metamodel structure and classes.
3. **G2**: Instance population and initial imbalance.
4. **G3**: Henshin transformation rule names and mappings.
5. **G4**: OCL objectives and algorithm parameters.
6. **G5**: Java helper logic (if applicable).
7. **G6**: Pre-Execution summary and file check.
8. **G7**: Post-Execution Pareto front evaluation.

To bypass HITL checks in CI/CD pipelines, set the environment variable `HITL_ENABLED=false` to automatically approve each gate.

---

## Henshin Sub-Agent Delegation Contract

Any Henshin generation request from the MOMoT coordinator MUST invoke the Henshin sub-agent prompt.

- **Trigger:** When `.henshin` generation or repair is needed.
- **Workflow:** Read knowledge bases (`henshin-agent/wiki/` + `docs/henshin/`), map requirements to LHS/RHS/NACs, generate unique XMI IDs, and run 3-tier validation (`validate-structure`, `validate-semantic`, and `apply`).
- **Input Contract:**
  - `ecorePath` / `ecoreContent`
  - `modelPath` / `modelContent` (XMI instance)
  - `nlRuleDescription` (rules to generate)
- **Output Contract:**
  - `henshinPath`
  - `henshinContent`
  - `validationResult` (3 tiers)

