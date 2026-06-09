# 09 - Debugging Runbook

Triage guide for Henshin failures.

## 1. "EPackage not found" or "nsURI not registered"
-   **Cause**: Henshin can't find your metamodel.
-   **Fix**: Check the `imports` tag in the `.henshin` file. Ensure the `nsURI` matches the Ecore file.

## 2. NullPointerException during matching
-   **Cause**: Often a broken reference in the XMI (e.g., a node type points to a non-existent class).
-   **Fix**: Run Tier 1 validation. Re-save the `.henshin` file in an editor to refresh references.

## 3. Rule is never applied
-   **Cause**: The LHS/NAC is too restrictive, or parameters are not being provided correctly.
-   **Fix**:
    -   Check if the test model actually contains the pattern.
    -   Try removing NACs one by one.
    -   Check if `IN` parameters are being bound.

## 4. VAR parameter is null
-   **Cause**: In a `SequentialUnit`, a prior step failed to set the `OUT` parameter that feeds the `VAR`.
-   **Fix**: Ensure the providing rule actually executes and has a mapping/attribute that sets the parameter.

## 5. "Inconsistent state" after rule application
-   **Cause**: Rule violates metamodel constraints (e.g., duplicate unique IDs, broken multiplicity).
-   **Fix**: Inspect the model after applying the rule using the CLI validator's `--apply` mode.
