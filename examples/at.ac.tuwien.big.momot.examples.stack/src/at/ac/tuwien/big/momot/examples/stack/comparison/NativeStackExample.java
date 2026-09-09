package at.ac.tuwien.big.momot.examples.stack.comparison;

import at.ac.tuwien.big.moea.experiment.analyzer.SearchAnalyzer;
import at.ac.tuwien.big.moea.experiment.executor.SearchExecutor;
import at.ac.tuwien.big.moea.experiment.executor.listener.SeedRuntimePrintListener;
import at.ac.tuwien.big.moea.experiment.instrumenter.SearchInstrumenter;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.moeaframework.algorithm.extension.Frequency;
import org.moeaframework.analysis.runtime.Instrumenter;
import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.BinaryVariable;

public class NativeStackExample {

   public static SearchExecutor createExecutor(final Integer[] initialLoads, final int populationSize,
         final int maxEvaluations, final String algorithm) {
      final Instrumenter instrumenter = new SearchInstrumenter()
            .withProblemClass(NativeStackProblem.class, Arrays.asList(initialLoads))
            // .attachAll()
            .withFrequency(Frequency.ofEvaluations(populationSize));
      try {
         instrumenter.withReferenceSet(new File("output/evolutionary_reference_set.csv"));
      } catch (final Exception e) {
         // ignore
      }

      final SearchExecutor executor = new SearchExecutor().withSameProblemAs(instrumenter).withAlgorithm("NSGAII")
            .withProperty("populationSize", populationSize).withProperty("maxEvaluations", maxEvaluations)
            .distributeOnAllCores()
            // .withProgressListener(new RuntimePrintListener())
            .withProgressListener(new SeedRuntimePrintListener()).withInstrumenter(instrumenter);

      return executor;
   }

   public static SearchAnalyzer doAnalysis(final String name, final Integer[] initialLoads, final SearchExecutor executor,
         final int runs) {
      final SearchAnalyzer analyzer = new SearchAnalyzer().withSameProblemAs(executor).withReferenceSet((File) null);

      final List<NondominatedPopulation> result = executor.runSeeds(runs);
      analyzer.addAll("NSGAII", result);
      final NondominatedPopulation referenceSet = analyzer.getReferenceSet();
      if(referenceSet != null) {
         printPopulation(referenceSet, initialLoads);
      } else if(!result.isEmpty()) {
         printPopulation(result.get(0), initialLoads);
      }
      analyzer.printAnalysis();
      return analyzer;
   }

   public static void main(final String[] args) {

      final Integer[] initialLoads = new Integer[] { 1, 7, 3, 9, 5 };

      final SearchExecutor executor = createExecutor(initialLoads, 100, 1000, "NSGAII");
      doAnalysis("NSGAII", initialLoads, executor, 5);
   }

   public static void printPopulation(final NondominatedPopulation population, final Integer[] initialLoads) {
      int solutionNr = 1;
      for(final Solution s : population) {
         System.out.println("Solution " + solutionNr++ + " of " + population.size() + ":");
         for(int i = 0; i < s.getNumberOfVariables(); i++) {
            final BinaryVariable var = (BinaryVariable) s.getVariable(i);
            System.out.println("  Var " + i + ": " + var.getBitSet());
         }
      }
   }
}
