---
name: results-and-verification
when_to_use: After a run, to confirm results were actually generated and judge whether they are valid.
inputs: An example's output directory after a run.
outputs: A pass/fail verification verdict with evidence (file list, non-emptiness, sanity of values).
---

# Skill 06: Results and Verification

"Generating results" means the search wrote its artifacts and they are non-empty and
plausible. A process exit code of 0 is NOT sufficient - verify the files.

## Result artifact types

| Artifact | Extension | Meaning |
| --- | --- | --- |
| Pareto / approximation set | `.pf` | objective vectors of the non-dominated solutions |
| Solutions / orchestrations | `.txt` | the transformation sequences found |
| Result models | `.xmi` | models produced by applying a solution's transformations |
| Analysis | `analysis.txt` (or named `.txt`) | indicator values and statistical comparison |
| Boxplots | files under a `boxplot/` dir | indicator distributions across runs |

## Where to look (per example)

Use [../reference/example-catalog.md](../reference/example-catalog.md) for the exact expected
paths. Examples:
- stack -> `example/output/*.pf`, `example/output/analysis.txt`, `example/output/solutions/`,
  `example/output/models/`.
- refactoring -> `model/output/referenceSet/approximation_set.pf`, `model/output/solutions/`.
- ecore (`ModularizationSearch`) -> `output/<langName>/nsgaii/approximation_nsgaii.pf` (+ `.txt`, models).

## Verification checklist

1. The expected output directory exists and was modified at/after the run time.
2. The `.pf` file exists and has at least one row (each row = one solution's objective values).
3. At least one result `.xmi` exists where the script declares a `models` block.
4. Objective values are finite and respect direction (e.g. a `minimize` objective is not
   absurdly large unless a constraint penalty fired - see skill 04).
5. If the example ships committed reference results, compare shape/scale against them. The
   stack example ships `model/output_test/` (and `model/output/`) as references.

Quick checks:

```bash
# from the example module root, after a run
find . -newermt "-10 minutes" -name "*.pf" -o -newermt "-10 minutes" -name "*.xmi" | head
wc -l example/output/*.pf 2>/dev/null     # rows > 0 means solutions were recorded
```

## Interpreting "no results"

If no files appeared:
- Re-read [02-run-an-example.md](02-run-an-example.md): wrong working directory is the most
  common cause (relative output paths resolved somewhere unexpected).
- Check the console for `SeedRuntimePrintListener` output. No progress lines means the search
  never started (model/Henshin not loaded, EPackage not registered).
- Go to [07-diagnose-failures.md](07-diagnose-failures.md).

## Reporting

Record the verdict in the [run report template](../templates/run-report.md): which files,
how many rows, and whether values are plausible.
