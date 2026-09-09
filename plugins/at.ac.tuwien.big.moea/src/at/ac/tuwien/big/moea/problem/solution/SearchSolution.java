package at.ac.tuwien.big.moea.problem.solution;

import at.ac.tuwien.big.moea.util.CastUtil;

import java.io.Serializable;

import org.moeaframework.core.Solution;

public class SearchSolution extends Solution {
   private static final long serialVersionUID = 1489801128861913870L;

   public static final String ATTRIBUTE_AGGREGATED_FITNESS = "AggregatedFitness";

   public SearchSolution(final double[] objectives) {
      super(0, objectives != null ? objectives.length : 0);
      if(objectives != null) {
         setObjectiveValues(objectives);
      }
   }

   public SearchSolution(final int numberOfVariables, final int numberOfObjectives) {
      super(numberOfVariables, numberOfObjectives);
   }

   public SearchSolution(final int numberOfVariables, final int numberOfObjectives, final int numberOfConstraints) {
      super(numberOfVariables, numberOfObjectives, numberOfConstraints);
   }

   public SearchSolution(final Solution solution) {
      super(solution);
   }

   public <A extends Serializable> A getAttribute(final String key, final Class<A> resultClass) {
      return CastUtil.asClass(getAttribute(key), resultClass);
   }

   public Double getStoredAggregatedFitness() {
      return getAttribute(ATTRIBUTE_AGGREGATED_FITNESS, Double.class);
   }

   public void removeStoredAggregatedFitness() {
      removeAttribute(ATTRIBUTE_AGGREGATED_FITNESS);
   }

   public void storeAggregatedFitness(final double fitness) {
      setAttribute(ATTRIBUTE_AGGREGATED_FITNESS, fitness);
   }
}
