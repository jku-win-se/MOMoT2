---
name: architecture
purpose: How the full branch builds and runs MOMoT examples, and how it differs from the standalone branch.
when_to_use: To orient on the build/run topology before diving into a specific example.
---

# Full-Branch Architecture

The `full` branch is the example-rich research codebase: the MOMoT engine plus seven example
families, built as a Maven/Tycho reactor and run as native Java applications. It does not
include the standalone branch's headless REST/Docker runner.

## What MOMoT does

MOMoT marries Model-Driven Engineering with Search-Based Software Engineering: it treats a
sequence of model-transformation rule applications (a Henshin orchestration) as a candidate
solution, and uses multi-objective metaheuristics (MOEA Framework: NSGA-II/III, eMOEA, local
search) to optimize models against domain objectives (coupling, cohesion, MQ, solution
length, ...).

## Layers

```mermaid
flowchart TD
  subgraph dsl [Authoring]
    Script[".momot script"]
    Gen["momot.lang generator"]
    Script --> Gen --> SearchExample["generated *SearchExample.java"]
  end

  subgraph engine [Engine plugins]
    Moea["at.ac.tuwien.big.moea (search/MOEA)"]
    Core["momot.core (transformation + search)"]
  end

  subgraph run [Run an example]
    Main["Java main: *SearchExample or *Search/*Orchestration"]
    Henshin["Henshin rules (.henshin)"]
    Model["input model (.xmi/.ecore)"]
    Out["results: .pf / .xmi / analysis.txt"]
    Main --> Henshin
    Main --> Model
    Main --> Out
  end

  SearchExample --> Main
  Core --> Main
  Moea --> Core
```

## Build topology

```mermaid
flowchart LR
  Root["root pom (Tycho 4.0.10, Java 17/JavaSE-21)"] --> Plugins["plugins/* (moea, core, lang, ui)"]
  Root --> Examples["examples/pom.xml"]
  Root --> Tooling["tooling (2026-03 target platform)"]
  Root --> Features
  Root --> Releng
  Root --> Tests
  Examples --> cra
  Examples --> ecore
  Examples --> emfrefactor
  Examples --> jsme["modularization.jsme"]
  Examples --> refactoring
  Examples --> stack
  TSE["examples/tse/* (NOT wired)"] -.-> Examples
```

The TSE family lives under `examples/tse/` but is not referenced by any pom; it is Eclipse/PDE
only (see [../runbooks/tse.md](../runbooks/tse.md)).

## Run model (native)

```mermaid
flowchart LR
  Cwd["working dir = example project root"] --> Jvm["JVM runs the example main"]
  Jvm --> Reg["initialization registers domain EPackage"]
  Reg --> Load["load input model + Henshin rules (relative paths)"]
  Load --> Search["MOEA search over transformation orchestrations"]
  Search --> Write["write .pf / .xmi / analysis under output dir"]
```

The relative-path + working-directory contract is the most failure-prone link; see
[../skills/02-run-an-example.md](../skills/02-run-an-example.md).

## Full vs standalone

| Aspect | full (this branch) | standalone |
| --- | --- | --- |
| Contents | engine + 7 example families | engine + MCP server + REST runner + Docker |
| Run mechanism | native Java mains / generated runners | headless `/run` zip-in/zip-out over REST in Docker |
| Docs | this `docs/agents/` scaffolding | `doc/00`-`09`, `GEMINI.md` |
| Primary goal | make every example build, run, and produce results | reproducible headless job execution |

When a headless, reproducible run is needed, the standalone branch's approach can be ported,
but on `full` the supported path is native execution.
