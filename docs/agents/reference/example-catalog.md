---
name: example-catalog
purpose: Authoritative dispatch table of every MOMoT example on the full branch.
when_to_use: The coordinator reads this first to pick the right runbook, entrypoint, working directory, and expected outputs for a given example.
inputs: An example name or module id.
outputs: Module path, domain, metamodel, Henshin rules, input model, entrypoint class(es), working directory, expected result artifacts, build status.
---

# MOMoT Example Catalog

This is the single source of truth that maps every example to how it is built, run, and
verified. The coordinator dispatches work from this table. Per-example detail lives in
[../runbooks/](../runbooks/).

## Build status at a glance

| Example | Module id | In Maven reactor? | Runbook |
| --- | --- | --- | --- |
| Stack | `at.ac.tuwien.big.momot.examples.stack` | Yes | [stack.md](../runbooks/stack.md) |
| CRA | `at.ac.tuwien.big.momot.examples.cra` | Yes | [cra.md](../runbooks/cra.md) |
| Ecore modularization | `at.ac.tuwien.big.momot.examples.ecore` | Yes | [ecore.md](../runbooks/ecore.md) |
| EMF Refactor | `at.ac.tuwien.big.momot.examples.emfrefactor` | Yes | [emfrefactor.md](../runbooks/emfrefactor.md) |
| JSME modularization | `at.ac.tuwien.big.momot.examples.modularization.jsme` | Yes | [modularization-jsme.md](../runbooks/modularization-jsme.md) |
| Refactoring | `at.ac.tuwien.big.momot.examples.refactoring` | Yes | [refactoring.md](../runbooks/refactoring.md) |
| TSE (family) | `examples/tse/*` (7 bundles) | No (PDE only, no `pom.xml`) | [tse.md](../runbooks/tse.md) |

The Maven reactor membership is defined in [`examples/pom.xml`](../../../examples/pom.xml).
TSE is intentionally absent there; treat it as Eclipse/PDE only until a pom is added (see
[../skills/08-author-new-example.md](../skills/08-author-new-example.md)).

## Cross-cutting facts (apply to every example)

- Working directory: every `.momot` script and Java main uses RELATIVE paths
  (`model/...`, `data/...`, `problem/...`, `output/...`). The JVM working directory MUST be
  the example project root, otherwise inputs are not found and no results are written.
- Standalone EPackage registration: each script's `initialization` block registers the
  domain `EPackage` (for example `StackPackage.eINSTANCE`). This is mandatory when running
  headless outside an Eclipse runtime.
- Two runnable forms per example:
  1. The generated `*SearchExample.java` (produced from the `.momot` script by the MOMoT
     language generator during the build) - the canonical runner.
  2. Hand-written `*Search` / `*Orchestration` mains that build the search programmatically.
- Result artifacts: Pareto-front files (`.pf`), solution/transformation dumps (`.txt`),
  result models (`.xmi`), and an `analysis.txt`. See
  [../skills/06-results-and-verification.md](../skills/06-results-and-verification.md).

## Per-example detail

### Stack (canonical reference example)

- Domain: balance the load across a set of stacks (toy problem, fastest to run).
- Metamodel: `model/stack.ecore` (`StackModel`, `Stack`, `load`).
- Henshin: `model/stack.henshin` (also `model/stackNested.henshin`); units `ShiftLeft`,
  `ShiftRight` are searched; `CreateStack`, `ConnectStacks` are ignored.
- Input model: `model/input/model/model_five_stacks.xmi`.
- Script: `src/.../stack/StackSearchExample.momot` (package `...stack.momot`).
- Generated runner: `src-gen/.../stack/momot/StackSearchExample.java` (has `main`).
- Hand-written mains: `StackOrchestration`, `StackSearch`, `StackEvalSearch`,
  `comparison/NativeStackExample`, `comparison/Comparison`.
- Objectives: `StandardDeviation` (min), `SolutionLength` (min). Algorithms: Random,
  NSGA-II, NSGA-III.
- Working dir: `examples/at.ac.tuwien.big.momot.examples.stack/`.
- Expected outputs (generated runner): `example/output/overall_objectives.pf`,
  `example/output/moea_objectives.pf`, `example/output/analysis.txt`,
  `example/output/solutions/`, `example/output/models/`.
- Committed reference results: `model/output/` and `model/output_test/` (compare against
  these for a sanity check).

### CRA (Class Responsibility Assignment)

- Domain: assign features (methods/attributes) to classes minimizing coupling and
  maximizing cohesion.
- Package: `icmt.tool.momot.demo`.
- Metamodel: `metamodel/architecture.ecore` (`ClassModel`, `Class`, `Feature`, `Method`,
  `Attribute`).
- Henshin: `transformations/architecture.henshin`.
- Input model: `problem/Cart_Item.xmi` (reference solution: `problem/Cart_Item_solution.xmi`).
- Script: `src/icmt/tool/momot/demo/ArchitectureSearch.momot`.
- Generated runner: produced into `src-gen/` at build time (only `.gitignore` is committed).
- Helper: `FitnessCalculator.java` (`calculateCoupling`, `calculateCohesion`).
- Objectives: `CouplingRatio` (min), `CohesionRatio` (max), `SolutionLength` (min).
  Algorithms: Random, NSGA-III, eMOEA.
- Working dir: `examples/at.ac.tuwien.big.momot.examples.cra/`.
- Expected outputs: `output/analysis/analysis.txt`, `output/objectives/`,
  `output/solutions/`, `output/models/`, `output/models/kneepoints/`.

### Ecore modularization

- Domain: modularize a (large) Ecore metamodel into cohesive modules; uses an ATL
  conversion to a generic modularization metamodel.
