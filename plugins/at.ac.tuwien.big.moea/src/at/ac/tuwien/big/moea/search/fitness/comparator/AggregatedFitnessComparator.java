package at.ac.tuwien.big.moea.search.fitness.comparator;

import at.ac.tuwien.big.moea.problem.solution.SearchSolution;
import at.ac.tuwien.big.moea.util.MathUtil;

import org.moeaframework.core.Solution;

public class AggregatedFitnessComparator<S extends Solution> extends AttributeFitnessComparator<Double, S> {

   public AggregatedFitnessComparator() {
      super(SearchSolution.ATTRIBUTE_AGGREGATED_FITNESS, Double.class);
   }

   @Override
   public Double getValue(final S solution) {
      Double fitness = super.getValue(solution);
      if(fitness == null) {
         fitness = MathUtil.getSum(solution.getObjectiveValues(), solution.getConstraintValues());
      }
      return fitness;
   }
}
