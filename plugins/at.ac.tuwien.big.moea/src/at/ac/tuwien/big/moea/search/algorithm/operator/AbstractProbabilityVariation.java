package at.ac.tuwien.big.moea.search.algorithm.operator;

import at.ac.tuwien.big.moea.util.MathUtil;

import org.moeaframework.core.PRNG;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.Variable;

public abstract class AbstractProbabilityVariation implements IProbabilityVariation {

   public static Variable getRandomVariable(final Solution solution) {
      if(solution == null || solution.getNumberOfVariables() == 0) {
         return null;
      }

      return solution.getVariable(MathUtil.randomInteger(solution.getNumberOfVariables()));
   }

   public static boolean shouldEvolve(final double probability) {
      if(probability == 0.0) {
         return false;
      }
      if(probability == 1.0) {
         return true;
      }
      return PRNG.nextDouble() <= probability;
   }

   private double probability;

   public AbstractProbabilityVariation() {
      this(ALWAYS_EVOLVE);
   }

   public AbstractProbabilityVariation(final double probability) {
      this.probability = probability;
   }

   protected void checkArity(final Solution[] parents) {
      if(parents == null || parents.length != getArity()) {
         throw new IllegalArgumentException(getArity() + " solutions must be supplied to this variation.");
      }
   }

   protected abstract Solution[] doEvolve(Solution[] parents);

   @Override
   public Solution[] evolve(final Solution[] parents) {
      checkArity(parents);
      if(!shouldEvolve()) {
         return parents;
      }

      return doEvolve(parents);
   }

   @Override
   public String getName() {
      return getClass().getSimpleName();
   }

   @Override
   public double getProbability() {
      return probability;
   }

   public AbstractProbabilityVariation setProbability(final double probability) {
      this.probability = probability;
      return this;
   }

   public boolean shouldEvolve() {
      return shouldEvolve(getProbability());
   }
}
