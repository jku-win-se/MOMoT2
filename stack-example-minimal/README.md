## Minimal stack example (MOMoT)

This folder contains a **minimal, self-contained** version of the MOMoT *stack* example:

- `src/.../StackSearchExample.momot`: the MOMoT search specification (entry point)
- `model/stack.henshin`: the transformation rules
- `model/input/model/model_five_stacks.xmi`: the input model
- `src/.../stack/*`: the (generated) EMF model used by the rules and search
- `src/.../StackModule.java`: constants referenced by the `.momot` file

### How to run

Import this folder as an Eclipse Plug-in project (PDE) in an environment that already contains MOMoT and its dependencies.
Then run the MOMoT search defined in `src/at/ac/tuwien/big/momot/examples/stack/StackSearchExample.momot`.

### Headless REST smoke test

For reproducible Docker REST validation of this minimal fixture, use the repository scripts:

- PowerShell: `scripts/run-minimal-rest-test.ps1`
- Bash: `scripts/run-minimal-rest-test.sh`

Both scripts build the payload zip, call `/run`, verify `runner/exit_code.txt == 0`, and print a summary.

