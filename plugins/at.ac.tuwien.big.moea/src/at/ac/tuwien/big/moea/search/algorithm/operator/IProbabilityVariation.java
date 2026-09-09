package at.ac.tuwien.big.moea.search.algorithm.operator;

import org.moeaframework.core.operator.Variation;

public interface IProbabilityVariation extends Variation {
   double ALWAYS_EVOLVE = 1.0;
   double NEVER_EVOLVE = 0.0;

   double getProbability();
}
