package icmt.tool.momot.demo;

import at.ac.tuwien.big.moea.SearchAnalysis;
import at.ac.tuwien.big.moea.SearchExperiment;
import at.ac.tuwien.big.moea.experiment.executor.listener.SeedRuntimePrintListener;
import at.ac.tuwien.big.moea.search.algorithm.EvolutionaryAlgorithmFactory;
import at.ac.tuwien.big.moea.util.MathUtil;
import at.ac.tuwien.big.moea.search.fitness.dimension.AbstractFitnessDimension;
import at.ac.tuwien.big.momot.util.MomotUtil;
import at.ac.tuwien.big.momot.ModuleManager;
import at.ac.tuwien.big.momot.TransformationResultManager;
import at.ac.tuwien.big.momot.TransformationSearchOrchestration;
import at.ac.tuwien.big.momot.problem.solution.TransformationSolution;
import at.ac.tuwien.big.momot.search.algorithm.operator.mutation.TransformationPlaceholderMutation;
import at.ac.tuwien.big.momot.search.fitness.dimension.TransformationLengthDimension;
import at.ac.tuwien.big.momot.search.solution.repair.TransformationPlaceholderRepairer;
import icmt.tool.momot.demo.architecture.ArchitectureFactory;
import icmt.tool.momot.demo.architecture.ArchitecturePackage;
import icmt.tool.momot.demo.architecture.ClassModel;

import java.io.IOException;

public class ArchitectureSearchJava {
   private static final int SOLUTION_LENGTH = 30;
   private static final int POPULATION_SIZE = 100;
   private static final int MAX_EVALUATIONS = 1000; // Reduced for smoke run
   private static final int NR_RUNS = 2; // Reduced for smoke run

   private static final String INPUT_MODEL = "problem/Cart_Item.xmi";

   public static void main(final String[] args) throws IOException {
      ArchitecturePackage.eINSTANCE.eClass();

      final TransformationSearchOrchestration search = new TransformationSearchOrchestration(
            new ModuleManager(), INPUT_MODEL, SOLUTION_LENGTH);
      
      search.getModuleManager().addModule("transformations/architecture.henshin");

      ClassModel cm = (ClassModel) search.getProblemGraph().getRoots().get(0);
      for(int i = 0; i < cm.getFeatures().size() - cm.getClasses().size(); i++) {
         icmt.tool.momot.demo.architecture.Class newClass = ArchitectureFactory.eINSTANCE.createClass();
         newClass.setName("Class_" + i);
         cm.getClasses().add(newClass);
      }
      for(icmt.tool.momot.demo.architecture.Feature feature : cm.getFeatures()) {
         if(feature.getIsEncapsulatedBy() == null) {
            cm.getClasses().get(MathUtil.randomInteger(cm.getClasses().size())).getEncapsulates().add(feature);
         }
      }

      search.getFitnessFunction().addObjective(new AbstractFitnessDimension<TransformationSolution>(TransformationSolution.class, "CouplingRatio") {
         @Override
         public double evaluate(TransformationSolution solution) {
            return FitnessCalculator.calculateCoupling(MomotUtil.getRoot(solution.execute(), ClassModel.class));
         }
      });
      search.getFitnessFunction().addObjective(new AbstractFitnessDimension<TransformationSolution>(TransformationSolution.class, "CohesionRatio", AbstractFitnessDimension.FunctionType.Maximum) {
         @Override
         public double evaluate(TransformationSolution solution) {
            return FitnessCalculator.calculateCohesion(MomotUtil.getRoot(solution.execute(), ClassModel.class));
         }
      });
      search.getFitnessFunction().addObjective(new TransformationLengthDimension());

      search.getFitnessFunction().setSolutionRepairer(new TransformationPlaceholderRepairer());

      final EvolutionaryAlgorithmFactory<TransformationSolution> moea = search
            .createEvolutionaryAlgorithmFactory(POPULATION_SIZE);
      
      search.addAlgorithm("Random", moea.createRandomSearch());
      search.addAlgorithm("NSGAIII", moea.createNSGAIII());
      search.addAlgorithm("eMOEA", moea.createEpsilonMOEA());

      final SearchExperiment<TransformationSolution> experiment = new SearchExperiment<>(search, MAX_EVALUATIONS);
      experiment.setNumberOfRuns(NR_RUNS);
      experiment.addProgressListener(new SeedRuntimePrintListener());

      experiment.run();

      final SearchAnalysis analysis = new SearchAnalysis(experiment);
      analysis.setAllIndicators(true);
      analysis.setShowAll(true);

      final TransformationResultManager resultManager = new TransformationResultManager(experiment);
      
      resultManager.setBaseDirectory("output/objectives/");
      resultManager.saveObjectives("objective_values.txt");
      
      resultManager.setBaseDirectory("output/models/");
      resultManager.saveModels();

      analysis.analyze().printAnalysis();
   }
}
