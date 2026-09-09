package at.ac.tuwien.big.moea.search.solution.generator;

import at.ac.tuwien.big.moea.search.solution.generator.solution.IRandomSolutionGenerator;

import org.moeaframework.core.Solution;

public class RandomPopulationGenerator<S extends Solution> implements IPopulationGenerator<S> {

   private final IRandomSolutionGenerator<S> solutionGenerator;
   private int populationSize;

   public RandomPopulationGenerator(final int populationSize, final IRandomSolutionGenerator<S> solutionGenerator) {
      this.populationSize = populationSize;
      this.solutionGenerator = solutionGenerator;
   }

   @Override
   public int getPopulationSize() {
      return populationSize;
   }

   public IRandomSolutionGenerator<S> getSolutionGenerator() {
      return solutionGenerator;
   }

   @Override
   public S[] initialize() {
      return initialize(getPopulationSize());
   }

   @Override
   public S[] initialize(final int count) {
      @SuppressWarnings("unchecked")
      final S[] population = (S[]) new Solution[count];

      for(int i = 0; i < count; i++) {
         population[i] = solutionGenerator.createRandomSolution();
      }

      return population;
   }

   @Override
   public IPopulationGenerator<S> setPopulationSize(final int populationSize) {
      this.populationSize = populationSize;
      return this;
   }
}
