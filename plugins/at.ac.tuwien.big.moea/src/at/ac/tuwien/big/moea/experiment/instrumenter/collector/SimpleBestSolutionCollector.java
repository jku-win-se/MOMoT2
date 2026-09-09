package at.ac.tuwien.big.moea.experiment.instrumenter.collector;

import at.ac.tuwien.big.moea.util.AccumulatorUtil;
import at.ac.tuwien.big.moea.util.MathUtil;

import org.moeaframework.algorithm.Algorithm;
import org.moeaframework.analysis.runtime.AttachPoint;
import org.moeaframework.analysis.runtime.Collector;
import org.moeaframework.analysis.series.ResultEntry;
import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.core.Solution;

public class SimpleBestSolutionCollector implements Collector {

   public static double calculateAggregatedFitness(final Solution solution) {
      return MathUtil.getSum(solution.getObjectiveValues(), solution.getConstraintValues());
   }

   private final Algorithm algorithm;

   public SimpleBestSolutionCollector() {
      this(null);
   }

   public SimpleBestSolutionCollector(final Algorithm algorithm) {
      this.algorithm = algorithm;
   }

   @Override
   public Collector attach(final Object object) {
      return new SimpleBestSolutionCollector((Algorithm) object);
   }

   @Override
   public void collect(final ResultEntry entry) {
      if(algorithm == null) {
         return;
      }
      final NondominatedPopulation result = algorithm.getResult();
      Solution bestSolution = result.size() > 0 ? result.get(0) : null;
      double bestObjectiveSum = Double.MAX_VALUE;

      double curObjectiveSum;
      for(final Solution solution : result) {
         curObjectiveSum = calculateAggregatedFitness(solution);
         if(curObjectiveSum < bestObjectiveSum) {
            bestObjectiveSum = curObjectiveSum;
            bestSolution = solution;
         }
      }

      if(bestSolution != null) {
         entry.getProperties().setString(AccumulatorUtil.Keys.SIMPLE_BEST_SOLUTION, bestSolution.toString());
      }
   }

   @Override
   public AttachPoint getAttachPoint() {
      return AttachPoint.isSubclass(Algorithm.class).and(AttachPoint.not(AttachPoint.isNestedIn(Algorithm.class)));
   }
}
