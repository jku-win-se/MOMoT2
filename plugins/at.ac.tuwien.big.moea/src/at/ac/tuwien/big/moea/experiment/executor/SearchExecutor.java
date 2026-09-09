package at.ac.tuwien.big.moea.experiment.executor;

import at.ac.tuwien.big.moea.experiment.instrumenter.SearchInstrumenter;
import at.ac.tuwien.big.moea.problem.ISearchProblem;
import at.ac.tuwien.big.moea.search.algorithm.provider.AbstractRegisteredAlgorithm;
import at.ac.tuwien.big.moea.search.algorithm.provider.DynamicAlgorithmFactory;
import at.ac.tuwien.big.moea.util.CastUtil;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.moeaframework.analysis.runtime.Instrumenter;
import org.moeaframework.algorithm.Algorithm;
import org.moeaframework.algorithm.AlgorithmTerminationException;
import org.moeaframework.algorithm.extension.CheckpointExtension;
import org.moeaframework.algorithm.extension.Frequency;
import org.moeaframework.core.TypedProperties;
import org.moeaframework.problem.Problem;
import org.moeaframework.core.comparator.ParetoDominanceComparator;
import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.core.spi.AlgorithmFactory;
import org.moeaframework.core.spi.ProblemFactory;
import org.moeaframework.core.termination.CompoundTerminationCondition;
import org.moeaframework.core.termination.MaxElapsedTime;
import org.moeaframework.core.termination.MaxFunctionEvaluations;
import org.moeaframework.core.termination.TerminationCondition;
import org.moeaframework.parallel.DistributedProblem;
import org.moeaframework.util.progress.ProgressEvent;
import org.moeaframework.util.progress.ProgressListener;

public class SearchExecutor {

   protected String name;
   protected Algorithm algorithm;
   protected String algorithmName;
   protected AlgorithmFactory algorithmFactory;
   protected Problem problem;
   protected ProblemFactory problemFactory;
   protected Class<?> problemClass;
   protected Object[] problemArguments;
   protected TypedProperties properties = new TypedProperties();
   protected int maxEvaluations = 25000;
   protected long maxTime = -1;
   protected Instrumenter instrumenter;
   protected List<ProgressListener> progressListeners = new ArrayList<>();
   protected File checkpointFile;
   protected int checkpointFrequency;
   protected AtomicBoolean isCanceled = new AtomicBoolean(false);
   protected int numberOfThreads = 1;
   protected ExecutorService executorService;
   protected double[] epsilon;
   protected List<TerminationCondition> terminationConditions = new ArrayList<>();

   public SearchExecutor() {}

   public SearchExecutor(final ISearchProblem<?> problem) {
      this();
      withProblem(problem);
   }

   public SearchExecutor(final String name) {
      this();
      setName(name);
   }

   public SearchExecutor(final String name, final ISearchProblem<?> problem) {
      this();
      setName(name);
      withProblem(problem);
   }

   public SearchExecutor checkpointEveryIteration() {
      this.checkpointFrequency = 1;
      return this;
   }

   public SearchExecutor clearProperties() {
      this.properties = new TypedProperties();
      return this;
   }

   protected Algorithm createAlgorithm(final Problem problem) {
      Algorithm alg = null;
      if(getAlgorithmName() != null) {
         AlgorithmFactory factory = getAlgorithmFactory();
         if(factory == null) {
            factory = new DynamicAlgorithmFactory();
         }
         alg = factory.getAlgorithm(getAlgorithmName(), getTypedProperties(), problem);
      } else {
         alg = getAlgorithm();
      }

      if(alg != null) {
         if(getCheckpointFile() != null && getCheckpointFrequency() > 0) {
            alg.addExtension(new CheckpointExtension(getCheckpointFile(), Frequency.ofIterations(getCheckpointFrequency())));
         }

         if(getInstrumenter() != null) {
            alg = getInstrumenter().instrument(alg);
         }
      }
      return alg;
   }

   protected TerminationCondition createTerminationCondition() {
      final List<TerminationCondition> conditions = new ArrayList<>(terminationConditions);
      if(maxEvaluations > 0) {
         conditions.add(new MaxFunctionEvaluations(maxEvaluations));
      }
      if(maxTime > 0) {
         conditions.add(new MaxElapsedTime(Duration.ofMillis(maxTime)));
      }
      if(conditions.isEmpty()) {
         return new MaxFunctionEvaluations(25000);
      } else if(conditions.size() == 1) {
         return conditions.get(0);
      } else {
         return new CompoundTerminationCondition(conditions.toArray(new TerminationCondition[0]));
      }
   }

