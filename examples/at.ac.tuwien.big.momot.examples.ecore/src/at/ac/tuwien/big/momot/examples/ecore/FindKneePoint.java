package at.ac.tuwien.big.momot.examples.ecore;

import at.ac.tuwien.big.moea.SearchResultManager;
import at.ac.tuwien.big.moea.experiment.analyzer.kneepoint.PopulationAnalyzer;
import at.ac.tuwien.big.moea.experiment.analyzer.kneepoint.PopulationAnalyzer.FitnessComparison;

import java.io.File;
import java.io.IOException;

import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.core.population.Population;
import at.ac.tuwien.big.moea.util.PopulationUtil;
import org.moeaframework.core.Solution;

public class FindKneePoint {
   private static final int NEIGHBORHOOD_SIZE = 150;
   private static final FitnessComparison FITNESS_COMPARISON = FitnessComparison.PROPER_UTILITY;

   public static void kneepoint(final NondominatedPopulation population) {
      final PopulationAnalyzer populationAnalyzer = new PopulationAnalyzer(population);
      final NondominatedPopulation kneePointSolutions = populationAnalyzer.getKneePointSolutions(population,
            NEIGHBORHOOD_SIZE, FITNESS_COMPARISON);

      System.out.println("KneePoints: " + kneePointSolutions.size());
      System.out.println("===============");

      System.out.println("KneePoint Objectives");
      System.out.println("--------------------");
      System.out.println(SearchResultManager.printObjectives(kneePointSolutions));
      ;

      System.out.println("KneePoint Attributes");
      System.out.println("--------------------");
      System.out.println(SearchResultManager.printAttributes(kneePointSolutions));

      System.out.println("Best Solution");
      System.out.println("=============");
      final Solution bestSolution = populationAnalyzer.getBestSolution(population, FITNESS_COMPARISON);
      final NondominatedPopulation bestPopulation = new NondominatedPopulation();
      bestPopulation.add(bestSolution);
      System.out.println(SearchResultManager.printObjectives(bestPopulation));
      System.out.println(SearchResultManager.printAttributes(bestPopulation));
   }

   public static void kneepoint(final String file) throws IOException {
      final Population population = PopulationUtil.readObjectives(new File(file));
      final NondominatedPopulation nonDominatedPopulation = new NondominatedPopulation(population);

      System.out.println("Solution: " + population.size());
      System.out.println("===============");
      System.out.println(SearchResultManager.printObjectives(population));

      kneepoint(nonDominatedPopulation);
   }

   public static void main(final String[] args) throws IOException {
      kneepoint("output/PrimitiveTypes/nsgaii/approximation_nsgaii.pf");
   }
}
