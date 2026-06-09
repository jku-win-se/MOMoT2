# 01 - Rule Anatomy

A Henshin rule is the fundamental unit of transformation. It defines how a specific pattern in a model should be modified.

## Structure

-   **LHS (Left-Hand Side)**: The "Match" pattern. Defines the conditions that must be met in the model for the rule to apply.
-   **RHS (Right-Hand Side)**: The "Result" pattern. Defines what the model should look like after the rule is applied.
-   **Mappings**: Connect nodes in the LHS to nodes in the RHS. If a node is mapped, it is *preserved*.

## Actions

Henshin uses an `action` attribute to simplify rule definition:

-   **preserve** (default): Node/Edge exists in both LHS and RHS.
-   **create**: Node/Edge exists only in RHS.
-   **delete**: Node/Edge exists only in LHS.

## XMI Skeleton

```xml
<units xsi:type="henshin:Rule" name="exampleRule">
  <lhs name="Lhs">
    <nodes name="nodeToPreserve" type="...#//SomeClass">
      <!-- Mapped to RHS -->
    </nodes>
    <nodes name="nodeToDelete" type="...#//SomeClass">
      <!-- Not mapped to RHS -->
    </nodes>
  </lhs>
  <rhs name="Rhs">
    <nodes name="nodeToPreserve" type="...#//SomeClass">
      <!-- Mapped from LHS -->
    </nodes>
    <nodes name="newNode" type="...#//SomeClass">
      <!-- Not mapped from LHS -->
    </nodes>
  </rhs>
  <mappings origin="//@units.0/@lhs/@nodes.0" image="//@units.0/@rhs/@nodes.0"/>
</units>
```
