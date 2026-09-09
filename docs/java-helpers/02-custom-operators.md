# Java-Helpers Wiki — Chapter 02: Custom Search Operators

This chapter describes how to write custom search operators in Java when declarative Henshin transformation rules are insufficient. Implementing custom operators gives you direct programmatic control over mutation and crossover behaviors inside MOEA frameworks (such as NSGA-II).

## When to Use Custom Operators

While Henshin graph-transformation rules are the default and recommended way to define mutations in MOMoT, custom Java operators are useful when:
1. **Dynamic Search Boundaries**: Move operators require non-trivial math that cannot be easily modeled in Henshin.
2. **Feasibility Preservation**: Ensuring that a mutation never violates hard constraints, bypassing the default generate-and-test repair loops.
3. **Complex Structural Re-organization**: Modifying the model in ways that would require dozens of nested or conditional Henshin units.

## The Base Class: `Mutation`

Custom mutation operators must implement the standard `org.moeaframework.core.operator.Mutation` interface from the MOEA Framework.

## Canonical Mutation Template

```java
package mypackage;

import at.ac.tuwien.big.momot.problem.solution.TransformationSolution;
import org.moeaframework.core.Solution;
import org.moeaframework.core.operator.Mutation;

public class FeasibilityPreservingMutation implements Mutation {

    private final double probability;

    public FeasibilityPreservingMutation(double probability) {
        this.probability = probability;
    }

    @Override
    public Solution mutate(Solution solution) {
        // Only apply mutation with the declared probability
        if (Math.random() < probability) {
            // Clone the current solution state safely to apply delta
            TransformationSolution ts = (TransformationSolution) solution.copy();
            
            // Apply feasibility-preserving transformations on the cloned state
            applyCustomMutation(ts);
            
            return ts;
        }
        return solution;
    }

    private void applyCustomMutation(TransformationSolution solution) {
        // Domain mutation logic goes here
    }
}
```

## Integrating into MOMoT Algorithms

Custom mutation operators are wired directly into algorithm configurations (e.g., NSGA_II) in your `.momot` script:

```momot
import org.moeaframework.core.selection.TournamentSelection
import org.moeaframework.core.operator.OnePointCrossover

algorithms = {
   NSGA_II : moea.createNSGAII(
       new TournamentSelection(2), 
       new OnePointCrossover(1.0),
       new FeasibilityPreservingMutation(0.15)
   )
}
```

## Verification Checklist

- [ ] Operator implements `org.moeaframework.core.operator.Mutation`.
- [ ] `mutate` method clones the solution safely (`solution.copy()`) before applying mutations.
- [ ] Method returns a valid `Solution` instance (the mutated copy or the original).
- [ ] Probability parameter is correctly declared and checked.
- [ ] Operator does not cause dangling references or containment tree breakage.