- Metamodel: `metamodel/Generic_Modularization_MM.ecore`.
- Henshin: `operations/modularization_rules.henshin`.
- Input models: `input/QVT_module.xmi`, `input/HTML_module.xmi`, `input/JAVA_module.xmi`,
  `input/OCL_module.xmi`.
- Conversion: `conversion/Ecore2Modularization.atl` (+ `.asm`, `.launch`).
- Script: `src/.../ecore/ModularizationQVT.momot` (package `...ecore.derived`).
- Hand-written mains: `ModularizationSearch.java` (`main` -> `performSearch(CaseStudy.QVT)`),
  `CaseStudy.java` (enum of case studies), `FindKneePoint.java`,
  `CalculateFitnessAndKneeopint.java`.
- Fitness: `ModularizationFitnessFunction`, `MetricsCalculator` (coupling/cohesion).
  Script objectives: `Coupling` (min), `Cohesion` (max), `NrModules` (max),
  `MinMaxDiffTest` (min). Algorithm: NSGA-III.
- Extra classpath dependency: `lib/java-string-similarity-0.13.jar`.
- Working dir: `examples/at.ac.tuwien.big.momot.examples.ecore/`.
- Expected outputs (`ModularizationSearch`): `output/<langName>/nsgaii/approximation_nsgaii.pf`,
  `approximation_nsgaii.txt`, result models. Script variant writes `data/_test/approximationSet/QVT.pf`.

### EMF Refactor

- Domain: refactor an Ecore metamodel (remove empty subclasses) using EMF Refactor metrics
  and OCL queries.
- Operates on: `EcorePackage` itself; input `model/input/metamodel.ecore`.
- Henshin: `transformation/refactorings/ecore/remove_empty_sub_eclass_all.henshin`.
- Script: `src/.../emfrefactor/emf.momot` (named search `EMFRefactorSearch`).
- Hand-written mains: `EMFRefactoringOrchestration.java`, `RefactorSearch.java`, `Testing.java`.
- Metric helpers: `metric/OCLManager.java` (uses `org.eclipse.ocl.ecore.OCL`), `metric/NSUBEC.java`.
- Objectives: `SolutionLength` (min), `SubClasses` (min). Algorithm: NSGA-III.
- Working dir: `examples/at.ac.tuwien.big.momot.examples.emfrefactor/`.
- Expected outputs: `model/output/metamodel/referenceSet.pf`, `model/output/metamodel/solutions/`.
- Known migration note: OCL API moved from generic `org.eclipse.ocl.OCL` to
  `org.eclipse.ocl.ecore.OCL` (see `MIGRATION.md` item 3).

### JSME modularization

- Domain: software (source-code) modularization on case studies such as `mtunis`; uses the
  Modularization Quality (MQ) metric.
- Metamodel: JSME modularization metamodel (`ModularizationPackage`).
- Henshin: `data/modularization_jsep.henshin` (runtime variant for the `_Runtime` script).
- Input model: `data/input/models/mtunis.xmi`.
- Scripts: `ModularizationJSEP.momot`, `ModularizationJSEP_Runtime.momot`.
- Hand-written mains: `moea/ModularizationSearch.java`, `ModularizationJSEPSearchECA.java`,
  `ModularizationJSEPSearchHillClimbing.java`, `ModularizationJSEP_RuntimeSearch.java`,
  `ModularizationComparison.java`, `henshin/HenshinOnlyExplorer.java`,
  `ModularizationResultManager.java`.
- Objectives: `Coupling` (min), `Cohesion` (max), `NrModules` (max), `MQ` (max),
  `MinMaxDiff` (min), `SolutionLength` (min). Constraints: `UnassignedClasses`,
  `EmptyModules`. Algorithms: NSGA-III, eMOEA, Random.
- Working dir: `examples/at.ac.tuwien.big.momot.examples.modularization.jsme/`.
- Expected outputs: `data/output/approximationSet/*.pf`, `data/output/models/`,
  `data/output/analysis/`.

### Refactoring (generic model refactoring)

- Domain: apply refactoring transformations to a generic model.
- Metamodel: refactoring metamodel (`RefactoringPackage`).
- Henshin: `model/Refactoring.henshin`.
- Input model: `model/SeveralRefactorings.xmi`.
- Script: `src/.../refactoring/Refactoring.momot`.
- Hand-written mains: `RefactoringOrchestration.java`, `RefactoringSearch.java`.
- Objectives: `SolutionLength` (min), `ContentSize` (min, OCL
  `properties->size() * 1.1 + entities->size()`). Algorithms: NSGA-II, NSGA-III.
- Working dir: `examples/at.ac.tuwien.big.momot.examples.refactoring/`.
- Expected outputs: `model/output/referenceSet/approximation_set.pf`, `model/output/solutions/`.

### TSE family (legacy, not in Maven build)

- Bundles: `tse.eval`, `tse.metric`, `tse.metric.ruletype`, `tse.modularization`,
  `tse.momot`, `tse.rdg`, `tse.resources` under `examples/tse/`.
- Build status: NOT in the Maven reactor and has no `pom.xml`. PDE/Eclipse projects only.
  Java level was raised to 1.8 and Apache Commons Lang migrated to 3.x (see `MIGRATION.md`
  item 5).
- Mains: `tse.momot/MOMoTSearch` (every case study in `main` is currently commented out, so
  it produces no output by default), `MOMoTSearchAll`, `console/Modularize`,
  `orchestration/ModularizationOrchestration`; `tse.rdg/RDGExample`, `RDGSearch`;
  `tse.eval/PairwiseExample`.
- Known issues: commented-out entrypoints and OCL initialization; not reachable from the
  Maven build. Treat as advanced / opt-in until wired up.
