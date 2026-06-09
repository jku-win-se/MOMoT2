# Henshin Knowledge Base

Welcome to the Henshin transformation knowledge base. This directory contains a comprehensive guide to writing, validating, and testing Henshin rules, specifically tailored for integration with MOMoT.

## Reading Order

1.  **[00-overview.md](00-overview.md)**: Introduction to Henshin and its role in MOMoT.
2.  **[01-rule-anatomy.md](01-rule-anatomy.md)**: Deep dive into the structure of a Henshin rule (LHS, RHS, Actions).
3.  **[02-parameters.md](02-parameters.md)**: How to use parameters to make rules dynamic and searchable.
4.  **[03-composite-units.md](03-composite-units.md)**: Combining rules into sequences, loops, and conditional units.
5.  **[04-conditions-nac-pac.md](04-conditions-nac-pac.md)**: Adding application conditions (Negative/Positive Application Conditions).
6.  **[05-metamodel-binding.md](05-metamodel-binding.md)**: Wiring rules to Ecore metamodels.
7.  **[06-momot-integration.md](06-momot-integration.md)**: How MOMoT uses Henshin modules for multi-objective search.
8.  **[07-common-patterns.md](07-common-patterns.md)**: Reusable XMI templates for common transformation tasks.
9.  **[08-testing-strategy.md](08-testing-strategy.md)**: The two-tier testing loop (CLI Validator + MOMoT Integration).
10. **[09-debugging-runbook.md](09-debugging-runbook.md)**: Triage steps for common Henshin and MOMoT errors.

## Examples

Check the **[examples/](examples/)** directory for annotated `.henshin` files.

## Tools

Use the **[Henshin Validator CLI](../../tools/henshin-validator/)** for fast local feedback without needing a full MOMoT/Docker environment.
