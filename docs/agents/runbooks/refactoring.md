---
name: runbook-refactoring
example: at.ac.tuwien.big.momot.examples.refactoring
when_to_use: To build, run, and verify the generic model-refactoring example.
working_dir: examples/at.ac.tuwien.big.momot.examples.refactoring/
status: in Maven reactor; small and quick; uses an OCL objective.
---

# Runbook: Refactoring

Applies refactoring transformations to a generic model, optimizing for short solutions and
small content size.

## Inputs

- Metamodel: refactoring metamodel (`RefactoringPackage`)
- Henshin: `model/Refactoring.henshin`
- Input model: `model/SeveralRefactorings.xmi`
- Script: `src/.../refactoring/Refactoring.momot`

## Entrypoints

1. Hand-written: `RefactoringOrchestration`, `RefactoringSearch`.
2. Generated `*SearchExample` from `Refactoring.momot`.

## Objectives and algorithms

- `SolutionLength` (min), `ContentSize` (min, OCL: `properties->size() * 1.1 + entities->size()`).
- NSGA-II, NSGA-III. Experiment: population 50, 1500 evaluations, 30 runs.
- Uses `TransformationPlaceholderRepairer`.

## Build

```bash
mvn -B -am -pl examples/at.ac.tuwien.big.momot.examples.refactoring clean verify
```

## Run

- Working directory = `examples/at.ac.tuwien.big.momot.examples.refactoring/`.
- Run a hand main or the generated runner. (Reduce `nrRuns` for a smoke run.)

## Expected outputs

- `model/output/referenceSet/approximation_set.pf`
- `model/output/solutions/` (result `.xmi` models)

## Verify

- `approximation_set.pf` non-empty; result models present; content-size objective decreases
  versus input.

## Known issues

- Wrong working directory -> `model/SeveralRefactorings.xmi` not found.
- Ensure `RefactoringPackage.eINSTANCE` is registered (script `initialization`).

## Done criteria

PASS when `approximation_set.pf` and result models are generated with plausible objective
values.
