# Migration Guide

This repository was migrated to work with newer Eclipse/Tycho/Xtext tooling, a modern Java 17+ development setup, and **MOEA Framework 5.1** (from 2.12). This file summarizes the changes so the rationale is documented in one place.

## What Changed

### 1. Build and toolchain modernization
- The main build was validated with Tycho 4.0.10 against the Eclipse 2026-03 target platform.
- The target platform was extended with missing bundles required by the current codebase, including:
  - `org.eclipse.gef`
  - `org.eclipse.draw2d`
  - `org.eclipse.gmf.runtime.common.core`
  - `org.eclipse.gmf.runtime.common.ui`
  - `org.hamcrest`
  - `org.junit`
  - `org.eclipse.m2m.atl.engine`
  - `org.eclipse.m2m.atl.engine.emfvm.launch`
  - `org.eclipse.m2m.atl.dsls`
  - `org.apache.commons.cli`
- These additions were made in [tooling/at.ac.tuwien.big.momot.tooling/targetplatform/2026-03.target](tooling/at.ac.tuwien.big.momot.tooling/targetplatform/2026-03.target).

### 2. Xtext/MWE2 migration
- The MOMoT language workflow was updated from an older `Workflow { component = XtextGenerator { ... } }` style to the direct `XtextGenerator` root form.
- This fixes the modern MWE2 type expectations in recent Xtext releases.
- The change was applied in [plugins/at.ac.tuwien.big.momot.lang/src/at/ac/tuwien/big/momot/lang/GenerateMOMoT.mwe2](plugins/at.ac.tuwien.big.momot.lang/src/at/ac/tuwien/big/momot/lang/GenerateMOMoT.mwe2).

### 3. OCL API migration
- The EMF Refactor example migrated from the generic OCL API to the Ecore-specific API.
- `org.eclipse.ocl.OCL` was replaced with `org.eclipse.ocl.ecore.OCL` in [examples/at.ac.tuwien.big.momot.examples.emfrefactor/src/at/ac/tuwien/big/momot/examples/emfrefactor/metric/OCLManager.java](examples/at.ac.tuwien.big.momot.examples.emfrefactor/src/at/ac/tuwien/big/momot/examples/emfrefactor/metric/OCLManager.java).
- This matches the current OCL API shape and avoids the removed generic factory pattern.

### 4. Example project cleanup
- Several example projects had stale Xtext builder entries in their `.project` files even though they are plain Java/PDE bundles.
- The Xtext builder and `org.eclipse.xtext.ui.shared.xtextNature` were removed from the following projects:
  - `examples/at.ac.tuwien.big.momot.examples.cra`
  - `examples/at.ac.tuwien.big.momot.examples.ecore`
  - `examples/at.ac.tuwien.big.momot.examples.emfrefactor`
  - `examples/at.ac.tuwien.big.momot.examples.modularization.jsme`
  - `examples/at.ac.tuwien.big.momot.examples.refactoring`
  - `examples/at.ac.tuwien.big.momot.examples.stack`
- This eliminated the Eclipse builder error that surfaced as `javax/inject/Provider`.

### 5. TSE example modernization
The TSE examples under [examples/tse](examples/tse) were migrated to a newer baseline.

#### Java level update
- All TSE bundles were first raised from `JavaSE-1.7` to `JavaSE-1.8`, then to `JavaSE-17` to match the rest of the reactor (source/target 17, JDK 21).
- Their JDT classpath containers and compiler preferences follow the same Java 17 baseline.

#### Apache Commons migration
- Usage of Apache Commons Lang 2.x was migrated to Commons Lang 3.
- Updated imports include:
  - `org.apache.commons.lang3.StringUtils`
  - `org.apache.commons.lang3.time.StopWatch`
- The affected files include:
  - [examples/tse/at.ac.tuwien.big.momot.examples.tse.momot/src/at/ac/tuwien/big/momot/examples/tse/momot/util/OrchestrationUtil.java](examples/tse/at.ac.tuwien.big.momot.examples.tse.momot/src/at/ac/tuwien/big/momot/examples/tse/momot/util/OrchestrationUtil.java)
  - [examples/tse/at.ac.tuwien.big.momot.examples.tse.metric/src/at/ac/tuwien/big/momot/examples/tse/metric/Metrics.java](examples/tse/at.ac.tuwien.big.momot.examples.tse.metric/src/at/ac/tuwien/big/momot/examples/tse/metric/Metrics.java)
  - [examples/tse/at.ac.tuwien.big.momot.examples.tse.rdg/src/at/ac/tuwien/big/momot/examples/tse/rdg/RDGExperiment.java](examples/tse/at.ac.tuwien.big.momot.examples.tse.rdg/src/at/ac/tuwien/big/momot/examples/tse/rdg/RDGExperiment.java)
  - [examples/tse/at.ac.tuwien.big.momot.examples.tse.rdg/src/at/ac/tuwien/big/momot/examples/tse/rdg/RDGExample.java](examples/tse/at.ac.tuwien.big.momot.examples.tse.rdg/src/at/ac/tuwien/big/momot/examples/tse/rdg/RDGExample.java)

#### Stale dependency removal
- The following obsolete manifest requirements were removed where the source no longer depends on them:
  - `org.eclipse.ocl.examples.library`
  - `org.eclipse.ocl.examples.pivot`
  - `org.eclipse.ocl.examples.codegen`
  - `org.eclipse.emf.emfstore.common`
