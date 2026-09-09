package at.ac.tuwien.big.moea.search.fitness.comparator;

import org.moeaframework.core.Solution;

public class ObjectiveFitnessComparator<S extends Solution> extends AbstractFitnessComparator<Double, S> {

   private int objectiveIndex = 0;

   public ObjectiveFitnessComparator(final int objectiveIndex) {
      this.objectiveIndex = objectiveIndex;
   }

   public int getObjectiveIndex() {
      return objectiveIndex;
   }

   @Override
   public Double getValue(final S solution) {
      if(getObjectiveIndex() >= solution.getNumberOfObjectives()) {
         throw new IllegalArgumentException("Solution does not have " + getObjectiveIndex() + 1 + " objectives.");
      }
      return solution.getObjectiveValue(getObjectiveIndex());
   }
}
