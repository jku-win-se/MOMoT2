package at.ac.tuwien.big.moea.util;

import at.ac.tuwien.big.moea.experiment.executor.SearchExecutor;
import at.ac.tuwien.big.moea.experiment.instrumenter.SearchInstrumenter;
import at.ac.tuwien.big.moea.experiment.instrumenter.collector.LocalBestFitnessCollector;
import at.ac.tuwien.big.moea.experiment.instrumenter.collector.SimpleBestSolutionCollector;

import java.io.Serializable;
import java.util.List;

import org.moeaframework.algorithm.Algorithm;
import org.moeaframework.analysis.collector.Accumulator;
import org.moeaframework.analysis.runtime.Instrumenter;
import org.moeaframework.core.Solution;

public final class AccumulatorUtil {

   public final class Keys {

      public static final String NUMBER_OF_EVALUATIONS = "NFE";
      public static final String POPULATION_SIZE = "Population Size";
      public static final String ARCHIVE_SIZE = "Archive Size";
      public static final String APPROXIMATION_SET = "Approximation Set";
      public static final String ELAPSED_TIME = "Elapsed Time";
      public static final String NUMBER_OF_RESTARTS = "Number of Restarts";
      public static final String NUMBER_OF_IMPROVEMENTS = "Number of Improvements";
      public static final String NUMBER_OF_DOMINATING_IMPROVEMENTS = "Number of Dominating Improvements";
      public static final String INDICATOR_CONTRIBUTION = "Contribution";
      public static final String INDICATOR_ADDITIVE_EPSILON = "AdditiveEpsilonIndicator";
      public static final String INDICATOR_SPACING = "Spacing";
      public static final String INDICATOR_INVERTED_GENERATIONAL_DISTANCE = "InvertedGenerationalDistance";
      public static final String INDICATOR_GENERATIONAL_DISTANCE = "GenerationalDistance";
      public static final String INDICATOR_HYPERVOLUME = "Hypervolume";
      public static final String SIMPLE_BEST_SOLUTION = "SimpleBestSolution";
      public static final String LOCAL_BEST_FITNESS = "LocalBestFitness";
      public static final String ALGORITHM = "Algorithm";

      private Keys() {}
   }

   public static Serializable getAccumulatorData(final Accumulator accumulator, final String key, final int index) {
      if(accumulator == null) {
         return null;
      }
      try {
         return accumulator.get(key, index);
      } catch(IllegalArgumentException | IndexOutOfBoundsException e) {
         return null;
      }
   }

   public static <T extends Object> T getAccumulatorData(final Accumulator accumulator, final String key,
         final int index, final Class<T> clazz) {
      return CastUtil.asClass(getAccumulatorData(accumulator, key, index), clazz);
   }

   public static Double getAdditiveEpsilon(final Accumulator accumulator, final int index) {
      return getAccumulatorData(accumulator, Keys.INDICATOR_ADDITIVE_EPSILON, index, Double.class);
   }

   @SuppressWarnings("unchecked")
   public static List<Solution> getApproximationSet(final Accumulator accumulator, final int index) {
      return getAccumulatorData(accumulator, Keys.APPROXIMATION_SET, index, List.class);
   }

   public static Integer getArchiveSize(final Accumulator accumulator, final int index) {
      return getAccumulatorData(accumulator, Keys.ARCHIVE_SIZE, index, Integer.class);
   }

   public static Solution getBestSolution(final Accumulator accumulator, final int index) {
      return getAccumulatorData(accumulator, Keys.SIMPLE_BEST_SOLUTION, index, Solution.class);
   }

   public static Double getContribution(final Accumulator accumulator, final int index) {
      return getAccumulatorData(accumulator, Keys.INDICATOR_CONTRIBUTION, index, Double.class);
   }

   public static Double getElapsedTime(final Accumulator accumulator, final int index) {
      return getAccumulatorData(accumulator, Keys.ELAPSED_TIME, index, Double.class);
   }

