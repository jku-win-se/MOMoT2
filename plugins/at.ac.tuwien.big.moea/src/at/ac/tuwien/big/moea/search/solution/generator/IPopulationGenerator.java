package at.ac.tuwien.big.moea.search.solution.generator;

import org.moeaframework.core.initialization.Initialization;
import org.moeaframework.core.Solution;

public interface IPopulationGenerator<T extends Solution> extends Initialization {
   int getPopulationSize();

   @Override
   T[] initialize(int count);

   T[] initialize();

   IPopulationGenerator<T> setPopulationSize(int populationSize);
}
