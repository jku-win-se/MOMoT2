# T03 — Tree Depth Reduction

## Problem Statement

You are given a rooted tree with **7 nodes**. The main branch forms a deep chain
(`root → A → B → C → D → E`) with one shallow sibling (`root → F`).

The search must **reparent nodes** to reduce the maximum depth while making as
few structural changes as possible.

**Initial tree**

```
root  (depth 0)
├── A (depth 1)
│   └── B (depth 2)
│       └── C (depth 3)
│           └── D (depth 4)
│               └── E (depth 5)   ← MaxDepth = 5
└── F (depth 1)
```

## Search Objectives

| Objective        | Direction | Description                                               |
|------------------|-----------|-----------------------------------------------------------|
| `MaxDepth`       | minimise  | Maximum `depth` value across all nodes in the final tree  |
| `SolutionLength` | minimise  | Number of `reparentNode` rule applications                |

## Henshin Transformation Rule

| Rule           | Parameters                              | Effect                                                           |
|----------------|-----------------------------------------|------------------------------------------------------------------|
| `reparentNode` | `nodeName`, `newParentName`, `newParentDepth` | Detaches the named node from its current parent and attaches it to the new parent; updates the moved node's `depth` to `newParentDepth + 1` |

**Parameter sampling**: `nodeName` ∈ {A,B,C,D,E,F} (non-root nodes); `newParentName` ∈ {root,A,B,C,D,E,F}. The `newParentDepth` parameter is matched from the model by Henshin and is listed as `ignoreParameters` in the `.momot` script.

## Pareto-Optimal Solutions

| Move sequence (each detaches node → attaches to root) | MaxDepth | SolutionLength |
|-------------------------------------------------------|----------|----------------|
| (none)                                                | 5        | 0              |
| reparent D → root                                     | 3        | 1              |
| reparent D → root, reparent C → root                  | 2        | 2              |
| reparent D, C, B → root                               | 1        | 3              |

After 3 moves the tree becomes a star (all nodes directly under root, depth ≤ 1).

## Source Files

| File | Description |
|------|-------------|
| `model/tree.ecore` | Metamodel (Tree, Node with name/depth/parent/children) |
| `model/tree.henshin` | Single rule: reparentNode |
| `model/input/model_skewed_tree.xmi` | 7-node skewed chain instance |
| `src/.../TreeSearchExample.momot` | MOMoT search script |

## Notes

- The `reparentNode` rule updates only the moved node's `depth`. If the moved
  node has children, their depths become stale. A production implementation
  should either (a) propagate depth updates to the subtree via a loop unit or
  (b) recompute `MaxDepth` by full traversal in the fitness function.
- The placeholder `{ 0.0 }` for `MaxDepth` must be replaced with a traversal
  that reads `node.depth` values or recomputes them recursively.
- Moving the root node is implicitly prevented: the `reparentNode` rule matches
  an `oldParent → node` edge; the root has no parent, so it can never match
  the `node` role in the LHS.
