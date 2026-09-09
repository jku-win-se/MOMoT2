package at.ac.tuwien.big.moea.search.fitness;

import at.ac.tuwien.big.moea.util.MathUtil;

import org.moeaframework.core.Solution;

public abstract class AbstractFitnessFunction<T extends Solution> implements IFitnessFunction<T> {

   private final Class<T> clazz;

   public AbstractFitnessFunction(final Class<T> clazz) {
      this.clazz = clazz;
   }

   @Override
   public double doEvaluate(final Solution solution) {
      if(!clazz.isInstance(solution)) {
         for(final int i : evaluatesObjectives()) {
            solution.setObjectiveValue(i, WORST_FITNESS);
         }
         for(final int i : evaluatesConstraints()) {
            solution.setConstraintValue(i, WORST_FITNESS);
         }
      } else {
         return evaluate(clazz.cast(solution));
      }

      return getAggregateFitness(solution);
   }

   @Override
   public abstract double evaluate(T solution);

   @Override
   public abstract int[] evaluatesConstraints();

   @Override
   public int evaluatesNrConstraints() {
      return evaluatesConstraints().length;
   }

   @Override
   public int evaluatesNrObjectives() {
      return evaluatesObjectives().length;
   }

   @Override
   public abstract int[] evaluatesObjectives();

   protected double getAggregateFitness(final Solution solution) {
      return MathUtil.getSum(solution.getConstraintValues(), solution.getObjectiveValues());
   }
}
