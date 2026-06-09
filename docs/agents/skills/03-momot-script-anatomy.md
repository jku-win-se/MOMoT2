---
name: momot-script-anatomy
when_to_use: To read, understand, or edit a .momot script, or to map a script block to the artifact it depends on.
inputs: A .momot script.
outputs: A clear mental model of each script block and the files/classes it references.
---

# Skill 03: Anatomy of a `.momot` Script

A `.momot` script is the MOMoT DSL that declares a search-based model-transformation
experiment. The language generator compiles it into a `*SearchExample.java`. Every example
on this branch follows the same block structure. (This adapts the standalone branch's
`doc/01`-`doc/07` to the example scripts.)

Reference scripts to read alongside this skill:
- Simplest: [`stack/StackSearchExample.momot`](../../../examples/at.ac.tuwien.big.momot.examples.stack/src/at/ac/tuwien/big/momot/examples/stack/StackSearchExample.momot)
- With OCL objective: [`refactoring/Refactoring.momot`](../../../examples/at.ac.tuwien.big.momot.examples.refactoring/src/at/ac/tuwien/big/momot/examples/refactoring/Refactoring.momot)
- With preprocess + constraints: [`modularization.jsme/ModularizationJSEP.momot`](../../../examples/at.ac.tuwien.big.momot.examples.modularization.jsme/src/at/ac/tuwien/big/momot/examples/modularization/jsme/ModularizationJSEP.momot)

## Block-by-block

### `package` and `import`
Declares the generated class's package and pulls in domain classes (the `EPackage`, factory,
model types) plus MOMoT/MOEA operators. Note the DSL escapes some identifiers with a caret,
e.g. `^search`, `^fitness`, `^experiment`, `^modules` - the caret is DSL syntax, not part of
the name.

### `initialization`
Runs once before the search. Its critical job is registering the domain `EPackage` so EMF can
load the model standalone, e.g. `StackPackage.eINSTANCE.class`. Missing this is a common
"runs but loads nothing" failure.

### `search`
The core declaration:
- `model = { file = "<relative path>" }` - the input model (and optionally an `adapt` block
  that mutates the loaded model before search, as in CRA).
- `solutionLength` - max number of orchestrated transformation-unit applications.
- `transformations = { modules = [ "<relative .henshin>" ] ... }` - the Henshin rules; may
  declare `ignoreUnits`, `ignoreParameters`, and `parameterValues`. See the per-example
  runbooks for which units are searched.
- `fitness = { ... }` - objectives and (optionally) constraints. See
  [04-objectives-and-fitness.md](04-objectives-and-fitness.md).
- `algorithms = { ... }` - the search algorithms. See
  [05-search-and-experiment.md](05-search-and-experiment.md).
- Optional `equalityHelper = { ... }` - custom object matching (e.g. name-based in EMF
  Refactor and JSME).

### `experiment`
`populationSize`, `maxEvaluations`, `nrRuns`, and `progressListeners`. These define the run
cost; lower them for smoke tests.

### `analysis` (optional)
Quality indicators (`hypervolume`, `generationalDistance`, `additiveEpsilonIndicator`, ...),
significance level, what to `show`, and an `outputFile`/`boxplotDirectory`.

### `results`
Declares what artifacts to persist: `objectives` (Pareto fronts `.pf`), `solutions`
(transformation orchestrations), and `models` (resulting `.xmi`). Each block sets an
`outputFile` and/or `outputDirectory` and may restrict to specific `algorithms` or select
knee points via `neighborhoodSize`. See
[06-results-and-verification.md](06-results-and-verification.md).

### `finalization` (optional)
Runs once after the search (often just a completion log line).

## Path dependency map (what each block needs on disk)

| Block field | Depends on | Failure if missing |
| --- | --- | --- |
| `search.model.file` | input `.xmi`/`.ecore` (relative) | model not found -> no run |
| `transformations.modules` | `.henshin` file (relative) | rules not loaded -> no transformations |
| `initialization` | generated `EPackage` class on classpath | EMF cannot resolve types |
| `results.*.outputFile/outputDirectory` | writable relative path | results silently not written |

Always cross-check these against the working directory rule in
[02-run-an-example.md](02-run-an-example.md).
