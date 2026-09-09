package at.ac.tuwien.big.moea.search.solution.generator;

import at.ac.tuwien.big.moea.search.solution.generator.solution.IRandomSolutionGenerator;

import java.util.ArrayList;
import java.util.List;

import org.moeaframework.core.Solution;

public class InjectedRandomPopulationGenerator<S extends Solution> extends RandomPopulationGenerator<S>
      implements IInjectedPopulationGenerator<S> {
   private List<S> injectedSolutions = new ArrayList<>();

   public InjectedRandomPopulationGenerator(final int populationSize,
         final IRandomSolutionGenerator<S> solutionGenerator) {
      super(populationSize, solutionGenerator);
   }

   public InjectedRandomPopulationGenerator(final int populationSize,
         final IRandomSolutionGenerator<S> solutionGenerator, final List<S> injectedSolutions) {
      super(populationSize, solutionGenerator);
      this.injectedSolutions = injectedSolutions;
   }

   @Override
   public void addInjectedSolution(final S injectedSolution) {
      injectedSolutions.add(injectedSolution);
   }

   @Override
   public void addInjectedSolutions(final Iterable<S> injectedSolutions) {
      for(final S solution : injectedSolutions) {
         this.injectedSolutions.add(solution);
      }
   }

   @Override
   public List<S> getInjectedSolutions() {
      return injectedSolutions;
   }

   @Override
   public S[] initialize() {
      return initialize(getPopulationSize());
   }

   @Override
   public S[] initialize(final int count) {
      @SuppressWarnings("unchecked")
      final S[] population = (S[]) new Solution[count];

      int i = 0;
      while(i < getInjectedSolutions().size() && i < count) {
         population[i] = getInjectedSolutions().get(i);
         i++;
      }

      while(i < count) {
         population[i] = getSolutionGenerator().createRandomSolution();
         i++;
      }

      return population;
   }

   @Override
   public void setInjectedSolutions(final List<S> injectedSolutions) {
      this.injectedSolutions = injectedSolutions;
   }
}
