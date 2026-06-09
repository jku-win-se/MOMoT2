# Henshin Iterative Test Loop

This prompt drives the iterative development of Henshin rules using a two-tier strategy.

## Tier 1: Fast CLI Loop (No Docker)

1.  **Modify Rule**: Make changes to the \`.henshin\` file based on current requirements or feedback.
2.  **Validate Semantic**:
    ```bash
    node tools/henshin-validator/validate.mjs --validate-semantic <file.henshin> --metamodel <file.ecore>
    ```
3.  **Triage**:
    - If errors: consult \`doc/henshin/09-debugging-runbook.md\`, fix the rule, and go back to step 1.
    - If success: proceed to step 4.
4.  **Apply Test**:
    ```bash
    node tools/henshin-validator/validate.mjs --apply <file.henshin> --metamodel <file.ecore> --model <file.xmi> --rule <ruleName>
    ```
5.  **Verify Result**: Inspect the generated \`out_result.xmi\`. If the transformation is incorrect, go back to step 1.

## Tier 2: MOMoT Integration (Docker Required)

6.  **Full Test**: Once Tier 1 is stable, run the full integration:
    ```bash
    execute_momot_job(config_path="path/to/your.momot")
    ```
7.  **Final Triage**: If the MOMoT job fails or finds no solutions, analyze the \`diagnostics.rootCauseHint\` and return to Tier 1 if the rules need adjustment.

## Goal

Terminate only when Tier 2 succeeds with at least one non-trivial solution found.
