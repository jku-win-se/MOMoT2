---
name: runbook-ecore
example: at.ac.tuwien.big.momot.examples.ecore
when_to_use: To build, run, and verify the Ecore metamodel-modularization example.
working_dir: examples/at.ac.tuwien.big.momot.examples.ecore/
status: in Maven reactor; has a hand-written main with case studies; needs an extra lib on the classpath.
---

# Runbook: Ecore Modularization

Modularizes a (large) Ecore metamodel into cohesive modules. Input metamodels are first
converted via ATL into a generic modularization model, then optimized.

## Inputs

- Metamodel: `metamodel/Generic_Modularization_MM.ecore`
- Henshin: `operations/modularization_rules.henshin`
- Input models: `input/QVT_module.xmi` (default), `input/HTML_module.xmi`,
  `input/JAVA_module.xmi`, `input/OCL_module.xmi`
- ATL conversion: `conversion/Ecore2Modularization.atl` (+ `.asm`, `.launch`)
- Script: `src/.../ecore/ModularizationQVT.momot`
- Extra classpath dependency: `lib/java-string-similarity-0.13.jar`

## Entrypoints

1. Hand-written (canonical here): `at.ac.tuwien.big.momot.examples.ecore.ModularizationSearch`
   - `main` calls `performSearch(CaseStudy.QVT)`. Case studies are defined in `CaseStudy.java`
     (model, solution length, number of modules). To run another, change the enum value in
     `main`.
2. Generated `*SearchExample` from `ModularizationQVT.momot` (writes to `data/_test/...`).
3. Analysis helpers: `FindKneePoint`, `CalculateFitnessAndKneeopint`.

## Objectives and algorithms

- `Coupling` (min), `Cohesion` (max), `NrModules` (max), `MinMaxDiffTest` (min); uses a
  `preprocess` block computing metrics once per solution.
- NSGA-III (script). `ModularizationSearch.java` registers NSGA-II. Experiment in the script:
  population 300, 21000 evaluations, 30 runs. The Java main defaults to population 100, 2 runs
  (lighter).

## Build

```bash
mvn -B -am -pl examples/at.ac.tuwien.big.momot.examples.ecore clean verify
```

## Run

- Working directory = `examples/at.ac.tuwien.big.momot.examples.ecore/`.
- Ensure `lib/java-string-similarity-0.13.jar` is on the classpath (used by vocabulary
  distance metrics).
- Run `ModularizationSearch.main` (lighter, recommended) or the generated runner.

## Expected outputs

- From `ModularizationSearch`: `output/<langName>/nsgaii/approximation_nsgaii.pf`,
  `approximation_nsgaii.txt`, and result models (empty modules are cleaned from saved models).
  `<langName>` comes from the loaded `Language` (e.g. `QVT`).
- From the generated script runner: `output/_test/QVT.txt`, `data/_test/approximationSet/QVT.pf`,
  `data/_test/models/QVT/`.

## Verify

- The `.pf` exists with >= 1 row; result models present; console prints input metrics,
  reference set, approximation set, and knee-point objectives.

## Known issues

- `NoClassDefFoundError` for `info.debatty...` (string similarity) -> add the bundled
  `lib/java-string-similarity-0.13.jar`.
- Wrong working directory -> `input/QVT_module.xmi` not found.
- ATL runtime must resolve (`org.eclipse.m2m.atl.*` were added to the target platform).

## Done criteria

PASS when an approximation `.pf` and result models are generated and coupling/cohesion/module
metrics are printed and plausible.
