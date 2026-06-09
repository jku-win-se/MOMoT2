---
description: "Run the full E2E test suite, diagnose every failure, fix test artifacts or project code, and loop until all four test cases pass all three validation tiers."
name: "E2E Test Suite: Verify and Fix"
argument-hint: "Run with no arguments to validate and fix all four test cases (T01–T04) in test-suite/."
agent: "agent"
---

# E2E Test Suite — Verify and Fix

You are working in the MOMoT repository. Your single goal is to make **all four E2E test
cases pass all three validation tiers**. Do not stop until every tier is green for every test.

## Test Cases

| ID  | Folder                            | Primary rule       | Objectives                     |
|-----|-----------------------------------|--------------------|--------------------------------|
| T01 | `test-suite/T01-stack-balancing/` | shiftLeft/shiftRight | LoadRange ↓, SolutionLength ↓ |
| T02 | `test-suite/T02-cra/`             | assignFeature      | NegCRAIndex ↓, SolutionLength ↓ |
| T03 | `test-suite/T03-tree-depth/`      | reparentNode       | MaxDepth ↓, SolutionLength ↓   |
| T04 | `test-suite/T04-task-scheduling/` | reassignTask       | Makespan ↓, SolutionLength ↓   |

## Three-Tier Pass Criteria

A test case **passes** only when it clears all three tiers in order.

### Tier 1 — Henshin Validation (CLI, no Docker)

For each test's `.henshin` file:

```bash
# Structure check — must exit 0 with no errors
node tools/henshin-validator/validate.mjs \
  --validate-structure test-suite/<TX>/model/<module>.henshin

# Semantic check — must exit 0 with no errors
node tools/henshin-validator/validate.mjs \
  --validate-semantic  test-suite/<TX>/model/<module>.henshin \
  --metamodel          test-suite/<TX>/model/<metamodel>.ecore

# Apply test — must produce a valid out_result.xmi and exit 0
node tools/henshin-validator/validate.mjs \
  --apply     test-suite/<TX>/model/<module>.henshin \
  --metamodel test-suite/<TX>/model/<metamodel>.ecore \
  --model     test-suite/<TX>/model/input/<instance>.xmi \
  --rule      <primaryRuleName>
```

Tier 1 pass conditions:
- Exit code = 0 for all three commands.
- `--apply` produces an `out_result.xmi` that is a valid XMI document (parseable and non-empty).
- No `ERROR` lines in the validator output.

### Tier 2 — MOMoT REST Execution (Docker required)

For each test case, zip the job payload and submit it to the REST runner:

```bash
cd test-suite/<TX>
zip -r ../../tmp/<TX>.zip model/ src/
```

Then call the MCP tool `execute_momot_job` with:
- `jobZipPath`  = `tmp/<TX>.zip`
- `scriptPath`  = the `.momot` file path inside the zip (e.g. `src/at/.../StackSearchExample.momot`)

Tier 2 pass conditions:
- `exit_code` in the response = `"0"` (string).
- The response zip contains `out/objectives/overall_objectives.pf` with at least one data line.
- No `SEVERE` or `Exception` lines in the runner log.

### Tier 3 — Pareto Front Validation

After Tier 2 succeeds, read:
- `out/objectives/overall_objectives.pf` from the response zip
- `expected/pareto-front.json` from the test case folder

**For each `reference_front` point** `(obj1_ref, obj2_ref)`, verify that the found
front contains at least one solution `(obj1_found, obj2_found)` such that:

```
obj1_found ≤ obj1_ref + epsilon_1   AND   obj2_found ≤ obj2_ref + epsilon_2
```

where the ε tolerance per test is:

| Test | ε₁ (primary objective) | ε₂ (SolutionLength) |
|------|------------------------|---------------------|
| T01  | LoadRange ≤ 1          | ±2                  |
| T02  | NegCRAIndex ≤ 0.1      | ±1                  |
| T03  | MaxDepth ≤ 1           | ±1                  |
| T04  | Makespan ≤ 1           | ±1                  |

Tier 3 pass condition: **every** `reference_front` point has at least one ε-dominating
solution in the found front.

---

## Execution Order

Work through each test case in sequence: T01 → T02 → T03 → T04.

For each test case:
1. Run **Tier 1**. If it fails, repair the `.henshin` or `.ecore` file and re-run Tier 1.
2. Once Tier 1 is green, run **Tier 2**. If it fails, repair the `.momot` script or model
   files and re-run Tier 2 (re-zip and re-submit).
3. Once Tier 2 is green, run **Tier 3**. If it fails, either:
   a. Fix the `.henshin` rule or `.momot` script if the objective is not being computed, OR
   b. Update `expected/pareto-front.json` if the reference front was analytically wrong
      (document why with a `notes` field update).
4. Record the tier results in the **Status Report** section below.

After all four test cases are complete, do a final pass over the Status Report. If any
test is not fully green, fix it before reporting completion.

---

## Repair Playbook