   public SearchExecutor distributeOn(final int numberOfThreads) {
      this.numberOfThreads = numberOfThreads;
      return this;
   }

   public SearchExecutor distributeOnAllCores() {
      return distributeOn(Runtime.getRuntime().availableProcessors());
   }

   public SearchExecutor distributeWith(final ExecutorService executorService) {
      this.executorService = executorService;
      return this;
   }

   public Algorithm getAlgorithm() {
      return algorithm;
   }

   public AlgorithmFactory getAlgorithmFactory() {
      return algorithmFactory;
   }

   public String getAlgorithmName() {
      return algorithmName != null ? algorithmName : "<no-algorithm-name>";
   }

   public AtomicBoolean getCanceled() {
      return isCanceled;
   }

   public File getCheckpointFile() {
      return checkpointFile;
   }

   public int getCheckpointFrequency() {
      return checkpointFrequency;
   }

   public ExecutorService getExecutorService() {
      return executorService;
   }

   public Instrumenter getInstrumenter() {
      return instrumenter;
   }

   public String getName() {
      if(name == null && getAlgorithmName() != null) {
         return getAlgorithmName();
      }
      return name;
   }

   public int getNumberOfThreads() {
      return numberOfThreads;
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
                        return CastUtil.asClass(ctor.newInstance(problemArguments), Problem.class);
                     }
                  }
               }
            } else {
               return CastUtil.asClass(problemClass.getDeclaredConstructor().newInstance(), Problem.class);
            }
         } catch(final Exception e) {
            e.printStackTrace();
         }
      }
      if(problemFactory != null && name != null) {
         return problemFactory.getProblem(name);
      }
      return null;
   }

   public Class<?> getProblemClass() {
      return problemClass;
   }

   public Object[] getProblemArguments() {
      return problemArguments;
   }

   public ProblemFactory getProblemFactory() {
      return problemFactory;
   }

   public List<ProgressListener> getProgressListeners() {
      return progressListeners;
   }

   public List<TerminationCondition> getTerminationConditions() {
      return terminationConditions;
   }

   public TypedProperties getTypedProperties() {
      return properties;
   }

   public boolean isCanceled() {
      return isCanceled.get();
   }

   protected NondominatedPopulation newArchivePopulation() {
      return new NondominatedPopulation(new ParetoDominanceComparator());
   }

   protected void notifyProgress(final int currentSeed, final int totalSeeds, final int currentNFE, final int maxNFE,
         final double elapsedTime, final double remainingTime) {
      final ProgressEvent event = new ProgressEvent(this, currentSeed, totalSeeds, currentNFE, maxNFE, elapsedTime, remainingTime);
      for(final ProgressListener listener : progressListeners) {
         listener.progressUpdate(event);
      }
   }

   public SearchExecutor resetCheckpointFile() throws IOException {
      if(checkpointFile != null && checkpointFile.exists()) {
         checkpointFile.delete();
      }
      return this;
   }

   public NondominatedPopulation run() {
      setCanceled(false);
      return runSingleSeed(1, 1, createTerminationCondition());
   }

   protected NondominatedPopulation runAlgorithm(final Problem problem, final TerminationCondition terminationCondition) {
      return runAlgorithm(problem, terminationCondition, 1, 1);
   }

   protected NondominatedPopulation runAlgorithm(final Problem problem, final TerminationCondition terminationCondition,
         final int currentSeed, final int totalSeeds) {
      final NondominatedPopulation result = newArchivePopulation();
      Algorithm alg = null;
      try {
         alg = createAlgorithm(problem);
         this.algorithm = alg;

         terminationCondition.initialize(alg);

         while(!alg.isTerminated() && !terminationCondition.shouldTerminate(alg)) {
            if(isCanceled()) {
               return null;
            }
            alg.step();
            notifyProgress(currentSeed, totalSeeds, alg.getNumberOfEvaluations(), maxEvaluations, 0, 0);
         }

         result.addAll(alg.getResult());
      } finally {
         if(alg != null) {
            alg.terminate();
         }
      }
      return result;
   }

   public List<NondominatedPopulation> runSeeds(final int numberOfSeeds) {
      final List<NondominatedPopulation> results = new ArrayList<>();
      setCanceled(false);
      final long startTime = System.currentTimeMillis();
      for(int seed = 1; seed <= numberOfSeeds; seed++) {
         if(isCanceled()) {
            break;
         }
         final long seedStartTime = System.currentTimeMillis();
         final NondominatedPopulation result = runSingleSeed(seed, numberOfSeeds, createTerminationCondition());
         final double elapsed = (System.currentTimeMillis() - startTime) / 1000.0;
         final double seedElapsed = (System.currentTimeMillis() - seedStartTime) / 1000.0;
         final double remaining = (numberOfSeeds - seed) * seedElapsed;
         notifyProgress(seed, numberOfSeeds, maxEvaluations, maxEvaluations, elapsed, remaining);
         if(result != null) {
            results.add(result);
         }
      }
      return results;
   }

   protected NondominatedPopulation runSingleSeed(final int seed, final int numberOfSeeds, final int maxEvaluations) {
      this.withMaxEvaluations(maxEvaluations);
      return runSingleSeed(seed, numberOfSeeds, createTerminationCondition());
   }

   protected NondominatedPopulation runSingleSeed(final int seed, final int numberOfSeeds,
         final TerminationCondition terminationCondition) {
      if(getAlgorithmName() == null && algorithm == null) {
         throw new IllegalArgumentException("No algorithm specified");
      }

      Problem prob = null;
      ExecutorService execService = null;

      try {
         prob = getProblem();
         if(getExecutorService() != null) {
            prob = new DistributedProblem(prob, getExecutorService());
         } else if(getNumberOfThreads() > 1) {
            execService = Executors.newFixedThreadPool(getNumberOfThreads());
            prob = new DistributedProblem(prob, execService);
         }

         return runAlgorithm(prob, terminationCondition, seed, numberOfSeeds);
      } catch(final AlgorithmTerminationException e) {
         System.err.println(e.getMessage());
         return null;
      } catch(final SecurityException | IllegalArgumentException ex) {
         ex.printStackTrace();
         System.err.println(ex.getMessage());
         return null;
      } finally {
         if(execService != null) {
            execService.shutdown();
         }
         if(prob != null) {
            prob.close();
         }
      }
   }

   protected void setCanceled(final boolean cancel) {
      isCanceled.set(cancel);
   }

   public SearchExecutor setName(final String name) {
      this.name = name;
      return this;
   }

   public void terminateRun() {
      if(algorithm != null && !algorithm.isTerminated()) {
         algorithm.terminate();
      }
   }

   @Override
   public String toString() {
      return getClass().getSimpleName() + "['" + getName() + "']";
   }

   public SearchExecutor usingAlgorithmFactory(final AlgorithmFactory algorithmFactory) {
      this.algorithmFactory = algorithmFactory;
      return this;
   }

   public SearchExecutor usingProblemFactory(final ProblemFactory problemFactory) {
      this.problemFactory = problemFactory;
      return this;
   }

   public <A extends Algorithm> SearchExecutor withAlgorithm(final A algorithm) {
      return withAlgorithm(new AbstractRegisteredAlgorithm<A>() {
         @Override
         public A createAlgorithm() {
            return algorithm;
         }
      }.register());
   }

   public SearchExecutor withAlgorithm(final String algorithmName) {
      this.algorithmName = algorithmName;
      return this;
   }

   public SearchExecutor withCheckpointFile(final File checkpointFile) {
      this.checkpointFile = checkpointFile;
      return this;
   }

   public SearchExecutor withCheckpointFrequency(final int checkpointFrequency) {
      this.checkpointFrequency = checkpointFrequency;
      return this;
   }

   public SearchExecutor withEpsilon(final double... epsilon) {
      this.epsilon = epsilon;
      return this;
   }

   public SearchExecutor withInstrumenter(final Instrumenter instrumenter) {
      this.instrumenter = instrumenter;
      return this;
   }

   public SearchExecutor withMaxEvaluations(final int maxEvaluations) {
      this.maxEvaluations = maxEvaluations;
      this.properties.setInt("maxEvaluations", maxEvaluations);
      return this;
   }

   public SearchExecutor withMaxTime(final long maxTime) {
      this.maxTime = maxTime;
      this.properties.setLong("maxTime", maxTime);
      return this;
   }

   public SearchExecutor withProblem(final ISearchProblem<?> problem) {
      if(problem != null) {
         this.problemClass = problem.getClass();
         this.problemArguments = new Object[] { problem.getFitnessFunction(), problem.getSolutionGenerator() };
      }
      return this;
   }

   public SearchExecutor withProblem(final Problem problem) {
      this.problem = problem;
      return this;
   }

   public SearchExecutor withProblem(final String problemName) {
      if(problemFactory != null) {
         this.problem = problemFactory.getProblem(problemName);
      }
      return this;
   }

   public SearchExecutor withProblemClass(final Class<?> problemClass, final Object... problemArguments) {
      this.problemClass = problemClass;
      this.problemArguments = problemArguments;
      return this;
   }

   public SearchExecutor withProblemClass(final String problemClassName, final Object... problemArguments)
         throws ClassNotFoundException {
      this.problemClass = Class.forName(problemClassName);
      this.problemArguments = problemArguments;
      return this;
   }

   public SearchExecutor withProgressListener(final ProgressListener listener) {
      if(listener != null && !progressListeners.contains(listener)) {
         this.progressListeners.add(listener);
      }
      return this;
   }

   public SearchExecutor withProperties(final Properties properties) {
      if(properties != null) {
         for(final String key : properties.stringPropertyNames()) {
            this.properties.setString(key, properties.getProperty(key));
         }
      }
      return this;
   }

   public SearchExecutor withProperty(final String key, final boolean value) {
      this.properties.setBoolean(key, value);
      return this;
   }

   public SearchExecutor withProperty(final String key, final byte value) {
      this.properties.setByte(key, value);
      return this;
   }

   public SearchExecutor withProperty(final String key, final byte[] values) {
      this.properties.setByteArray(key, values);
      return this;
   }

   public SearchExecutor withProperty(final String key, final double value) {
      this.properties.setDouble(key, value);
      return this;
   }

   public SearchExecutor withProperty(final String key, final double[] values) {
      this.properties.setDoubleArray(key, values);
      return this;
   }

   public SearchExecutor withProperty(final String key, final float value) {
      this.properties.setFloat(key, value);
      return this;
   }

   public SearchExecutor withProperty(final String key, final float[] values) {
      this.properties.setFloatArray(key, values);
      return this;
   }

   public SearchExecutor withProperty(final String key, final int value) {
      this.properties.setInt(key, value);
      return this;
   }

   public SearchExecutor withProperty(final String key, final int[] values) {
      this.properties.setIntArray(key, values);
      return this;
   }

   public SearchExecutor withProperty(final String key, final long value) {
      this.properties.setLong(key, value);
      return this;
   }

   public SearchExecutor withProperty(final String key, final long[] values) {
      this.properties.setLongArray(key, values);
      return this;
   }

   public SearchExecutor withProperty(final String key, final short value) {
      this.properties.setShort(key, value);
      return this;
   }

   public SearchExecutor withProperty(final String key, final short[] values) {
      this.properties.setShortArray(key, values);
      return this;
   }

   public SearchExecutor withProperty(final String key, final String value) {
      this.properties.setString(key, value);
      return this;
   }

   public SearchExecutor withProperty(final String key, final String[] values) {
      this.properties.setStringArray(key, values);
      return this;
   }

   public SearchExecutor withSameProblemAs(final Instrumenter instrumenter) {
      if(instrumenter instanceof SearchInstrumenter) {
         final SearchInstrumenter searchInst = (SearchInstrumenter) instrumenter;
         if(searchInst.getProblem() != null) {
            withProblem(searchInst.getProblem());
         } else if(searchInst.getProblemClass() != null) {
            withProblemClass(searchInst.getProblemClass(), searchInst.getProblemArguments());
         } else if(searchInst.getProblemName() != null) {
            withProblem(searchInst.getProblemName());
         }
      }
      return this;
   }

   public SearchExecutor withSameProblemAs(final SearchExecutor executor) {
      if(executor != null && executor.getProblem() != null) {
         withProblem(executor.getProblem());
      }
      return this;
   }

   public SearchExecutor withTerminationCondition(final TerminationCondition condition) {
      if(condition != null) {
         this.terminationConditions.add(condition);
      }
      return this;
   }
}
