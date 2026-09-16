# Migration Guide

This repository was migrated to work with newer Eclipse/Tycho/Xtext tooling and a modern Java 17+ development setup. This file summarizes the changes made across the sessions so the rationale is documented in one place.

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
- All TSE bundles were raised from `JavaSE-1.7` to `JavaSE-1.8` in their manifests.
- Their JDT classpath containers were also updated from `JavaSE-1.7` to `JavaSE-1.8`.
- The compiler preferences were updated from source/compliance/target `1.7` to `1.8`.

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

## Validation
- Full reactor validation passed with:
  - `mvn -DskipTests=true verify`
- Result:
  - `BUILD SUCCESS`
  - 22/22 reactor modules completed successfully

## Notes
- The migration intentionally favors compatibility with newer Eclipse tooling over preserving legacy build metadata that was no longer needed.
- Some generated or serialized modeling artifacts changed format as a side effect of opening/saving them with newer Eclipse components.
- If Eclipse still shows stale markers after pulling these changes, refresh the projects and run a clean build in the IDE.