### Henshin structural errors
- Check that `xmlns:xsi` is declared on the root `<henshin:Module>` element.
- Check that `<imports href="<nsURI>#/"/>` matches the `.ecore` nsURI exactly.
- Check that every `<type href="..."/>` path ends in a valid EClass or EStructuralFeature.
- Consult `doc/henshin/09-debugging-runbook.md` for the full triage checklist.

### Semantic validation failures
- Ensure all node `<type>` hrefs resolve: `<nsURI>#//<ClassName>` for classes, `<nsURI>#//<Class>/<feature>` for references.
- Verify that every attribute `<type>` points to an `EAttribute`, not a `EReference`.
- Confirm that mapping `origin`/`image` IDs exist in LHS and RHS respectively.
- For NAC rules: the `<mappings>` inside `<child xsi:type="henshin:NestedCondition">` must map LHS nodes to the NAC conclusion nodes.

### Apply test produces wrong result
- Inspect `out_result.xmi` against the expected post-condition described in `problem.md`.
- Check attribute expressions in RHS: `value="x-y"` and `value="x+y"` are Henshin arithmetic expressions evaluated at runtime.
- For "move containment" rules (T02 assignFeature): ensure the LHS has the `encapsulates` edge from source class to feature, and the RHS has the `encapsulates` edge from target class to feature (not source), with the feature node mapped across.

### MOMoT execution fails (exit_code ≠ 0)
- Read the `runner/runner.log` from the response zip for the root cause.
- Common causes:
  - **Module not found**: path in `modules = [ "model/..." ]` does not match actual file location in the zip.
  - **Model not found**: path in `file = "model/input/..."` does not match the XMI location.
  - **Parameter type mismatch**: `RandomStringValue` used where `RandomIntegerValue` is needed or vice versa.
  - **ignoreParameters**: check that the parameter path `"Module::Rule::paramName"` matches exactly (case-sensitive, module name = `name` attribute of `<henshin:Module>`).

### Pareto front not found / insufficient coverage
- If `{ 0.0 }` placeholders are in the objectives, the search cannot differentiate solutions.
  The `.momot` file needs a real fitness expression or a Java `IFitnessDimension` class.
  For this test suite, the placeholder means the test can only verify Tier 2 (execution
  succeeds); Tier 3 is skipped for objectives with `{ 0.0 }` until a real implementation
  is available. Document this as a `"tier3_status": "skipped_placeholder"` in the JSON.
- If the found front is clearly wrong (e.g. all solutions have objective = 0.0), annotate
  `expected/pareto-front.json` with `"note": "Tier 3 deferred: real fitness not yet implemented"`.

---

## Reference Files

| Resource | Path |
|----------|------|
| Henshin knowledge base | `doc/henshin/README.md` (chapters 00–09) |
| Debugging runbook | `doc/henshin/09-debugging-runbook.md` |
| Common XMI patterns | `doc/henshin/07-common-patterns.md` |
| Canonical working example | `test-suite/T01-stack-balancing/model/stack.henshin` |
| Canonical working .momot | `test-suite/T01-stack-balancing/src/.../StackSearchExample.momot` |
| MCP tooling docs | `mcp/README.md` |
| Test suite README | `test-suite/README.md` |

---

## Status Report

Fill this in as you complete each tier. Update the file `test-suite/RESULTS.md` with the
same content when done.

```
T01 Stack Load Balancing
  Tier 1 (Henshin validation): [ ] PASS  [ ] FAIL
  Tier 2 (MOMoT execution):    [ ] PASS  [ ] FAIL  [ ] SKIP
  Tier 3 (Pareto front):       [ ] PASS  [ ] FAIL  [ ] SKIP
  Notes:

T02 CRA
  Tier 1 (Henshin validation): [ ] PASS  [ ] FAIL
  Tier 2 (MOMoT execution):    [ ] PASS  [ ] FAIL  [ ] SKIP
  Tier 3 (Pareto front):       [ ] PASS  [ ] FAIL  [ ] SKIP
  Notes:

T03 Tree Depth
  Tier 1 (Henshin validation): [ ] PASS  [ ] FAIL
  Tier 2 (MOMoT execution):    [ ] PASS  [ ] FAIL  [ ] SKIP
  Tier 3 (Pareto front):       [ ] PASS  [ ] FAIL  [ ] SKIP
  Notes:

T04 Task Scheduling
  Tier 1 (Henshin validation): [ ] PASS  [ ] FAIL
  Tier 2 (MOMoT execution):    [ ] PASS  [ ] FAIL  [ ] SKIP
  Tier 3 (Pareto front):       [ ] PASS  [ ] FAIL  [ ] SKIP
  Notes:
```

---

## Termination Condition

Stop and report **only when**:
- All four test cases have Tier 1 = PASS.
- All four test cases have Tier 2 = PASS or SKIP (SKIP only if Docker is unavailable).
- All four test cases have Tier 3 = PASS or SKIP (SKIP only if `{ 0.0 }` placeholder).
- `test-suite/RESULTS.md` has been written with the final status report.

Do not declare success if any tier shows FAIL.
