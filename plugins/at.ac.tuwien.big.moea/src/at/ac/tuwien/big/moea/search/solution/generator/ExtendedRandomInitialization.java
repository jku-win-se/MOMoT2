package at.ac.tuwien.big.moea.search.solution.generator;

import org.moeaframework.problem.Problem;
import org.moeaframework.core.initialization.RandomInitialization;
import org.moeaframework.core.Solution;

public class ExtendedRandomInitialization extends RandomInitialization {

   private final int populationSize;

   public ExtendedRandomInitialization(final Problem problem, final int populationSize) {
      super(problem);
      this.populationSize = populationSize;
   }

   public int getPopulationSize() {
      return populationSize;
   }

   public Solution[] initialize() {
      return initialize(populationSize);
   }
}