   public static Double getGenerationalDistance(final Accumulator accumulator, final int index) {
      return getAccumulatorData(accumulator, Keys.INDICATOR_GENERATIONAL_DISTANCE, index, Double.class);
   }

   public static Double getHypervolume(final Accumulator accumulator, final int index) {
      return getAccumulatorData(accumulator, Keys.INDICATOR_HYPERVOLUME, index, Double.class);
   }

   public static Double getInvertedGenerationalDistance(final Accumulator accumulator, final int index) {
      return getAccumulatorData(accumulator, Keys.INDICATOR_INVERTED_GENERATIONAL_DISTANCE, index, Double.class);
   }

   public static Serializable getLatestAccumulatorData(final Accumulator accumulator, final String key) {
      try {
         return getAccumulatorData(accumulator, key, accumulator.size(key) - 1);
      } catch(final IllegalArgumentException e) {
         return null;
      }
   }

   public static <T extends Object> T getLatestAccumulatorData(final Accumulator accumulator, final String key,
         final Class<T> clazz) {
      return CastUtil.asClass(getLatestAccumulatorData(accumulator, key), clazz);
   }

   public static Serializable getLatestAccumulatorData(final SearchExecutor executor, final String key) {
      if(executor == null) {
         return null;
      }
      return getLatestAccumulatorData(executor.getInstrumenter(), key);
   }

   public static <T extends Object> T getLatestAccumulatorData(final SearchExecutor executor, final String key,
         final Class<T> clazz) {
      return CastUtil.asClass(getLatestAccumulatorData(executor, key), clazz);
   }

   public static Serializable getLatestAccumulatorData(final Instrumenter instrumenter, final String key) {
      if(instrumenter == null) {
         return null;
      }
      if(instrumenter instanceof SearchInstrumenter) {
         return getLatestAccumulatorData(((SearchInstrumenter) instrumenter).getLastAccumulator(), key);
      }
      return null;
   }

   public static <T extends Object> T getLatestAccumulatorData(final Instrumenter instrumenter, final String key,
         final Class<T> clazz) {
      return CastUtil.asClass(getLatestAccumulatorData(instrumenter, key), clazz);
   }

   public static Double getLatestAdditiveEpsilon(final Accumulator accumulator) {
      return getLatestAccumulatorData(accumulator, Keys.INDICATOR_ADDITIVE_EPSILON, Double.class);
   }

   public static Algorithm getLatestAlgorithm(final Accumulator accumulator) {
      return getLatestAccumulatorData(accumulator, Keys.ALGORITHM, Algorithm.class);
   }

   public static Algorithm getLatestAlgorithm(final SearchExecutor executor) {
      return getLatestAccumulatorData(executor, Keys.ALGORITHM, Algorithm.class);
   }

   @SuppressWarnings("unchecked")
   public static List<Solution> getLatestApproximationSet(final Accumulator accumulator) {
      return getLatestAccumulatorData(accumulator, Keys.APPROXIMATION_SET, List.class);
   }

   public static Integer getLatestArchiveSize(final Accumulator accumulator) {
      return getLatestAccumulatorData(accumulator, Keys.ARCHIVE_SIZE, Integer.class);
   }

   public static Solution getLatestBestSolution(final Accumulator accumulator) {
      return getLatestAccumulatorData(accumulator, Keys.SIMPLE_BEST_SOLUTION, Solution.class);
   }

   public static Solution getLatestBestSolution(final SearchExecutor executor) {
      return getLatestAccumulatorData(executor, Keys.SIMPLE_BEST_SOLUTION, Solution.class);
   }

   public static Double getLatestContribution(final Accumulator accumulator) {
      return getLatestAccumulatorData(accumulator, Keys.INDICATOR_CONTRIBUTION, Double.class);
   }

   public static Double getLatestElapsedTime(final Accumulator accumulator) {
      return getLatestAccumulatorData(accumulator, Keys.ELAPSED_TIME, Double.class);
   }

