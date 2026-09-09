package at.ac.tuwien.big.moea.search.algorithm.operator.mutation;

import at.ac.tuwien.big.moea.search.algorithm.operator.AbstractProbabilityVariation;

import org.moeaframework.core.Solution;

public abstract class AbstractMutationVariation extends AbstractProbabilityVariation implements IMutationVariation {

   public AbstractMutationVariation() {}

   public AbstractMutationVariation(final double probability) {
      super(probability);
   }

   @Override
   public int getArity() {
      return 1;
   }

   @Override
   public Solution mutate(final Solution solution) {
      final Solution[] result = evolve(new Solution[] { solution });
      return result != null && result.length > 0 ? result[0] : solution;
   }
}
