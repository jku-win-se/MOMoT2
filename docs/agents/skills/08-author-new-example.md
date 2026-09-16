---
name: author-new-example
when_to_use: To add a new example module, or to wire an existing un-built family (e.g. TSE) into the Maven reactor.
inputs: A domain metamodel, Henshin rules, an input model, and objectives.
outputs: A reactor-built, runnable example with verifiable outputs.
---

# Skill 08: Author or Wire a New Example

Use this when adding a new example or bringing an existing PDE-only family (TSE) into the
build.

## Anatomy of an example module

Mirror an existing reactor example (the stack example is the cleanest template). A module
contains:

- `META-INF/MANIFEST.MF`, `build.properties`, `plugin.xml`, `pom.xml`, `.project`,
  `.classpath`.
- A metamodel: `*.ecore` (+ `*.genmodel`), with generated EMF model classes under `src` or
  `src-gen`.
- Henshin rules: `*.henshin` (+ `*_diagram`).
- One or more input models: `*.xmi`.
- A `.momot` script under `src/...`; the language generator emits `*SearchExample.java` into
  `src-gen/`.
- Optional hand-written `*Search` / `*Orchestration` mains.

## Wire it into the build

1. Add the module to [`examples/pom.xml`](../../../examples/pom.xml) `<modules>`.
2. Give it a `pom.xml` with the example parent and `eclipse-plugin` packaging consistent with
   the sibling examples.
3. Ensure the `.project` is a plain Java/PDE project (no Xtext nature) to avoid the
   `javax/inject/Provider` builder error (MIGRATION item 4).
4. Keep manifest/JDT Java levels consistent with the reactor (source/target 17, run on 21).

## Make it runnable headless (optional but recommended)

Since the reactor does not wire an `exec`/`shade` goal, a new example is otherwise only
runnable from Eclipse. To get a repeatable command-line run, add an explicit execution (for
example a Maven `exec:java` profile or a shaded runnable jar) that:
- sets the working directory to the module root, and
- includes the MOMoT engine + Henshin + MOEA (+ OCL/ATL if needed) on the classpath.

Record the exact command so the coordinator can reuse it.

## Wiring TSE specifically

TSE (`examples/tse/*`) has no `pom.xml` and is not in the reactor. To build it with Maven you
would: add a `examples/tse/pom.xml` aggregator, add `pom.xml` to each of the seven bundles,
add `examples/tse` (or each bundle) to the parent `<modules>`, and confirm dependencies
(Commons Lang 3, OCL, ATL) resolve from the target platform. Until then, treat TSE as
Eclipse-only and select a concrete case study in `MOMoTSearch.main` before running.

## Done criteria

- `mvn -B clean verify` builds the new module.
- The generated `*SearchExample` (or a hand main) runs from the module root and writes
  verifiable results per [06-results-and-verification.md](06-results-and-verification.md).
