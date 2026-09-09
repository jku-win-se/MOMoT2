package at.ac.tuwien.big.moea.search.algorithm.local;

import java.util.ArrayList;

import org.moeaframework.problem.Problem;
import org.moeaframework.core.Solution;

public class RandomDescent<S extends Solution> extends AbstractLocalSearchAlgorithm<S> {

   public RandomDescent(final Problem problem, final S initialSolution,
         final INeighborhoodFunction<S> neighborhoodFunction, final IFitnessComparator<?, S> fitnessComparator) {
      super(problem, initialSolution, neighborhoodFunction, fitnessComparator);
      if(neighborhoodFunction.getMaxNeighbors() == INeighborhoodFunction.UNLIMITED) {
         System.err.println(
               "Warning: Neighborhood-Function may produce infinite neighbors, Random-Descent may get stuck in infinite loop.");
      }
   }

   @Override
   protected void iterate() {
      final ArrayList<S> neighbors = new ArrayList<>();
      for(final S neighbor : generateCurrentNeighbors()) {
         evaluate(neighbor);
         neighbors.add(neighbor);
         if(update(neighbor)) {
            break;
         }
      }
      System.out.println(-getCurrentSolution().getObjectiveValue(0));
      if(neighbors.isEmpty()) {
         terminate();
         return;
      }
   }
}
