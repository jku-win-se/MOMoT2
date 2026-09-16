# MOMoT Full-Branch Agent Scaffolding

This folder is an AI-agent operating manual for the `full` branch. Its purpose: let an agent
build, run, diagnose, and verify every MOMoT example so they reliably generate results.

## Entry point

Start at [coordinator.md](coordinator.md). It is the orchestrator: it decides what to do next
and which skill or runbook to load. Everything else is loaded on demand.

## Layout

```
docs/agents/
  coordinator.md            # START HERE - orchestrator + decision tree + dispatch table
  README.md                 # this index
  skills/                   # reusable how-to playbooks (load on demand)
    00-environment-setup.md
    01-build-full-branch.md
    02-run-an-example.md
    03-momot-script-anatomy.md
    04-objectives-and-fitness.md
    05-search-and-experiment.md
    06-results-and-verification.md
    07-diagnose-failures.md
    08-author-new-example.md
  runbooks/                 # one per example family
    stack.md  cra.md  ecore.md  emfrefactor.md
    modularization-jsme.md  refactoring.md  tse.md
  reference/
    example-catalog.md      # authoritative dispatch data (what/where/how per example)
    architecture.md         # build + run topology, full vs standalone
    glossary.md             # MDE / MOMoT / optimization terms
  templates/
    run-report.md           # standard per-example result report
```

## How an agent uses this

1. Read `coordinator.md`.
2. Follow its decision tree, loading a `skills/*` playbook per step.
3. For a specific example, open its `runbooks/*` file (dispatched from
   `reference/example-catalog.md`).
4. Verify results, then emit a `templates/run-report.md`.

## Conventions

- Each skill and runbook carries front-matter (`name`, `when_to_use`, `inputs`, `outputs`) so
  the coordinator can route by intent.
- Files are tool-agnostic Markdown - usable from any agent harness. The repo-root
  [`AGENTS.md`](../../AGENTS.md) points here.
- Golden rule for every run: the JVM working directory must be the example project root
  (examples use relative paths).
