package at.ac.tuwien.big.moea.search.algorithm;

import at.ac.tuwien.big.moea.ISearchOrchestration;
import at.ac.tuwien.big.moea.search.algorithm.operator.mutation.PlaceholderMutation;
import at.ac.tuwien.big.moea.search.algorithm.provider.AbstractRegisteredAlgorithm;
import at.ac.tuwien.big.moea.search.algorithm.provider.IRegisteredAlgorithm;
import at.ac.tuwien.big.moea.search.solution.generator.IInjectedPopulationGenerator;

import java.util.ArrayList;
import java.util.List;

import org.moeaframework.algorithm.continuation.AdaptiveTimeContinuationExtension;
import org.moeaframework.algorithm.EpsilonMOEA;
import org.moeaframework.algorithm.IBEA;
import org.moeaframework.algorithm.NSGAII;
import org.moeaframework.algorithm.PAES;
import org.moeaframework.algorithm.PESA2;
import org.moeaframework.algorithm.RandomSearch;
import org.moeaframework.core.population.ReferencePointNondominatedSortingPopulation;
import org.moeaframework.util.weights.NormalBoundaryDivisions;
import org.moeaframework.algorithm.SMSEMOA;
import org.moeaframework.algorithm.SPEA2;
import org.moeaframework.algorithm.VEGA;
import org.moeaframework.core.fitness.FitnessEvaluator;
import org.moeaframework.core.initialization.Initialization;
import org.moeaframework.core.selection.Selection;
import org.moeaframework.core.selection.TournamentSelection;
import org.moeaframework.core.selection.UniformSelection;
import org.moeaframework.core.Solution;
import org.moeaframework.core.operator.Mutation;
import org.moeaframework.core.operator.Variation;
import org.moeaframework.core.fitness.IndicatorFitnessEvaluator;
import org.moeaframework.core.operator.CompoundVariation;
import org.moeaframework.core.operator.OnePointCrossover;
import org.moeaframework.core.operator.real.UM;

public class EvolutionaryAlgorithmFactory<S extends Solution> extends AbstractAlgorithmFactory<S> {
   private static final int MAX_POPULATIONSIZE_FACTOR = 10;
   private static final double DEFAULT_EPSILON = 0.01;
   private static final int DEFAULT_MAX_WINDOW_SIZE = 100;
   private static final int DEFAULT_WINDOW_SIZE = 100;
   private static final double DEFAULT_MUTATION_PROBABILITY = 0.10;
   private static final int DEFAULT_DIVISION_OUTER = 4;
   protected int populationSize = 100;
   protected List<S> initialSolutions = new ArrayList<>();

   public EvolutionaryAlgorithmFactory() {}

   public EvolutionaryAlgorithmFactory(final ISearchOrchestration<S> searchOrchestration) {
      setSearchOrchestration(searchOrchestration);
   }

   public void addInitialSolution(final S solution) {
      this.initialSolutions.add(solution);
   }

   public IRegisteredAlgorithm<NSGAII> createENSGAII(final double injectionRate,
         final Selection selection, final Variation... variation) {
      return new AbstractRegisteredAlgorithm<NSGAII>() {
         @Override
         public NSGAII createAlgorithm() {
            final NSGAII nsgaii = createNSGAII(selection, variation).createAlgorithm();
            final AdaptiveTimeContinuationExtension extension = new AdaptiveTimeContinuationExtension(
                  DEFAULT_WINDOW_SIZE, DEFAULT_MAX_WINDOW_SIZE, 1.0 / injectionRate, getPopulationSize(),
                  getPopulationSize() * MAX_POPULATIONSIZE_FACTOR, new UniformSelection(), new UM(1.0));
            nsgaii.addExtension(extension);
            return nsgaii;
         }
      };
   }

   public AbstractRegisteredAlgorithm<EpsilonMOEA> createEpsilonMOEA() {
      return createEpsilonMOEA(DEFAULT_EPSILON, new TournamentSelection(2), new OnePointCrossover(1.0),
            new PlaceholderMutation(DEFAULT_MUTATION_PROBABILITY));
   }

   public AbstractRegisteredAlgorithm<EpsilonMOEA> createEpsilonMOEA(final double epsilon, final Selection selection,
         final Variation... variation) {
      return new AbstractRegisteredAlgorithm<EpsilonMOEA>() {
         @Override
         public EpsilonMOEA createAlgorithm() {
            return new EpsilonMOEA(createProblem(), getPopulationSize(), createPopulation(), createEpsilonBoxArchive(epsilon), selection,
                  createVariation(variation), createInitialization());
         }
      };
   }

   public AbstractRegisteredAlgorithm<IBEA> createIBEA(final IndicatorFitnessEvaluator evaluator,
         final Variation... variation) {
      return new AbstractRegisteredAlgorithm<IBEA>() {
         @Override
         public IBEA createAlgorithm() {
            return new IBEA(createProblem(), getPopulationSize(), createPopulation(), createInitialization(), createVariation(variation),
                  evaluator);
         }
      };
   }

   protected Initialization createInitialization() {
      final IInjectedPopulationGenerator<S> initialization = getSearchOrchestration()
            .createPopulationGenerator(getPopulationSize());
      initialization.addInjectedSolutions(getInitialSolutions());
      return initialization;
   }

