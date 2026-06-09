---
name: objectives-and-fitness
when_to_use: To understand or modify what a search optimizes, including objectives, constraints, and the preprocess pattern.
inputs: A .momot script's fitness block or a *Search Java main.
outputs: A correct fitness model (objectives minimized/maximized, constraints penalized).
---

# Skill 04: Objectives and Fitness

The `fitness` block defines the multi-objective optimization problem. MOMoT evaluates each
candidate solution (a transformation orchestration) by executing it and scoring the resulting
model.

## Objective forms

Three equivalent ways to express an objective appear across the examples:

1. Inline Xtend expression:
   ```
   StandardDeviation : minimize { MathUtil.getStandardDeviation((root as StackModel).stacks.map[load]) }
   ```
2. OCL string:
   ```
   ContentSize : minimize "properties->size() * 1.1 + entities->size()"
   NrModules   : maximize "modules->size()"
   ```
3. A fitness-dimension object:
   ```
   SolutionLength : minimize new TransformationLengthDimension
   ```

`minimize` / `maximize` set the optimization direction. `root` is the root of the model
produced by executing the candidate solution; `graph` (where used) is the underlying Henshin
`EGraph`.

## The `preprocess` pattern (expensive metrics)

When several objectives share an expensive computation (e.g. coupling + cohesion + MQ over a
modularization), compute it once in `preprocess` and stash it on the solution, then read it in
each objective:

```
preprocess = {
   val root = MomotUtil.getRoot(solution.execute, typeof(ModularizationModel))
   solution.setAttribute(attribute, new ModularizationCalculator(root))
}
objectives = {
   Coupling : minimize { solution.getAttribute(attribute, typeof(ModularizationCalculator)).metrics.coupling }
   ...
}
```

This is used by `ecore` and `modularization.jsme`.

## Constraints

Constraints mark invalid solutions by adding large penalties, e.g. in JSME:

```
constraints = {
   UnassignedClasses : minimize { penalty * (root as ModularizationModel).classes.filter[c | c.module == null].size }
   EmptyModules      : minimize { penalty * (root as ModularizationModel).modules.filter[m | m.classes.empty].size }
}
```

## Repairers and equality helpers

- `solutionRepairer = new TransformationPlaceholderRepairer` replaces non-applicable rules in
  an orchestration with empty placeholders so the solution stays valid.
- `equalityHelper = { ... }` customizes how EMF objects are matched (e.g. by name in EMF
  Refactor, by module index in JSME).

## Java-side equivalent

Hand-written mains build the same model programmatically, e.g. `StackOrchestration` adds an
`AbstractEGraphFitnessDimension` for standard deviation plus a `TransformationLengthDimension`
and sets a `TransformationPlaceholderRepairer`. Keep the Java main and the `.momot` script in
sync when editing objectives.

## Objective inventory (per example)

See [../reference/example-catalog.md](../reference/example-catalog.md) for the exact
objectives, directions, and algorithms of each example.
