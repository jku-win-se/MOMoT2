---
name: environment-setup
when_to_use: Before any build or run. Establishes the JDK, Maven, Tycho, and target-platform prerequisites for the full branch.
inputs: A developer machine or CI runner.
outputs: A verified toolchain (JDK 21, Maven 3.9+, Tycho 4.0.10) ready to build the reactor.
---

# Skill 00: Environment Setup

Establish the toolchain before building or running any example. These facts come from the
root [`pom.xml`](../../../pom.xml) and [`MIGRATION.md`](../../../MIGRATION.md).

## Required toolchain

- JDK: install a JDK 21 (execution environment `JavaSE-21`). The build compiles sources at
  Java 17 level (`java.source=17`, `java.target=17`) but runs on a 21 runtime.
- Maven: 3.9+ (the standalone branch's Docker build pins `maven:3.9-eclipse-temurin-21`).
- Tycho: `4.0.10` (declared as `tycho-version` in the root pom; downloaded by Maven, no
  manual install).
- Eclipse target platform: `2026-03`, defined in
  [`tooling/at.ac.tuwien.big.momot.tooling/targetplatform/2026-03.target`](../../../tooling/at.ac.tuwien.big.momot.tooling/targetplatform/2026-03.target).
- For running examples interactively, an Eclipse IDE with PDE + the resolved target platform
  is the most reliable host (see [02-run-an-example.md](02-run-an-example.md)).

## Verify the toolchain

```bash
java -version      # expect a 21.x runtime
mvn -version       # expect Maven 3.9+, and the JDK it reports should be 21
```

## Target platform contents that were added during migration

The `2026-03.target` was extended with bundles the current code needs. If target-platform
resolution fails, confirm these are present (full list in `MIGRATION.md` item 1):

- `org.eclipse.gef`, `org.eclipse.draw2d`
- `org.eclipse.gmf.runtime.common.core`, `org.eclipse.gmf.runtime.common.ui`
- `org.hamcrest`, `org.junit`
- `org.eclipse.m2m.atl.engine`, `org.eclipse.m2m.atl.engine.emfvm.launch`, `org.eclipse.m2m.atl.dsls`
- `org.apache.commons.cli`

## Runtime dependencies the examples need

The search engine pulls in Henshin (model transformation), MOEA Framework (optimization),
and for some examples OCL and ATL. In a full Tycho/PDE build these come from the target
platform. When running a built example outside Eclipse you must reproduce that classpath
(see [02-run-an-example.md](02-run-an-example.md) and [07-diagnose-failures.md](07-diagnose-failures.md)).

## Done criteria

- `java -version` reports a 21 runtime.
- `mvn -version` reports Maven 3.9+ on JDK 21.
- The `2026-03.target` resolves (no missing-bundle errors when the reactor builds the
  `momot.tooling` / `momot.lang` modules).
