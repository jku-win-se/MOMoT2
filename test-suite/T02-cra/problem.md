# T02 — Class-Responsibility Assignment (CRA)

## Problem Statement

You are given a **ClassModel** with two classes and five features (3 methods, 2 attributes)
that have data-dependency relationships. All features start in **C1** (one big class).

The search must redistribute features across the two classes to **maximise cohesion**
(features that depend on each other should be in the same class) while **minimising
coupling** (cross-class dependencies should be as few as possible).

**Dependency graph**

```
m1 ──dataDep──► a1
m2 ──dataDep──► a1
m2 ──dataDep──► a2
m3 ──dataDep──► a2
```

**Initial assignment**: C1 = {m1, m2, m3, a1, a2}, C2 = {} (empty)

## CRA-Index Formula

```
CRA-Index = Σ cohesion(Cᵢ) − Σ coupling(Cᵢ, Cⱼ)

cohesion(C)    = |{(m,a) : m ∈ C.methods, a ∈ C.attributes, m dataDependsOn a}|
                 ─────────────────────────────────────────────────────────────
                 |C.methods| × |C.attributes|        (0 if either set is empty)

coupling(Cᵢ,Cⱼ) = |{(m,a) : m ∈ Cᵢ.methods, a ∈ Cⱼ.attributes, m dataDependsOn a}|
                   ─────────────────────────────────────────────────────────────────
                   |Cᵢ.methods| × |Cⱼ.attributes|   (0 if either set is empty)
```

## Search Objectives

| Objective        | Direction | Description                                               |
|------------------|-----------|-----------------------------------------------------------|
| `NegCRAIndex`    | minimise  | Negative CRA-Index (minimising this maximises cohesion)   |
| `SolutionLength` | minimise  | Number of `assignFeature` rule applications               |

## Henshin Transformation Rules

| Rule               | Parameters                           | Effect                                             |
|--------------------|--------------------------------------|----------------------------------------------------|
| `assignFeature`    | `featureName`, `targetClassName`     | Moves feature to the target class                  |
| `createClass`      | `newClassName`                       | Adds a new empty class (ignored by search)         |
| `deleteEmptyClass` | `emptyClassName`                     | Removes a class with no features (ignored)         |

The search uses only `assignFeature`. Both `featureName` and `targetClassName` are
sampled uniformly from the sets `{m1,m2,m3,a1,a2}` and `{C1,C2}` respectively.

## Optimal Solutions

| Assignment                         | CRA-Index | NegCRAIndex | Moves from initial |
|------------------------------------|-----------|-------------|-------------------|
| C1={m1,m2,a1} / C2={m3,a2}        | 1.5       | −1.5        | 2                 |
| C1={m1,a1} / C2={m2,m3,a2}        | 1.5       | −1.5        | 3                 |
| C1={m1,m2,m3,a1,a2} (initial)     | 0.667     | −0.667      | 0                 |

CRA-Index = 1.5 is the global optimum for this 5-feature, 2-class instance and is
achievable in **2 moves** from the initial state.

## Source Files

| File | Description |
|------|-------------|
| `model/cra.ecore` | Metamodel (ClassModel, Class, Feature, Method, Attribute) |
| `model/cra.henshin` | Three rules: assignFeature, createClass, deleteEmptyClass |
| `model/input/model_cra_small.xmi` | Small 2-class, 5-feature instance |
| `src/.../CRASearchExample.momot` | MOMoT search script |

## Notes

The `NegCRAIndex` objective body `{ 0.0 }` in the `.momot` file is a placeholder.
In a real deployment the actual CRA-Index computation must be provided as a Java
`IFitnessDimension` implementation or as an OCL expression registered with the
MOMoT fitness registry.
