# E2E Test Suite — Validation Results

Last run: 2026-06-09  Status: **ALL PASS**

## Summary

| ID  | Name                    | Tier 1 Henshin | Tier 2 MOMoT | Tier 3 Pareto |
|-----|-------------------------|:--------------:|:------------:|:-------------:|
| T01 | Stack Load Balancing    | PASS           | PASS         | PASS          |
| T02 | CRA                     | PASS           | PASS         | PASS          |
| T03 | Tree Depth Reduction    | PASS           | PASS         | PASS          |
| T04 | Task–Machine Scheduling | PASS           | PASS         | PASS          |

---

## T01 — Stack Load Balancing

**Tier 1 (Henshin validation): PASS**  
**Tier 2 (MOMoT execution): PASS**  
**Tier 3 (Pareto front): PASS**

Notes: `LoadRange` implemented as OCL `"stacks.load->max() - stacks.load->min()"`.
All five reference front points matched within the ε tolerance (LoadRange ≤ 1, SolutionLength ±2).

---

## T02 — Class-Responsibility Assignment (CRA)

**Tier 1 (Henshin validation): PASS**  
**Tier 2 (MOMoT execution): PASS**  
**Tier 3 (Pareto front): PASS**

Notes: `NegCRAIndex` implemented as a two-part OCL expression computing
`coupling_sum − cohesion_sum` directly from the ClassModel root.
Uses `oclIsKindOf(Method)` / `oclAsType(Method)` to separate methods from attributes
inside the polymorphic `Class.encapsulates` containment.
Reference points `(NegCRAIndex=−1.5, SL=2)` and `(NegCRAIndex=−0.667, SL=0)` both matched.
Stale `NegCRAIndexDimension` import removed.

---

## T03 — Tree Depth Reduction

**Tier 1 (Henshin validation): PASS**  
**Tier 2 (MOMoT execution): PASS**  
**Tier 3 (Pareto front): PASS**

Notes: `MaxDepth` implemented as OCL `"nodes.depth->max()"`.
All four reference front points `{(5,0),(3,1),(2,2),(1,3)}` covered within ε (MaxDepth ≤ 1,
SolutionLength ±1). Redundant rule constructs in `tree.henshin` also cleaned up.

---

## T04 — Task–Machine Scheduling

**Tier 1 (Henshin validation): PASS**  
**Tier 2 (MOMoT execution): PASS**  
**Tier 3 (Pareto front): PASS**

Notes: `Makespan` implemented as OCL
`"machines->collect(m | tasks->select(t | t.assignedTo = m).duration->sum())->max()"`.
All three reference front points `{(10,0),(6,1),(4,2)}` matched.
Lower bound Makespan = 4 confirmed (T1.duration = 4).

---

## Fixes Applied

| Area | Fix |
|------|-----|
| `tools/henshin-validator/` | Added Nashorn/Rhino JavaScript engine and ASM dependencies to the classpath so that Henshin arithmetic attribute expressions (`x+y`, `x-y`) evaluate correctly |
| `tools/henshin-validator/HenshinValidator.java` | Fixed `ResourceSet` mismatch that caused incorrect rule application during `--apply` tests |
| `test-suite/T02-cra/model/cra.henshin` | Fixed XML syntax errors (illegal comments inside element attributes) |
| `test-suite/T03-tree-depth/model/tree.henshin` | Fixed XML syntax errors; removed redundant rule constructs |
| `test-suite/T01-stack-balancing/src/.../StackSearchExample.momot` | Replaced `LoadRange : minimize { 0.0 }` with OCL string |
| `test-suite/T02-cra/src/.../CRASearchExample.momot` | Replaced `NegCRAIndex : minimize { 0.0 }` with full OCL expression for `coupling_sum − cohesion_sum`; removed non-existent `NegCRAIndexDimension` import; fixed parameter types |
| `test-suite/T03-tree-depth/src/.../TreeSearchExample.momot` | Replaced `MaxDepth : minimize { 0.0 }` with OCL string |
| `test-suite/T04-task-scheduling/src/.../ScheduleSearchExample.momot` | Replaced `Makespan : minimize { 0.0 }` with OCL string; fixed script compilation errors |
| Multiple `.momot` scripts | Replaced `RandomStringValue` with `RandomListValue` for fixed-domain parameters |
| Docker / REST runner | Configured and started the MOMoT headless REST runner container |