   public IRegisteredAlgorithm<NSGAII> createNSGAII() {
      return createNSGAII(new TournamentSelection(2), new OnePointCrossover(1.0),
            new PlaceholderMutation(DEFAULT_MUTATION_PROBABILITY));
   }

   public IRegisteredAlgorithm<NSGAII> createNSGAII(final Selection selection, final Variation... variation) {
      return new AbstractRegisteredAlgorithm<NSGAII>() {
         @Override
         public NSGAII createAlgorithm() {
            return new NSGAII(createProblem(), getPopulationSize(), createSortingPopulation(), createEpsilonBoxArchive(), selection,
                  createVariation(variation), createInitialization());
         }
      };
   }

   public IRegisteredAlgorithm<NSGAII> createNSGAIII() {
      return createNSGAIII(DEFAULT_DIVISION_OUTER, 0, new TournamentSelection(2), new OnePointCrossover(1.0),
            new PlaceholderMutation(DEFAULT_MUTATION_PROBABILITY));
   }

   public IRegisteredAlgorithm<NSGAII> createNSGAIII(final int divisionsOuter, final int divisionsInner,
         final Selection selection, final Variation... variation) {
      return new AbstractRegisteredAlgorithm<NSGAII>() {
         @Override
         public NSGAII createAlgorithm() {
            return new NSGAII(createProblem(), getPopulationSize(),
                  new ReferencePointNondominatedSortingPopulation(
                        getSearchOrchestration().getProblem().getNumberOfObjectives(), new NormalBoundaryDivisions(divisionsOuter, divisionsInner)),
                  createEpsilonBoxArchive(), selection, createVariation(variation), createInitialization());
         }
      };
   }

   public IRegisteredAlgorithm<NSGAII> createNSGAIII(final int divisions, final Selection selection,
         final Variation... variation) {
      return createNSGAIII(divisions, 0, selection, variation);
   }

   public IRegisteredAlgorithm<NSGAII> createNSGAIII(final Selection selection, final Variation... variation) {
      return createNSGAIII(DEFAULT_DIVISION_OUTER, 0, selection, variation);
   }

   public AbstractRegisteredAlgorithm<PAES> createPAES(final int bisections, final int archiveSize,
         final Variation... variation) {
      return new AbstractRegisteredAlgorithm<PAES>() {
         @Override
         public PAES createAlgorithm() {
            final Variation var = createVariation(variation);
            final Mutation mut = var instanceof Mutation ? (Mutation) var : new PlaceholderMutation(DEFAULT_MUTATION_PROBABILITY);
            return new PAES(createProblem(), mut, bisections, archiveSize);
         }
      };
   }

   public AbstractRegisteredAlgorithm<PESA2> createPESA2(final int bisections, final int archiveSize,
         final Variation... variation) {
      return new AbstractRegisteredAlgorithm<PESA2>() {
         @Override
         public PESA2 createAlgorithm() {
            return new PESA2(createProblem(), getPopulationSize(), createVariation(variation), createInitialization(), bisections,
                  archiveSize);
         }
      };
   }

   public IRegisteredAlgorithm<RandomSearch> createRandomSearch() {
      return new AbstractRegisteredAlgorithm<RandomSearch>() {
         @Override
         public RandomSearch createAlgorithm() {
            return new RandomSearch(createProblem(), getPopulationSize(), createInitialization(), createPopulation());
         }
      };
   }

   public AbstractRegisteredAlgorithm<SMSEMOA> createSMSEMOA(final FitnessEvaluator evaluator,
         final Variation... variation) {
      return new AbstractRegisteredAlgorithm<SMSEMOA>() {
         @Override
         public SMSEMOA createAlgorithm() {
            return new SMSEMOA(createProblem(), getPopulationSize(), createInitialization(), createVariation(variation), evaluator);
         }
      };
   }

   public AbstractRegisteredAlgorithm<SPEA2> createSPEA2(final int numberOfOffspring, final int k,
         final Variation... variation) {
      return new AbstractRegisteredAlgorithm<SPEA2>() {
         @Override
         public SPEA2 createAlgorithm() {
            return new SPEA2(createProblem(), getPopulationSize(), createInitialization(), createVariation(variation), numberOfOffspring, k);
         }
      };
   }

   public AbstractRegisteredAlgorithm<SPEA2> createSPEA2(final int numberOfOffspring, final Variation... variation) {
      return createSPEA2(numberOfOffspring, 1, variation);
   }

   protected Variation createVariation(final Variation... variation) {
      return new CompoundVariation(variation);
   }

   public AbstractRegisteredAlgorithm<VEGA> createVEGA(final Variation... variation) {
      return new AbstractRegisteredAlgorithm<VEGA>() {
         @Override
         public VEGA createAlgorithm() {
            return new VEGA(createProblem(), getPopulationSize(), createPopulation(), createPopulation(), createInitialization(),
                  createVariation(variation));
         }
      };
   }

   public List<S> getInitialSolutions() {
      return initialSolutions;
   }

   public int getPopulationSize() {
      return populationSize;
   }

   public void setInitialSolutions(final List<S> initialSolutions) {
      this.initialSolutions = initialSolutions;
   }

   public void setPopulationSize(final int populationSize) {
      this.populationSize = populationSize;
   }
}
