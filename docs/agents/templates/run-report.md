---
name: run-report-template
purpose: Standard structure for reporting the outcome of building/running/verifying an example.
when_to_use: The coordinator emits one filled copy per example it processes.
---

# Run Report Template

Copy this block and fill it in for each example. Keep it terse and evidence-based.

```
## Run report: <example name>

- Date / agent: <when, who>
- Module: <module id>  (in reactor: yes/no)
- Goal: <build only | run + produce results | diagnose + fix>

### Phase outcomes
- Environment (skill 00): PASS | FAIL  - <note>
- Build (skill 01):       PASS | FAIL  - <command, result>
- Run (skill 02):         PASS | FAIL  - <entrypoint class, working dir>
- Results (skill 06):     PASS | FAIL  - <files + row counts>

### Result artifacts
- <path>  (<rows/size>, plausible? yes/no)
- ...

### Root cause (if any failure)
- Phase: build | runtime | no-output
- Cause: <one line>
- Fix applied: <files changed / config / none>
- Re-run outcome: PASS | FAIL

### Verdict
- PASS (results generated and plausible)
- FAIL (blocked) - next action: <...>
- DEFERRED / out of scope - reason: <...>   (e.g. TSE not wired into the build)
```

## Aggregate summary (across all examples)

When processing several examples, end with a one-line-per-example table:

```
| Example | Build | Run | Results | Verdict |
| --- | --- | --- | --- | --- |
| stack | PASS | PASS | PASS | PASS |
| cra | ... | ... | ... | ... |
| ecore | ... | ... | ... | ... |
| emfrefactor | ... | ... | ... | ... |
| modularization.jsme | ... | ... | ... | ... |
| refactoring | ... | ... | ... | ... |
| tse | n/a | n/a | n/a | DEFERRED |
```
