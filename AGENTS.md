# AGENTS

This is the `full` branch of MOMoT (the MOMoT engine plus seven example families), built as a
Maven/Tycho reactor and run as native Java applications.

## Start here

Read [docs/agents/coordinator.md](docs/agents/coordinator.md). It is the orchestrator: it
tells you what to do and which skill/runbook to load to build, run, diagnose, and verify the
examples so they generate results.

Full operating manual and index: [docs/agents/README.md](docs/agents/README.md).

## Non-negotiables

- Set the JVM working directory to the example project root before running anything (examples
  use relative `model/`, `data/`, `output/` paths). This is the #1 cause of "no results".
- Build with JDK 21 (source/target 17), Maven 3.9+, Tycho 4.0.10, target platform `2026-03`.
- The TSE family (`examples/tse/*`) is not in the Maven build; see
  [docs/agents/runbooks/tse.md](docs/agents/runbooks/tse.md).
- Background on the toolchain migration lives in [MIGRATION.md](MIGRATION.md).
