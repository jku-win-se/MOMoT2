---
name: runbook-modularization-jsme
example: at.ac.tuwien.big.momot.examples.modularization.jsme
when_to_use: To build, run, and verify the JSME software-modularization example.
working_dir: examples/at.ac.tuwien.big.momot.examples.modularization.jsme/
status: in Maven reactor; multiple algorithm variants; uses preprocess + constraints.
---

# Runbook: JSME Modularization

Modularizes software (e.g. the `mtunis` case study) using the Modularization Quality (MQ)
metric and coupling/cohesion objectives.

## Inputs

- Metamodel: JSME modularization metamodel (`ModularizationPackage`)
- Henshin: `data/modularization_jsep.henshin` (runtime variant for the `_Runtime` script)
- Input model: `data/input/models/mtunis.xmi`
- Scripts: `ModularizationJSEP.momot`, `ModularizationJSEP_Runtime.momot`

## Entrypoints

Multiple variants (pick one):
- `moea/ModularizationSearch` - MOEA-based search.
- `ModularizationJSEPSearchECA` - evolutionary computation algorithm variant.
- `ModularizationJSEPSearchHillClimbing` - local-search variant.
- `ModularizationJSEP_RuntimeSearch` - uses the runtime Henshin variant.
- `ModularizationComparison` - compares algorithms.
- `henshin/HenshinOnlyExplorer` - Henshin-only exploration (no MOMoT search).
- Generated `*SearchExample` from each `.momot` script.

## Objectives and algorithms

- `Coupling` (min), `Cohesion` (max), `NrModules` (max), `MQ` (max), `MinMaxDiff` (min),
  `SolutionLength` (min).
- Constraints: `UnassignedClasses`, `EmptyModules` (penalized).
- NSGA-III, eMOEA, Random. Experiment: population 300, 21000 evaluations, 30 runs (reduce for
  smoke runs).
- Uses a `preprocess` block (one `ModularizationCalculator` per solution) and a module-index
  `equalityHelper`.

## Build

```bash
mvn -B -am -pl examples/at.ac.tuwien.big.momot.examples.modularization.jsme clean verify
```

## Run

- Working directory = `examples/at.ac.tuwien.big.momot.examples.modularization.jsme/`.
- Run one entrypoint above (start with `moea/ModularizationSearch` or the generated runner for
  `ModularizationJSEP.momot`).

## Expected outputs

- `data/output/approximationSet/*.pf` (e.g. `mtunis_statistic2.pf`)
- `data/output/models/` (result modularizations)
- `data/output/analysis/` (e.g. `mtunis_statistic2.txt`)

## Verify

- `.pf` non-empty; result models present; MQ/coupling/cohesion plausible; constraint penalties
  should be ~0 for valid solutions (no unassigned classes / empty modules).

## Known issues

- Wrong working directory -> `data/input/models/mtunis.xmi` not found.
- High default budget makes full runs slow; lower `nrRuns`/`maxEvaluations` for validation.
- Ensure `ModularizationPackage.eINSTANCE` is registered (the script `initialization` does
  this).

## Done criteria

PASS when an approximation `.pf` and result modularizations are generated with valid (non
penalized) solutions and a reasonable MQ.
