# 03 - Composite Units

Composite units allow you to group multiple rules or units into complex transformation flows.

## Unit Types

### SequentialUnit
Executes sub-units in a fixed order. Often uses `VAR` parameters to pass data between steps.
-   **Fail-fast**: If one sub-unit fails, the whole sequence fails (unless configured otherwise).

### LoopUnit
Repeatedly executes a sub-unit as long as it is applicable.
-   **Warning**: Ensure the rule eventually fails to match, or you will hit an infinite loop.

### IndependentUnit
Randomly selects one sub-unit from all that are currently applicable. Useful for introducing non-determinism in MOMoT.

### PriorityUnit
Tries to execute sub-units in order. The first one that is applicable is executed, and the unit finishes.

### ConditionalUnit
An if-then-else structure.
-   `if`: A rule or unit used as a condition.
-   `then`: Executed if `if` succeeds.
-   `else`: Executed if `if` fails.

## MOMoT Treatment
MOMoT treats a composite unit as a single atomic step in the search process.
