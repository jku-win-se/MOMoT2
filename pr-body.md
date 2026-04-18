## Summary

Fixes the generator-side bug tracked in #3: the MOMoT `.momot` script emitted
by `generateArtifactsFromEcore` now includes correctly escaped DSL keyword
segments in the `TransformationLengthDimension` import, so the generated
scenario compiles and executes end-to-end.

Closes #3

## The bug, in one sentence

`search` and `fitness` are reserved keywords in the MOMoT Xtext DSL. When they
appear as segments of an FQN (e.g. in an `import` statement), they must be
escaped with a caret prefix. Without the escape, the DSL parser silently
fails to resolve the type and the Java code generator falls back to
`new Object()`, which the Java compiler rejects with a type mismatch against
`IFitnessDimension<TransformationSolution>`.

## The fix

One line in `mcp/lib.js` (`buildMomotScript`):

Before:

    import at.ac.tuwien.big.momot.search.fitness.dimension.TransformationLengthDimension

After:

    import at.ac.tuwien.big.momot.^search.^fitness.dimension.TransformationLengthDimension

Matches the pattern used by the hand-written fixture at
`stack-example-minimal/src/at/ac/tuwien/big/momot/examples/stack/StackSearchExample.momot`.

A short comment is added above the line in `lib.js` explaining the caret
requirement, so future refactors do not silently strip it.

## How it was identified

A dedicated diagnostic script (`local-scripts/Diagnose-Gate-C1.ps1`, not
tracked) replays the full pipeline manually and saves the response zip
intact, including `runner/compile/src-gen/*.java`. Inspection of the
generated Java at `line 59` showed:

    protected IFitnessDimension<TransformationSolution> _createObjectiveHelper_1() {
       Object _instance = new Object();
       return _instance;
    }

i.e. the compiler had fallen back to `new Object()` because it could not
resolve `TransformationLengthDimension`. Diffing the generated `.momot`
script against the fixture highlighted the missing caret escapes on the
import FQN.

## Validation

Three independent confirmations:

1. **Diagnostic script** — `Diagnose-Gate-C1.ps1` re-run after the fix
   returned `exitCode: 0`, response zip size went from 4707 to 13985 bytes,
   and the response now includes `out/models/model_0.0_8.0.xmi` and
   `out/objectives.txt` (both absent before the fix).

2. **Test suite** — `npm run test:stdio` now reports 6/6 pass (was 5/6
   with test #6 failing by design, tracking #3).

3. **Generated Java** — `_createObjectiveHelper_1()` now emits
   `new TransformationLengthDimension()` instead of `new Object()`.

## Related

- Test #6 in `mcp/test/stdio.test.js` (landed in #2) is the regression test
  that surfaced this. It stays in place as coverage going forward.
- The `knownGoodFixture: true` branch of `run_end_to_end` was never affected
  because it bypasses the generator entirely and uses the hand-written
  script from `stack-example-minimal` (which was already correctly escaped).