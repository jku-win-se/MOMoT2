---
description: "Implement support for Henshin VAR parameters in Momot search and execution"
name: "Add Henshin VAR parameter support"
argument-hint: "Use this prompt to add or debug support for Henshin VAR parameters in composed units"
agent: "agent"
---

You are working in the Momot repository.

Goal: implement robust support for Henshin `VAR` parameters used as internal bindings inside composed units, especially sequential and independent units that pass an `EClass`-typed binding from one subunit to the next.

The current issue is that Momot can parse Henshin modules that contain `kind="VAR"`, but search and execution can fail when a unit depends on an internal variable binding created by a previous subunit. The expected behavior is that a `VAR` parameter should be preserved as an internal connection between subunits, not treated as a solution parameter to randomize, strip, or ignore.

Work in the existing Momot architecture instead of inventing a parallel execution path. Focus on the root cause.

What to inspect first:
- `plugins/at.ac.tuwien.big.momot.core/src/at/ac/tuwien/big/momot/ModuleManager.java`
- `plugins/at.ac.tuwien.big.momot.core/src/at/ac/tuwien/big/momot/search/solution/executor/SearchHelper.java`
- `plugins/at.ac.tuwien.big.momot.core/src/at/ac/tuwien/big/momot/problem/solution/variable/UnitApplicationVariable.java`
- `plugins/at.ac.tuwien.big.momot.core/src/at/ac/tuwien/big/momot/problem/solution/variable/RuleApplicationVariable.java`
- `plugins/at.ac.tuwien.big.momot.core/src/at/ac/tuwien/big/momot/problem/solution/TransformationSolution.java`
- Any code that builds search solutions, partial matches, parameter assignments, or parameter mappings for composed units

Requirements:
- Preserve `VAR` bindings across subunits in composed units when Momot executes or searches them.
- Keep existing `IN`, `OUT`, and `INOUT` behavior unchanged.
- Do not expose `VAR` parameters as solution variables unless that is explicitly required by a unit’s semantics.
- Ensure search can discover matches for composed units that depend on a `VAR` binding introduced earlier in the chain.
- Keep the fix compatible with the current Henshin runtime and Momot’s search abstractions.

Investigation checklist:
1. Determine whether Momot already reads and stores all parameter kinds from Henshin modules or whether `VAR` is being collapsed into a generic parameter path.
2. Trace how assignments are created for `Rule`, `SequentialUnit`, and `IndependentUnit` applications.
3. Check whether parameter values are copied, cleared, or randomized in a way that destroys internal bindings.
4. Verify how `findUnitApplication(...)` and `findUnitApplications(...)` handle composed units with intermediate bindings.
5. Check whether `clearNonSolutionParameters(...)` and `assignParameterValues(...)` should treat `VAR` differently from solution parameters.
6. Confirm whether `UnitApplicationVariable` and `RuleApplicationVariable` preserve internal parameter bindings across execution, redo, undo, and copying.

Implementation guidance:
- Prefer a minimal fix that preserves Momot’s existing abstractions.
- If the issue is in parameter propagation, make the propagation explicit and local to the unit execution path.
- If the issue is that Momot incorrectly categorizes `VAR` parameters, introduce a clear distinction between external solution parameters and internal binding parameters.
- If the Henshin API already supports this behavior, adapt Momot to use it correctly rather than re-implementing Henshin semantics.
- Add comments only where the logic is non-obvious.

Validation expectations:
- Add or update tests that reproduce the failure with a composed Henshin unit using `VAR` parameters.
- Prefer a small, focused regression test over broad test changes.
- Run the most targeted build or test command that proves the fix.
- If the repository already has a minimal example or headless test harness, use it as the regression case.

Expected output from the agent:
- A short explanation of the root cause.
- The exact files changed.
- The behavioral change for Henshin `VAR` parameters.
- Any remaining limitations, if the fix is partial.
- Verification results from tests or build commands.