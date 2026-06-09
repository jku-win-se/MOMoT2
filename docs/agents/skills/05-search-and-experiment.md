---
name: search-and-experiment
when_to_use: To choose/configure search algorithms or tune experiment cost (population, evaluations, runs).
inputs: A .momot script's algorithms/experiment blocks or a *Search Java main.
outputs: A correctly configured, appropriately sized search run.
---

# Skill 05: Search Algorithms and Experiment Configuration

MOMoT delegates optimization to the MOEA Framework via `moea.create...` factories. The
`algorithms` block registers one or more named algorithms; `experiment` sets the budget.

## Algorithms used across the examples

| Factory | Meaning | Seen in |
| --- | --- | --- |
| `moea.createRandomSearch()` | random baseline | stack, cra, jsme |
| `moea.createNSGAII(...)` | NSGA-II (multi-objective GA) | stack, refactoring, ecore (Java) |
| `moea.createNSGAIII(...)` | NSGA-III (many-objective) | all example families |
| `moea.createEpsilonMOEA(...)` | epsilon-MOEA | cra, jsme |
| Local search (`LocalSearchAlgorithmFactory`) | hill climbing / random descent | stack (`output_test/local/...`), jsme hill-climbing main |

Typical operator wiring for an evolutionary algorithm:

```
NSGA_II : moea.createNSGAII(
   new TournamentSelection(2),
   new OnePointCrossover(1.0),
   new TransformationPlaceholderMutation(0.15),
   new TransformationParameterMutation(0.1, orchestration.moduleManager))
```

- `TournamentSelection`, `OnePointCrossover` - standard MOEA operators.
- `TransformationPlaceholderMutation` - mutates which rule occupies an orchestration slot.
- `TransformationParameterMutation` / `TransformationVariableMutation` - mutate rule
  parameters / Henshin variable bindings.

## Experiment block

```
experiment = {
   populationSize    = 100
   maxEvaluations    = 2000
   nrRuns            = 5
   progressListeners = [ new SeedRuntimePrintListener ]
}
```

- `populationSize` - candidates per generation.
- `maxEvaluations` - total fitness evaluations per run (iterations = maxEvaluations /
  populationSize).
- `nrRuns` - independent seeds, for statistical comparison in `analysis`.
- `SeedRuntimePrintListener` - prints per-seed runtime progress (your main signal that the
  search is actually progressing).

## Sizing guidance

Shipped values are research-scale and can take a long time:

| Example | populationSize | maxEvaluations | nrRuns |
| --- | --- | --- | --- |
| stack | 100 | 2000 | 5 |
| cra | 100 | 10000 | 10 |
| refactoring | 50 | 1500 | 30 |
| emfrefactor | 50 | 1000 | 30 |
| ecore / jsme | 300 | 21000 | 30 |

For a smoke test, drop `nrRuns` to 1-2 and `maxEvaluations` to a few hundred; restore the
original values before committing or reporting research results. Stack at default size is
already quick enough to validate the whole pipeline.

## Analysis indicators

`analysis.indicators` may include `hypervolume`, `generationalDistance`,
`invertedGenerationalDistance`, `additiveEpsilonIndicator`, `maximumParetoFrontError`,
`contribution`. These quantify Pareto-front quality across runs and feed
`statisticalSignificance` reporting.
