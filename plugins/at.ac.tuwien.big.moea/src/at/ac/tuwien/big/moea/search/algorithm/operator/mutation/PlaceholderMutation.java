package at.ac.tuwien.big.moea.search.algorithm.operator.mutation;

import at.ac.tuwien.big.moea.problem.solution.variable.PlaceholderVariable;
import at.ac.tuwien.big.moea.util.MathUtil;

import org.moeaframework.core.Solution;

public class PlaceholderMutation extends AbstractMutationVariation {

   public PlaceholderMutation() {}

   public PlaceholderMutation(final double probability) {
      super(probability);
   }

   @Override
   protected Solution[] doEvolve(final Solution[] parents) {
      final Solution copy = parents[0].copy();
      final Solution mutant = mutate(copy);
      return new Solution[] { mutant };
   }

   @Override
   public Solution mutate(final Solution mutant) {
      final int randomVariable = MathUtil.randomInteger(mutant.getNumberOfVariables());
      mutant.setVariable(randomVariable, new PlaceholderVariable());
      return mutant;
   }
}
