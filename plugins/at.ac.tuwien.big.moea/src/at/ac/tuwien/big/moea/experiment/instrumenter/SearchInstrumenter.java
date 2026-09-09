package at.ac.tuwien.big.moea.experiment.instrumenter;

import at.ac.tuwien.big.moea.problem.ISearchProblem;

import java.util.ArrayList;
import java.util.List;

import org.moeaframework.analysis.collector.Accumulator;
import org.moeaframework.analysis.runtime.Instrumenter;
import org.moeaframework.problem.Problem;
import org.moeaframework.core.Solution;

public class SearchInstrumenter extends Instrumenter {
   protected List<Accumulator> accumulators = new ArrayList<>();
   protected Accumulator lastAccumulator = new Accumulator();
   protected Problem problem;
   protected Class<?> problemClass;
   protected Object[] problemArguments;
   protected String problemName;

   public SearchInstrumenter() {}

   public SearchInstrumenter(final ISearchProblem<? extends Solution> problem) {
      this();
      withProblem(problem);
   }

   public void addAccumulator(final Accumulator accumulator) {
      if(!accumulators.contains(accumulator)) {
         accumulators.add(accumulator);
      }
      this.lastAccumulator = accumulator;
   }

   public SearchInstrumenter attachApproximationSetCollector() {
      return this;
   }

   public List<Accumulator> getAccumulators() {
      return accumulators;
   }

   public Accumulator getLastAccumulator() {
      return lastAccumulator;
   }

   public Problem getProblem() {
      if(problem != null) {
         return problem;
      }
      if(problemClass != null) {
         try {
            if(problemArguments != null && problemArguments.length > 0) {
               for(final java.lang.reflect.Constructor<?> ctor : problemClass.getConstructors()) {
                  final Class<?>[] paramTypes = ctor.getParameterTypes();
                  if(paramTypes.length == problemArguments.length) {
                     boolean match = true;
                     for(int i = 0; i < paramTypes.length; i++) {
                        if(problemArguments[i] != null && !paramTypes[i].isAssignableFrom(problemArguments[i].getClass())) {
                           match = false;
                           break;
                        }
                     }
                     if(match) {
                        return (Problem) ctor.newInstance(problemArguments);
                     }
                  }
               }
            } else {
               return (Problem) problemClass.getDeclaredConstructor().newInstance();
            }
         } catch(final Exception e) {
            e.printStackTrace();
         }
      }
      return null;
   }

   public Class<?> getProblemClass() {
      return problemClass;
   }

   public Object[] getProblemArguments() {
      return problemArguments;
   }

   public String getProblemName() {
      return problemName;
   }

   public SearchInstrumenter withProblem(final ISearchProblem<? extends Solution> problem) {
      return withProblemClass(problem.getClass(), problem.getFitnessFunction(), problem.getSolutionGenerator());
   }

   public SearchInstrumenter withProblem(final Problem problemInstance) {
      this.problem = problemInstance;
      return this;
   }

   public SearchInstrumenter withProblemClass(final Class<?> problemClass, final Object... problemArguments) {
      this.problemClass = problemClass;
      this.problemArguments = problemArguments;
      return this;
   }

   public SearchInstrumenter withProblemName(final String problemName) {
      this.problemName = problemName;
      return this;
   }
}
