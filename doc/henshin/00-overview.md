# 00 - Henshin Overview

Henshin is a graph transformation language and toolset for the Eclipse Modeling Framework (EMF). It allows you to define transformations on models (represented as graphs) using a visual or XMI-based syntax.

## Key Concepts

-   **EMF Integration**: Henshin operates directly on Ecore-based models.
-   **Graph Transformation**: Transformations are defined as rules that match a pattern in a source graph (LHS) and replace it with a pattern in a target graph (RHS).
-   **Declarative Rules**: You define *what* should change, and the Henshin engine handles the *how* (matching and applying).

## Role in MOMoT

In the MOMoT (Multi-Objective Model Transformation) framework:
1.  **Search Operators**: Henshin rules serve as the "mutation" or "crossover" operators for the search algorithm.
2.  **State Space**: The model being transformed represents a state in the search space.
3.  **Search Variables**: Parameters of Henshin rules are exposed as search variables that MOMoT can optimize.

MOMoT applies these rules iteratively to explore the search space and find models that optimize specified objectives.