- In `at.ac.tuwien.big.momot.examples.tse.momot`, the old external `org.apache.commons.lang` dependency was replaced with `org.apache.commons.lang3`.
- In `at.ac.tuwien.big.momot.examples.tse.metric` and `at.ac.tuwien.big.momot.examples.tse.rdg`, `org.apache.commons.lang` was replaced with `org.apache.commons.lang3`.
- In `at.ac.tuwien.big.momot.examples.tse.momot`, the old local `lib/commons-cli-1.3.1.jar` classpath entry was dropped from the bundle metadata and the bundle now relies on the Orbit-provided `org.apache.commons.cli` bundle.

#### ATL integration
- The TSE resources bundle still uses ATL APIs, so the target platform was extended with the ATL 4.9.0 repository.
- The existing ATL-based code was kept, but now resolves through the current Eclipse p2 repository instead of failing IDE resolution.

### 6. Legacy modeling metadata cleanup
- Several Sirius/AIRD and generated-model resources were refreshed by the tooling during the migration.
- These updates are not behavior changes; they are compatibility-oriented file format refreshes caused by the newer Eclipse stack.

### 7. MOEA Framework upgrade (2.12 to 5.1)

MOMoT’s search engine used to ship **MOEA Framework 2.12** (2016-era). The bundled library is now **[MOEA Framework 5.1](https://github.com/MOEAFramework/MOEAFramework/releases/tag/v5.1)** (`plugins/at.ac.tuwien.big.moea/lib/MOEAFramework-5.1.jar`). This is a breaking upgrade of the optimization runtime, not a JAR drop-in.

#### Why upgrade
2.12 is unmaintained. 5.x is the current line (5.0 in January 2025, 5.1 in June 2025) and is what new MOEA documentation and algorithms target. For MOMoT that means:

- **Actively maintained algorithms and indicators** — NSGA-II/III, ε-MOEA, SPEA2, SMS-EMOA, and quality indicators keep receiving fixes instead of freezing on a decade-old snapshot.
- **Typed objectives and constraints** — 5.0 introduces `Objective` / `Constraint` (including explicit minimize vs maximize). Fitness is written with `setObjectiveValue` / `setObjectiveValues` instead of treating `getObjective(int)` as a `double`.
- **Clearer package layout** — populations live in `org.moeaframework.core.population`, selection in `org.moeaframework.core.selection` (`TournamentSelection` moved off `core.operator`), runtime collectors in `org.moeaframework.analysis.runtime`. Import errors after the upgrade are almost always a package move.
- **Explicit algorithm configuration** — constructors such as `NSGAII` and `RandomSearch` take `populationSize` (NSGA-III populations take `NormalBoundaryDivisions`). Search size is no longer implied by a side-channel.
- **Experiment APIs that match 5.x** — `Executor` and `Analyzer` were removed. MOMoT’s `SearchExecutor` is a standalone runner (checkpointing, instrumentation, fresh algorithm instance per seed). `SearchAnalyzer` uses `IndicatorStatistics`.
- **Java 17 alignment** — 5.x is a modern Java library, which matches this branch’s JDK 21 / source 17 toolchain instead of a Java 7-era 2.12 JAR.
- **5.1 extras** — parallel sample evaluation (`Samples.distributeAll`), a simpler data-store API, and plot builders. MOMoT does not have to use every 5.1 surface, but the bundled JAR is current.

#### Engine changes
- Bundle metadata (`build.properties`, `META-INF/MANIFEST.MF`, `.classpath`) now references `MOEAFramework-5.1.jar` and exports the 5.x packages.
- `SearchExecutor` no longer extends `org.moeaframework.Executor`.
- `SearchAnalyzer` no longer extends `org.moeaframework.Analyzer`.
- `PopulationUtil` replaced removed `PopulationIO`; variables implement 5.x `getName` / `getDefinition` / `encode` / `decode`; mutations implement `Mutation.mutate(Solution)`.
- Algorithm factories (`EvolutionaryAlgorithmFactory`) pass `populationSize` into `NSGAII`, `RandomSearch`, `EpsilonMOEA`, `SPEA2`, `PESA2`, and `SMSEMOA`.

#### Examples and DSL
All in-reactor examples and `.momot` scripts use 5.1 packages (`org.moeaframework.core.selection.TournamentSelection`, `org.moeaframework.core.population.*`). TSE Java sources (`RDGExample`, `RDGExperiment`, `RDGProblem`, `MOMoTSearch`, …) use the same constructors and `setObjectiveValue`.

The six TSE Java bundles are now Maven/Tycho modules listed in [`examples/pom.xml`](examples/pom.xml). The ATL-based `tse.resources` bundle still has a `pom.xml` but is **not** a reactor module (it needs ATL from the target platform at runtime).

## Validation
- Toolchain: JDK 21, Maven 3.9+, Tycho 4.0.10, Eclipse target platform `2026-03`.
- Reactor compile includes the engine plugins, IDE tooling, tests, the six original examples, and the six TSE Java modules.
- Set the JVM working directory to the example project root before running a main (relative `model/`, `data/`, `output/` paths).

## Notes
- The migration intentionally favors compatibility with newer Eclipse tooling and a current MOEA runtime over preserving legacy build metadata that was no longer needed.
- Some generated or serialized modeling artifacts changed format as a side effect of opening/saving them with newer Eclipse components.
- If Eclipse still shows stale markers after pulling these changes, refresh the projects and run a clean build in the IDE.
- Do not mix MOEA 2.12 imports (`org.moeaframework.core.operator.TournamentSelection`, `org.moeaframework.Executor`, `setObjective(int, double)`) with this branch.
