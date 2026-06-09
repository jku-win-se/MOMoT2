---
name: runbook-emfrefactor
example: at.ac.tuwien.big.momot.examples.emfrefactor
when_to_use: To build, run, and verify the EMF Refactor (Ecore refactoring) example.
working_dir: examples/at.ac.tuwien.big.momot.examples.emfrefactor/
status: in Maven reactor; uses OCL (migrated API) and EMF Refactor metrics.
---

# Runbook: EMF Refactor

Refactors an Ecore metamodel by removing empty subclasses, scored with EMF Refactor metrics
and OCL queries.

## Inputs

- Operates on `EcorePackage` itself (the model being refactored is an Ecore file).
- Input model: `model/input/metamodel.ecore`
- Henshin: `transformation/refactorings/ecore/remove_empty_sub_eclass_all.henshin` (only the
  conditional `removeEmptySubEClass` unit is used; the helper units are listed in
  `ignoreUnits`)
- Script: `src/.../emfrefactor/emf.momot` (named search `EMFRefactorSearch`)

## Entrypoints

1. Hand-written: `EMFRefactoringOrchestration`, `RefactorSearch`, `Testing`.
2. Generated `*SearchExample` from `emf.momot`.

## Objectives and algorithms

- `SolutionLength` (min), `SubClasses` (min, via `NSUBEC` metric over all `EClass` domains).
- NSGA-III. Experiment: population 50, 1000 evaluations, 30 runs.
- Uses a name-based `equalityHelper` for `ENamedElement` matching.

## Build

```bash
mvn -B -am -pl examples/at.ac.tuwien.big.momot.examples.emfrefactor clean verify
```

## Run

- Working directory = `examples/at.ac.tuwien.big.momot.examples.emfrefactor/`.
- Run a hand main (`EMFRefactoringOrchestration`/`RefactorSearch`) or the generated runner.

## Expected outputs

- `model/output/metamodel/referenceSet.pf`
- `model/output/metamodel/solutions/` (result `.xmi` metamodels with empty subclasses removed)

## Verify

- `referenceSet.pf` has >= 1 row; refactored metamodels present; subclass count reduced versus
  input.

## Known issues

- OCL API: must use `org.eclipse.ocl.ecore.OCL` (see `metric/OCLManager.java`,
  `MIGRATION.md` item 3). A generic `org.eclipse.ocl.OCL` import will not compile.
- If OCL evaluation fails at runtime, the OCL delegate domain may need initialization.
- Wrong working directory -> `model/input/metamodel.ecore` not found.

## Done criteria

PASS when `referenceSet.pf` and refactored metamodels are generated and the subclass metric is
reduced relative to the input.
