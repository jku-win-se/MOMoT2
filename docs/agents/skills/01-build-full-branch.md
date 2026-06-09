---
name: build-full-branch
when_to_use: To compile the plugins and example modules before running anything, or when a run fails with missing/stale classes.
inputs: A verified toolchain (skill 00).
outputs: Compiled plugins and example bundles; generated *SearchExample.java sources; a green reactor build.
---

# Skill 01: Build the Full Branch

The full branch is a Maven/Tycho reactor. The root [`pom.xml`](../../../pom.xml) aggregates
the MOMoT plugins, the `examples` aggregator, features, releng, tests, and the tooling
target-platform module.

## Reactor modules

From the root pom `<modules>`:

- `plugins/at.ac.tuwien.big.moea` - search/optimization core (MOEA integration).
- `plugins/at.ac.tuwien.big.momot.core` - MOMoT transformation+search engine.
- `plugins/at.ac.tuwien.big.momot.lang` - the `.momot` DSL (Xtext/Xtend); generates
  `*SearchExample.java` from scripts.
- `plugins/at.ac.tuwien.big.momot.lang.ui`, `plugins/at.ac.tuwien.big.momot.ui.popup`,
  `plugins/at.ac.tuwien.big.momot.examples.wizards` - UI.
- `examples` - the example aggregator ([`examples/pom.xml`](../../../examples/pom.xml)).
- `features`, `releng`, `tests`, `tooling/at.ac.tuwien.big.momot.tooling`.

Example modules built by the reactor: `cra`, `ecore`, `emfrefactor`, `modularization.jsme`,
`refactoring`, `stack`. TSE is NOT in the reactor.

## Build commands

Full reactor build (run from the repo root):

```bash
mvn -B clean verify
```

Build only the engine plus one example (faster iteration), for example the stack example:

```bash
mvn -B -am -pl examples/at.ac.tuwien.big.momot.examples.stack clean verify
```

Notes:
- `momot.lang` uses pre-generated Xtend sources from `xtend-gen/` and is built with
  `-Dxtend.skip=true` to avoid a full Tycho target-platform Xtend resolution (see
  `MIGRATION.md`). Preserve that flag if it is configured in the build/profile.
- The MOMoT language generator turns each `.momot` script into a `*SearchExample.java` under
  the module's `src-gen/`. For `cra` only the `.gitignore` is committed, so the runnable
  generated class appears after a successful build.

## Known build failures and fixes (from MIGRATION.md)

| Symptom | Root cause | Fix |
| --- | --- | --- |
| `javax/inject/Provider` builder error in an example | stale Xtext builder/nature in a plain Java/PDE `.project` | Remove the Xtext builder and `org.eclipse.xtext.ui.shared.xtextNature` from the example `.project` (already done for the six reactor examples; re-check if reintroduced). |
| MWE2 type errors building `momot.lang` | old `Workflow { component = XtextGenerator { ... } }` form | Use the direct `XtextGenerator` root form in `GenerateMOMoT.mwe2`. |
| OCL compile error in EMF Refactor | generic `org.eclipse.ocl.OCL` removed | Use `org.eclipse.ocl.ecore.OCL` (done in `metric/OCLManager.java`). |
| Class file version / Xtend/ASM incompatibility | mixed JDK levels | Keep build JDK at 21, Java source/target 17, and the pinned `maven:3.9-eclipse-temurin-21` base for Docker. |
| Missing bundle during target resolution | bundle absent from `2026-03.target` | Add it to the target file (see skill 00). |

## Done criteria

- `mvn -B clean verify` completes with `BUILD SUCCESS`.
- Each reactor example has compiled classes under `target/classes` and, where applicable, a
  generated `*SearchExample.java` under `src-gen/`.
