# 08 - Testing Strategy

We use a two-tier approach to ensure Henshin rules are correct before running expensive MOMoT search jobs.

## Tier 1: Local Validation (Fast)
Use the **Henshin Validator CLI** (`tools/henshin-validator/`).
-   **Structure**: Check XMI validity and basic Henshin rules.
-   **Semantic**: Validate against the Ecore metamodel.
-   **Application**: Apply a rule to a small test model (`.xmi`) and inspect the output.

**Command:**
```bash
node tools/henshin-validator/validate.mjs --validate-semantic rules.henshin --metamodel model.ecore
```

## Tier 2: MOMoT Smoke Test (Integration)
Run a short MOMoT job with a small population and few iterations to ensure the rules interact correctly with the fitness functions and search engine.

**Command:**
```bash
# Via MCP
execute_momot_job(config_path="my.momot")
```

## Workflow
1. Write rule.
2. Run Tier 1. If fail, fix rule.
3. If Tier 1 pass, run Tier 2.
4. If Tier 2 fail (e.g., fitness error or no solutions), triage using the runbook.
