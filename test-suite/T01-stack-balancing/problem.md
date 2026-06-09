# T01 — Stack Load Balancing

## Problem Statement

You are given a ring of **five stacks**, each with an integer `load` value and
bidirectional `left`/`right` neighbour references that form a circular topology.
The total load across all stacks is fixed at 25.

The goal is to redistribute load so that every stack carries the same load (5),
while applying as few transformation steps as possible.

**Initial model**

| Stack  | Load |
|--------|------|
| Stack_1 |  1  |
| Stack_2 |  7  |
| Stack_3 |  3  |
| Stack_4 |  9  |
| Stack_5 |  5  |

`LoadRange` (max − min load) starts at **8**.

## Search Objectives

| Objective      | Direction | Description                                              |
|----------------|-----------|----------------------------------------------------------|
| `LoadRange`    | minimise  | Difference between the highest and lowest stack load     |
| `SolutionLength` | minimise | Number of Henshin rule applications in the solution    |

## Henshin Transformation Rules

| Rule        | Parameters                          | Effect                                            |
|-------------|-------------------------------------|---------------------------------------------------|
| `shiftLeft` | `fromId`, `toId`, `amount`          | Moves `amount` load from `from` to its left neighbour |
| `shiftRight`| `fromId`, `toId`, `amount`          | Moves `amount` load from `from` to its right neighbour |
| `createStack` | `stackId`, `stackLoad`            | Creates a new stack (ignored by the search)        |
| `connectStacks` | `left`, `right`                 | Links two stacks (ignored by the search)           |

The search ignores `createStack` and `connectStacks` and treats `amount` as a
random integer in [1, 5].

## Expected MOMoT Configuration

- `solutionLength = 8`
- Algorithms: NSGA-II, NSGA-III, Random
- Population: 100, evaluations: 2000, runs: 5

## Source Files

| File | Description |
|------|-------------|
| `model/stack.ecore` | Metamodel (StackModel, Stack) |
| `model/stack.henshin` | Transformation module with 4 rules |
| `model/input/model/model_five_stacks.xmi` | Five-stack ring instance |
| `src/.../StackSearchExample.momot` | MOMoT search script |

## Notes

This is the canonical MOMoT example. The artifacts are copied verbatim from
`headless-example/job-minimal/`. The Pareto front is analytically bounded:
achieving `LoadRange=0` requires at minimum 4 adjacent load transfers because
the ring topology forces load to traverse intermediate stacks.
