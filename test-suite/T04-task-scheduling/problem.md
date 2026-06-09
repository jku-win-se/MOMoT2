# T04 — Task–Machine Scheduling

## Problem Statement

You are given a **Schedule** with 3 machines and 4 tasks. All tasks start
assigned to **M1**, giving a makespan of 10. M2 and M3 are initially idle.

The search must redistribute tasks across machines to **minimise the makespan**
(the bottleneck machine's total workload) while making as **few task reassignments
as possible**.

**Task durations**

| Task | Duration |
|------|---------|
| T1   |  4      |
| T2   |  3      |
| T3   |  2      |
| T4   |  1      |
| **Total** | **10** |

**Initial assignment**: all tasks → M1, M2 and M3 idle.  
**Initial Makespan**: 10.

## Search Objectives

| Objective        | Direction | Description                                                   |
|------------------|-----------|---------------------------------------------------------------|
| `Makespan`       | minimise  | Max total task duration on any single machine                 |
| `SolutionLength` | minimise  | Number of `reassignTask` rule applications                    |

## Henshin Transformation Rule

| Rule           | Parameters                        | Effect                                                       |
|----------------|-----------------------------------|--------------------------------------------------------------|
| `reassignTask` | `taskId`, `targetMachineId`       | Changes `task.assignedTo` from the current machine to the target machine |

A NAC prevents no-op reassignment (task already on target machine).

**Parameter sampling**: `taskId` ∈ {T1,T2,T3,T4}; `targetMachineId` ∈ {M1,M2,M3}.

## Pareto-Optimal Solutions

| Move sequence                          | Machine loads             | Makespan | SolutionLength |
|----------------------------------------|---------------------------|----------|----------------|
| (none)                                 | M1=10, M2=0, M3=0         | 10       | 0              |
| T1 → M2                                | M1=6, M2=4, M3=0          | 6        | 1              |
| T1 → M2, T2 → M3                       | M1=3, M2=4, M3=3          | 4        | 2              |

**Minimum achievable makespan = 4** (T1 has duration 4 and must be on some machine,
so makespan ≥ 4 regardless of how other tasks are distributed).

## Source Files

| File | Description |
|------|-------------|
| `model/schedule.ecore` | Metamodel (Schedule, Machine, Task) |
| `model/schedule.henshin` | Single rule: reassignTask |
| `model/input/model_four_tasks.xmi` | 3-machine, 4-task instance |
| `src/.../ScheduleSearchExample.momot` | MOMoT search script |

## Notes

- The `Makespan` objective body `{ 0.0 }` is a placeholder. A real implementation
  iterates over machines, sums task durations per machine, and returns the maximum.
- The lower bound on Makespan is `max(task.duration)` = 4 (the optimal makespan
  for this instance is indeed 4, achieved in 2 moves).
- With 3 machines and 4 tasks the entire state space is small enough for
  exhaustive verification (3⁴ = 81 possible assignments).
