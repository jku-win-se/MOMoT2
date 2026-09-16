---
name: runbook-stack
example: at.ac.tuwien.big.momot.examples.stack
when_to_use: To build, run, and verify the Stack example (the canonical smoke test).
working_dir: examples/at.ac.tuwien.big.momot.examples.stack/
status: in Maven reactor; cheapest example; ships committed reference outputs.
---

# Runbook: Stack

The Stack example balances loads across stacks by orchestrating `ShiftLeft`/`ShiftRight`
Henshin rules. It is the fastest, most reliable example - use it first to validate the whole
pipeline.

## Inputs

- Metamodel: `model/stack.ecore`
- Henshin: `model/stack.henshin` (units `ShiftLeft`, `ShiftRight` searched; `CreateStack`,
  `ConnectStacks` ignored)
- Input model: `model/input/model/model_five_stacks.xmi`
- Script: `src/.../stack/StackSearchExample.momot`

## Entrypoints

1. Generated (canonical): `at.ac.tuwien.big.momot.examples.stack.momot.StackSearchExample`
   (`src-gen/.../momot/StackSearchExample.java`, has `main`).
2. Hand-written: `StackOrchestration` (used by `StackSearch`), `StackEvalSearch`,
   `comparison/NativeStackExample`.

## Objectives and algorithms

- `StandardDeviation` (minimize), `SolutionLength` (minimize).
- Random, NSGA-II, NSGA-III. Experiment: population 100, 2000 evaluations, 5 runs.

## Build

```bash
mvn -B -am -pl examples/at.ac.tuwien.big.momot.examples.stack clean verify
```

## Run

- Working directory MUST be `examples/at.ac.tuwien.big.momot.examples.stack/`.
- Run `StackSearchExample.main` (Eclipse: Run As Java Application, working dir = project root;
  see [../skills/02-run-an-example.md](../skills/02-run-an-example.md)).

## Expected outputs

- `example/output/overall_objectives.pf`, `example/output/moea_objectives.pf`,
  `example/output/moea_kneepoints_objectives.pf`
- `example/output/analysis.txt`, `example/output/boxplot/`
- `example/output/solutions/`, `example/output/models/`

## Verify

- `.pf` files exist with >= 1 row; result models present under `example/output/models/`.
- Compare shape/scale against the committed references in `model/output_test/` and
  `model/output/` (objective columns: standard deviation and solution length).

## Known issues

- Wrong working directory -> `FileNotFoundException` for `model/input/model/model_five_stacks.xmi`.
- If using a hand main, confirm the same relative paths resolve.

## Done criteria

PASS when `.pf` and result `.xmi` files are generated and objective values are finite and
comparable to the committed reference outputs.
