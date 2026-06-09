---
name: runbook-tse
example: examples/tse/* (tse.eval, tse.metric, tse.metric.ruletype, tse.modularization, tse.momot, tse.rdg, tse.resources)
when_to_use: To work on the TSE example family. Read this FIRST - TSE is not in the Maven build and needs setup before it can produce results.
working_dir: the specific tse bundle root (e.g. examples/tse/at.ac.tuwien.big.momot.examples.tse.momot/)
status: NOT in Maven reactor; PDE/Eclipse only; entrypoints commented out.
---

# Runbook: TSE Family (legacy)

The TSE bundles implement transformation-modularization case studies (RDG, pairwise, etc.).
They are the least ready to run on this branch - handle with the caveats below.

## Why TSE does not produce results out of the box

1. Not in the Maven reactor: `examples/tse/` has no `pom.xml`, and no TSE bundle is listed in
   [`examples/pom.xml`](../../../examples/pom.xml) or the root pom. `mvn clean verify` does not
   build it.
2. Entrypoint is a no-op: `tse.momot/MOMoTSearch.main` has every `executeCaseStudy(...)` call
   commented out, and OCL initialization is commented too. Running it does nothing.
3. Legacy level: TSE was migrated to Java 1.8 and Apache Commons Lang 3 (see `MIGRATION.md`
   item 5), but otherwise predates the modern engine wiring.

## Bundles

`tse.eval`, `tse.metric`, `tse.metric.ruletype`, `tse.modularization`, `tse.momot`,
`tse.rdg`, `tse.resources`.

## Entrypoints (once enabled)

- `tse.momot/MOMoTSearch` (and `MOMoTSearchAll`, `console/Modularize`,
  `orchestration/ModularizationOrchestration`)
- `tse.rdg/RDGExample`, `RDGSearch`
- `tse.eval/PairwiseExample`

## To make TSE runnable

1. Build it: either import the bundles into Eclipse with the `2026-03` target platform and
   build via PDE, or add poms and wire it into the reactor (see
   [../skills/08-author-new-example.md](../skills/08-author-new-example.md)).
2. Select a case study: in `MOMoTSearch.main`, uncomment one `executeCaseStudy(...)` call (and
   the OCL initialization if that case study needs OCL).
3. Set the working directory to the bundle root so the case study's relative model/reference/
   output paths resolve.

## Expected outputs (once enabled)

`executeCaseStudy(model, referenceFile, outputDir, ...)` writes per-algorithm results under
the given `outputDir` (`/momot/`, `/nsgaiii/`, `/nsgaii/`, `/random/`), i.e. approximation
sets and result models per algorithm.

## Known issues

- Running without uncommenting a case study yields no output (expected).
- Commons Lang must be 3.x (`org.apache.commons.lang3.*`).
- OCL/ATL runtime must resolve; OCL delegate may need initialization.

## Done criteria

PASS when at least one case study is enabled, the bundle builds, and the chosen `outputDir`
contains per-algorithm approximation sets and result models. If the goal is only the
Maven-built examples, mark TSE as "deferred / out of scope" in the run report rather than
forcing it.
