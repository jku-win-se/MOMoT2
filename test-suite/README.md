# MOMoT E2E Test Suite

A curated set of end-to-end benchmark problems for testing the full MOMoT pipeline:
Ecore metamodel → Henshin transformation rules → MOMoT search script → multi-objective optimisation.

Each test case is self-contained and follows the same layout as `headless-example/job-minimal/`
so it can be **zipped and submitted directly** to the MOMoT REST API.

---

## Test Cases

| ID  | Name                          | Domain               | Metamodel classes          | Rules        | Objectives                          |
|-----|-------------------------------|----------------------|----------------------------|--------------|-------------------------------------|
| T01 | Stack Load Balancing          | Infrastructure       | StackModel, Stack          | shiftLeft, shiftRight | LoadRange ↓, SolutionLength ↓ |
| T02 | Class-Responsibility Assignment (CRA) | Software design | ClassModel, Class, Method, Attribute | assignFeature | NegCRAIndex ↓, SolutionLength ↓ |
| T03 | Tree Depth Reduction          | Structural refactoring | Tree, Node               | reparentNode | MaxDepth ↓, SolutionLength ↓       |
| T04 | Task–Machine Scheduling       | Workflow optimisation | Schedule, Machine, Task   | reassignTask | Makespan ↓, SolutionLength ↓       |

---

## Folder Layout

Each test case `T0X-<name>/` contains:

```
T0X-<name>/
├── problem.md                            ← Natural-language problem statement
├── model/
│   ├── <metamodel>.ecore                 ← Ecore metamodel
│   ├── <module>.henshin                  ← Henshin transformation module
│   └── input/
│       └── <instance>.xmi               ← Initial model instance
├── src/at/ac/tuwien/big/momot/examples/<name>/
│   └── <Name>SearchExample.momot        ← MOMoT search script
└── expected/
    └── pareto-front.json                 ← Reference Pareto front
```

The `model/` and `src/` layout mirrors `headless-example/job-minimal/`.

---

## Running a Test via the REST API

1. **Start the MOMoT headless server**

   ```bash
   docker build -t momot-headless -f Dockerfile.headless .
   docker run -p 8080:8080 momot-headless
   ```

2. **Zip the test case** (excluding `expected/` and `problem.md`)

   ```bash
   cd test-suite/T01-stack-balancing
   zip -r ../T01.zip model/ src/
   ```

3. **Submit the job**

   ```bash
   curl -X POST "http://localhost:8080/run?script=src/at/ac/tuwien/big/momot/examples/stack/StackSearchExample.momot" \
        -F "job=@../T01.zip" \
        -o T01-response.zip
   ```

4. **Inspect results**

   ```bash
   unzip T01-response.zip -d T01-result
   cat T01-result/out/objectives/moea_objectives.pf
   ```

5. **Validate against the reference front**

   Compare `out/objectives/moea_objectives.pf` against `expected/pareto-front.json`
   using hypervolume or ε-dominance (see Validation section below).

---

## `expected/pareto-front.json` Schema

```jsonc
{
  "problem":      "string  – human-readable test case name",
  "description":  "string  – one-line summary",
  "objectives": [
    {
      "name":      "string  – matches the objective name in the .momot script",
      "direction": "minimize | maximize",
      "unit":      "string  – e.g. 'load units', 'rule applications'"
    }
  ],
  "ideal_point":  { "<obj1>": number, "<obj2>": number },
  "nadir_point":  { "<obj1>": number, "<obj2>": number },
  "reference_front": [
    {
      "<obj1>":  number,
      "<obj2>":  number,
      "notes":   "string – optional explanation of this point"
      // additional fields (moves, assignment, etc.) are informational
    }
  ],
  "derivation":   "analytical | analytical_exact | empirical",
  "solutionLength": number,  // matches solutionLength in the .momot script
  "validation": {
    "method":        "string",
    "pass_condition":"string"
  },
  "notes": "string – optional additional remarks"
}
```

---

## Validation Approach

### Exact dominance check (T02, T03, T04)

For small state spaces with analytically derived fronts, verify:
- Every `reference_front` point is **not dominated** by any found solution that
  is better on all objectives simultaneously.
- The found front contains at least one solution within ε = 1 of each
  `reference_front` point (per objective).

### Hypervolume indicator (T01 and stochastic runs)

Compute the hypervolume of the found front using `nadir_point` as the reference
point and compare against the hypervolume of the `reference_front`. A passing
test achieves ≥ 80 % of the reference hypervolume across all 5 runs.

---

## Pareto Front Reference Values

### T01 — Stack Load Balancing

| LoadRange | SolutionLength | Notes                                  |
|-----------|----------------|----------------------------------------|
| 0         | 4              | Fully balanced; minimum 4 adjacent shifts |
| 2         | 3              | Near-balanced in 3 moves               |
| 4         | 2              | Partial balance in 2 moves             |
| 6         | 1              | Best single shift                      |
| 8         | 0              | Initial state                          |

### T02 — CRA

| NegCRAIndex | SolutionLength | Notes                                  |
|-------------|----------------|----------------------------------------|
| −1.5        | 2              | Optimal CRA=1.5; C1={m1,m2,a1} / C2={m3,a2} |
| −0.667      | 0              | Initial state; all features in C1      |

### T03 — Tree Depth Reduction

| MaxDepth | SolutionLength | Notes                                  |
|----------|----------------|----------------------------------------|
| 1        | 3              | Star topology; all nodes at depth 1    |
| 2        | 2              | Reparent D and C to root               |
| 3        | 1              | Reparent D to root                     |
| 5        | 0              | Initial state                          |

### T04 — Task Scheduling

| Makespan | SolutionLength | Notes                                  |
|----------|----------------|----------------------------------------|
| 4        | 2              | Optimal; T1→M2, T2→M3                  |
| 6        | 1              | Best single move; T1→M2                |
| 10       | 0              | Initial state                          |

---

## Adding New Test Cases

1. Create `test-suite/T0N-<name>/` following the layout above.
2. Write `problem.md` with: problem statement, initial model description, objectives table, rule table, known optimal solutions.
3. Write `model/<name>.ecore` — keep the metamodel minimal (≤ 5 EClasses).
4. Write `model/<name>.henshin` — one primary search rule, optional utility rules.
5. Write `model/input/<instance>.xmi` — small instance (≤ 10 model objects) with a known Pareto front.
6. Write `src/.../<Name>SearchExample.momot` — follow the structure of T01's script.
7. Write `expected/pareto-front.json` — include analytically derived or exhaustively computed front points with derivation notes.

**Guidelines for good test cases**:
- Use small instances where the Pareto front can be computed analytically or exhaustively.
- Choose instances where the ideal point on each axis is achievable with a modest number of rule applications (≤ 5).
- Ensure that the search space has genuine trade-offs (improving one objective must cost something on the other).
- Document the lower bounds on each objective in `problem.md`.