   public static Double getLatestGenerationalDistance(final Accumulator accumulator) {
      return getLatestAccumulatorData(accumulator, Keys.INDICATOR_GENERATIONAL_DISTANCE, Double.class);
   }

   public static Double getLatestHypervolume(final Accumulator accumulator) {
      return getLatestAccumulatorData(accumulator, Keys.INDICATOR_HYPERVOLUME, Double.class);
   }

   public static Double getLatestInvertedGenerationalDistance(final Accumulator accumulator) {
      return getLatestAccumulatorData(accumulator, Keys.INDICATOR_INVERTED_GENERATIONAL_DISTANCE, Double.class);
   }

   public static Integer getLatestNumberOfDominatingImprovements(final Accumulator accumulator) {
      return getLatestAccumulatorData(accumulator, Keys.NUMBER_OF_DOMINATING_IMPROVEMENTS, Integer.class);
   }

   public static Integer getLatestNumberOfEvaluations(final Accumulator accumulator) {
      return getLatestAccumulatorData(accumulator, Keys.NUMBER_OF_EVALUATIONS, Integer.class);
   }

   public static Integer getLatestNumberOfImprovements(final Accumulator accumulator) {
      return getLatestAccumulatorData(accumulator, Keys.NUMBER_OF_IMPROVEMENTS, Integer.class);
   }

   public static Integer getLatestNumberOfRestarts(final Accumulator accumulator) {
      return getLatestAccumulatorData(accumulator, Keys.NUMBER_OF_RESTARTS, Integer.class);
   }

   public static Integer getLatestPopulationSize(final Accumulator accumulator) {
      return getLatestAccumulatorData(accumulator, Keys.POPULATION_SIZE, Integer.class);
   }

   public static Double getLatestSpacing(final Accumulator accumulator) {
      return getLatestAccumulatorData(accumulator, Keys.INDICATOR_SPACING, Double.class);
   }

   public static Integer getNumberOfDominatingImprovements(final Accumulator accumulator, final int index) {
      return getAccumulatorData(accumulator, Keys.NUMBER_OF_DOMINATING_IMPROVEMENTS, index, Integer.class);
   }

   public static Integer getNumberOfEvaluations(final Accumulator accumulator, final int index) {
      return getAccumulatorData(accumulator, Keys.NUMBER_OF_EVALUATIONS, index, Integer.class);
   }

   public static Integer getNumberOfImprovements(final Accumulator accumulator, final int index) {
      return getAccumulatorData(accumulator, Keys.NUMBER_OF_IMPROVEMENTS, index, Integer.class);
   }

   public static Integer getNumberOfRestarts(final Accumulator accumulator, final int index) {
      return getAccumulatorData(accumulator, Keys.NUMBER_OF_RESTARTS, index, Integer.class);
   }

   public static Integer getPopulationSize(final Accumulator accumulator, final int index) {
      return getAccumulatorData(accumulator, Keys.POPULATION_SIZE, index, Integer.class);
   }

   public static Double getSpacing(final Accumulator accumulator, final int index) {
      return getAccumulatorData(accumulator, Keys.INDICATOR_SPACING, index, Double.class);
   }

   public static boolean hasAccumulatorData(final Accumulator accumulator, final String key) {
      if(accumulator == null) {
         return false;
      }
      try {
         return accumulator.keySet().contains(key);
      } catch(final ClassCastException | NullPointerException e) {
         return false;
      }
   }

   public static boolean hasAccumulatorData(final SearchExecutor executor, final String key) {
      if(executor == null) {
         return false;
      }
      return hasAccumulatorData(executor.getInstrumenter(), key);
   }

   public static boolean hasAccumulatorData(final Instrumenter instrumenter, final String key) {
      if(instrumenter == null) {
         return false;
      }
      if(instrumenter instanceof SearchInstrumenter) {
         return hasAccumulatorData(((SearchInstrumenter) instrumenter).getLastAccumulator(), key);
      }
      return false;
   }

   private AccumulatorUtil() {}
}
