---
name: runbook-cra
example: at.ac.tuwien.big.momot.examples.cra
when_to_use: To build, run, and verify the Class Responsibility Assignment (CRA) example.
working_dir: examples/at.ac.tuwien.big.momot.examples.cra/
status: in Maven reactor; src-gen runner generated at build time.
---

# Runbook: CRA (Class Responsibility Assignment)

Assigns features (methods/attributes) to classes to minimize coupling and maximize cohesion.

## Inputs

- Package: `icmt.tool.momot.demo`
- Metamodel: `metamodel/architecture.ecore`
- Henshin: `transformations/architecture.henshin`
- Input model: `problem/Cart_Item.xmi` (reference solution: `problem/Cart_Item_solution.xmi`)
- Script: `src/icmt/tool/momot/demo/ArchitectureSearch.momot`

Note the script `search.model.adapt` block creates one class per feature and randomly
distributes features before the search starts.

## Entrypoints

1. Generated (canonical): the `*SearchExample` produced from `ArchitectureSearch.momot` into
   `src-gen/icmt/tool/momot/demo/` at build time (only `.gitignore` is committed, so it
   appears after a successful build).
2. Helper: `FitnessCalculator` provides `calculateCoupling` / `calculateCohesion` (used by the
   objectives; not the search runner).

## Objectives and algorithms

- `CouplingRatio` (minimize), `CohesionRatio` (maximize), `SolutionLength` (minimize).
- Random, NSGA-III, eMOEA. Experiment: population 100, 10000 evaluations, 10 runs.

## Build

```bash
mvn -B -am -pl examples/at.ac.tuwien.big.momot.examples.cra clean verify
```

Confirm the generated runner exists under `src-gen/icmt/tool/momot/demo/` after the build.

## Run

- Working directory = `examples/at.ac.tuwien.big.momot.examples.cra/`.
- Run the generated `*SearchExample.main`.
- For a smoke run, reduce `nrRuns`/`maxEvaluations` in the script first.

## Expected outputs

- `output/analysis/analysis.txt`
- `output/objectives/objective_values.txt`, `output/objectives/random_objective_values.txt`
- `output/solutions/` (and `all_solutions.txt`)
- `output/models/`, `output/models/kneepoints/`

The `results.adaptModels` block removes empty classes from result models before saving.

## Verify

- `output/objectives/*.txt` non-empty; result models present under `output/models/`.
- Coupling decreases / cohesion increases relative to the unoptimized input.

## Known issues

- If the generated runner is missing, the build did not run the MOMoT language generator -
  see [../skills/01-build-full-branch.md](../skills/01-build-full-branch.md).
- Wrong working directory -> `problem/Cart_Item.xmi` not found.

## Done criteria

PASS when objective and model artifacts are generated under `output/` with plausible
coupling/cohesion values.
