---
name: run-an-example
when_to_use: After a successful build, to execute an example's search and produce result artifacts.
inputs: A built example module and its runbook entry from the catalog.
outputs: A finished search run with result files written under the example's output directory.
---

# Skill 02: Run an Example (native)

Examples are run as plain Java applications. There is no headless REST runner on this branch
(that lives on the standalone branch); execution here is native Tycho/Java.

## The single most important rule: working directory

Every `.momot` script and Java main uses RELATIVE input/output paths (`model/...`,
`data/...`, `problem/...`, `output/...`). The JVM working directory MUST be the example
project root, for example:

```
examples/at.ac.tuwien.big.momot.examples.stack/
```

If the working directory is wrong you will see `FileNotFoundException` for the input model,
or the run "succeeds" but writes nothing where you expect. This is the number-one cause of
"not generating results". See [07-diagnose-failures.md](07-diagnose-failures.md).

## Choose an entrypoint

For each example the catalog lists two runnable forms. Prefer in this order:

1. The generated `*SearchExample` main (canonical; mirrors the `.momot` script exactly).
2. The hand-written `*Search` / `*Orchestration` main (programmatic equivalent; sometimes the
   only one with a `main`, e.g. ecore's `ModularizationSearch`).

Find mains and confirm the class:

```bash
# from the example module root
grep -rn "static void main" src src-gen
```

## Run via Eclipse (most reliable)

1. Import the reactor as existing Maven/PDE projects with the `2026-03` target platform set.
2. Run As -> Java Application on the chosen main class.
3. In the Run Configuration -> Arguments tab, set the working directory to the example
   project root (`${workspace_loc:/<example-project>}`).
4. Confirm the registered EPackage import is honored (the script `initialization` block does
   this automatically).

## Run from the command line (advanced)

You must reproduce the full OSGi/PDE classpath (MOMoT plugins + Henshin + MOEA + OCL/ATL for
examples that need them). The reactor build does not wire an `exec`/`shade` goal, so this is
not a single canned command. General shape:

```bash
cd examples/at.ac.tuwien.big.momot.examples.stack
java -cp "<momot+deps classpath>" \
  at.ac.tuwien.big.momot.examples.stack.momot.StackSearchExample
```

If you need a repeatable headless command, prefer wiring an explicit run (see
[08-author-new-example.md](08-author-new-example.md)) or fall back to Eclipse. Do not invent
a classpath; derive it from the built `target/` directories and the resolved target platform,
and record the exact command in the run report.

## Tuning run cost

Scripts ship research-scale settings (e.g. `maxEvaluations = 21000`, `nrRuns = 30`). For a
fast smoke run, temporarily lower `populationSize`, `maxEvaluations`, and `nrRuns` in the
`.momot` script or the main, then restore them. Stack is the cheapest example to validate the
pipeline end to end.

## Done criteria

- The process exits without an exception.
- Result files appear under the example's expected output directory.
- Verify with [06-results-and-verification.md](06-results-and-verification.md).
