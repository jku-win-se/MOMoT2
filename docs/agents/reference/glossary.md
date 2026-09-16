---
name: glossary
purpose: Definitions of MDE, MOMoT, and optimization terms used throughout the scaffolding.
when_to_use: When an unfamiliar term appears in a skill, runbook, or script.
---

# Glossary

- MOMoT: Marrying Search-based Optimization and Model Transformation Technology. The framework
  that optimizes sequences of model transformations against multiple objectives.
- MDE (Model-Driven Engineering): software engineering centered on models as primary
  artifacts.
- SBSE (Search-Based Software Engineering): applying metaheuristic search/optimization to
  software engineering problems.
- EMF (Eclipse Modeling Framework): the modeling runtime. Models conform to an Ecore
  metamodel and are serialized as `.xmi`.
- Ecore: EMF's metamodeling language. A `.ecore` file defines the types (`EClass`,
  `EReference`, ...); a `.genmodel` drives code generation.
- EPackage: the runtime handle for a metamodel's package. Must be registered
  (`XxxPackage.eINSTANCE`) before loading models standalone.
- Henshin: a graph-based model-transformation language. Rules and units live in `.henshin`
  files. MOMoT orchestrates Henshin units as the "moves" of the search.
- Unit / Rule: a Henshin transformation step. A solution is an orchestration (sequence) of
  unit applications, possibly with parameters.
- EGraph: Henshin's in-memory graph representation of a model during transformation.
- QVT / ATL: model-to-model transformation languages. ATL (`.atl`/`.asm`) is used by the
  ecore example to convert metamodels into a generic modularization model.
- OCL (Object Constraint Language): query/constraint language over models; used for objective
  expressions and metrics. On this branch use the Ecore-specific `org.eclipse.ocl.ecore.OCL`.
- Solution / TransformationSolution: a candidate = an orchestration of transformation units.
- SolutionLength: number of unit applications in a solution (a common minimize objective).
- Fitness / Objective: a scored dimension the search optimizes (minimize or maximize).
- Constraint: a penalized condition marking invalid solutions (e.g. unassigned classes).
- Repairer (`TransformationPlaceholderRepairer`): replaces non-applicable rules in an
  orchestration with empty placeholders to keep solutions valid.
- MOEA Framework: the underlying multi-objective optimization library.
- NSGA-II / NSGA-III: non-dominated sorting genetic algorithms (multi- and many-objective).
- eMOEA (epsilon-MOEA): a steady-state multi-objective EA using epsilon-dominance.
- Local search: hill climbing / random descent single-point search.
- Pareto front / Approximation set: the set of non-dominated solutions; saved as `.pf` (each
  row = one solution's objective vector).
- Reference set: a known/combined best set used to evaluate approximation-set quality.
- Knee point: the Pareto-front solution with the best trade-off (largest marginal gain);
  selected via `neighborhoodSize`.
- Hypervolume / Generational Distance / Additive Epsilon / Contribution: quality indicators
  comparing approximation sets to a reference.
- Coupling / Cohesion / MQ (Modularization Quality): structural metrics; lower coupling and
  higher cohesion/MQ indicate better modular designs.
- Tycho: Maven plugins that build Eclipse/OSGi (PDE) artifacts. Version `4.0.10` here.
- Target platform: the set of OSGi bundles a PDE build resolves against (`2026-03.target`).
- Reactor: the set of Maven modules built together from the root pom.
