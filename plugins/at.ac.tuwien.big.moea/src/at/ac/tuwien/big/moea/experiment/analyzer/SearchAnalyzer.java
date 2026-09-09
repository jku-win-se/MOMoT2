package at.ac.tuwien.big.moea.experiment.analyzer;

import at.ac.tuwien.big.moea.experiment.analyzer.chart.SearchBoxPlot;
import at.ac.tuwien.big.moea.experiment.analyzer.effectsize.CliffsDeltaEffectSize;
import at.ac.tuwien.big.moea.experiment.analyzer.effectsize.CohensDEffectSize;
import at.ac.tuwien.big.moea.experiment.analyzer.effectsize.VarghaDelaneyAEffectSize;
import at.ac.tuwien.big.moea.experiment.executor.SearchExecutor;
import at.ac.tuwien.big.moea.problem.ISearchProblem;
import at.ac.tuwien.big.moea.util.CastUtil;
import at.ac.tuwien.big.moea.util.FileUtil;
import at.ac.tuwien.big.moea.util.PopulationUtil;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.UnivariateStatistic;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.apache.commons.math3.stat.descriptive.rank.Max;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.commons.math3.stat.descriptive.rank.Min;
import org.moeaframework.analysis.runtime.Instrumenter;
import at.ac.tuwien.big.moea.experiment.instrumenter.SearchInstrumenter;
import org.moeaframework.analysis.IndicatorStatistics;
import org.moeaframework.problem.Problem;
import org.moeaframework.core.comparator.ParetoDominanceComparator;
import org.moeaframework.core.indicator.AdditiveEpsilonIndicator;
import org.moeaframework.core.indicator.Contribution;
import org.moeaframework.core.indicator.GenerationalDistance;
import org.moeaframework.core.indicator.Hypervolume;
import org.moeaframework.core.indicator.Indicator;
import org.moeaframework.core.indicator.InvertedGenerationalDistance;
import org.moeaframework.core.indicator.MaximumParetoFrontError;
import org.moeaframework.core.indicator.R1Indicator;
import org.moeaframework.core.indicator.R2Indicator;
import org.moeaframework.core.indicator.R3Indicator;
import org.moeaframework.core.indicator.Spacing;
import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.core.spi.ProblemFactory;

public class SearchAnalyzer {

   protected Problem problem;
   protected Class<?> problemClass;
   protected Object[] problemArguments;
   protected ProblemFactory problemFactory;
   protected File referenceSetFile;
   protected NondominatedPopulation referenceSet;
   protected double[] epsilon;
   protected Map<String, List<NondominatedPopulation>> data = new LinkedHashMap<>();
   protected List<UnivariateStatistic> statistics = new ArrayList<>();
   protected boolean showAggregate = true;
   protected boolean showStatisticalSignificance = true;
   protected boolean showIndividualValues = true;
   protected double significanceLevel = 0.05;

   protected boolean includeHypervolume = false;
   protected boolean includeGenerationalDistance = false;
   protected boolean includeInvertedGenerationalDistance = false;
   protected boolean includeAdditiveEpsilonIndicator = false;
   protected boolean includeContribution = false;
   protected boolean includeSpacing = false;
   protected boolean includeMaximumParetoFrontError = false;
   protected boolean includeR1 = false;
   protected boolean includeR2 = false;
   protected boolean includeR3 = false;

   public SearchAnalyzer() {
      showStatistic(new Min());
      showStatistic(new Median());
      showStatistic(new Max());
      showStatistic(new Mean());
      showStatistic(new StandardDeviation());
      showStatistic(new Variance());
   }

   public SearchAnalyzer(final ISearchProblem<?> problem) {
      this();
      withProblem(problem);
   }

   public SearchAnalyzer add(final String name, final NondominatedPopulation result) {
      List<NondominatedPopulation> list = data.get(name);
      if(list == null) {
         list = new ArrayList<>();
         data.put(name, list);
      }
      if(result != null) {
         list.add(result);
      }
      return this;
   }

   public SearchAnalyzer addAll(final String name, final Collection<NondominatedPopulation> results) {
      List<NondominatedPopulation> list = data.get(name);
      if(list == null) {
         list = new ArrayList<>();
         data.put(name, list);
      }
      if(results != null) {
         list.addAll(results);
      }
      return this;
   }

   protected void addEffectSizes(final SearchAnalyzerResults results) {
      final List<String> algorithms = results.getAlgorithms();
      for(final String indicator : results.getIndicators()) {
         for(int i = 0; i < algorithms.size() - 1; i++) {
            for(int j = i + 1; j < algorithms.size(); j++) {
               final String leftAlgorithm = algorithms.get(i);
               final String rightAlgorithm = algorithms.get(j);
               final SearchAlgorithmResult leftAlgorithmResults = results.get(leftAlgorithm);
               final SearchAlgorithmResult rightAlgorithmResults = results.get(rightAlgorithm);
               if(leftAlgorithmResults == null || rightAlgorithmResults == null) {
                  continue;
               }
               final SearchIndicatorResult leftIndicatorResults = leftAlgorithmResults.get(indicator);
               final SearchIndicatorResult rightIndicatorResults = rightAlgorithmResults.get(indicator);
               if(leftIndicatorResults != null && rightIndicatorResults != null) {
                  leftIndicatorResults.addAlgorithmEffectSize(CohensDEffectSize.calculate(rightAlgorithm,
                        leftIndicatorResults.getValues(), rightIndicatorResults.getValues()));
                  leftIndicatorResults.addAlgorithmEffectSize(CliffsDeltaEffectSize.calculate(rightAlgorithm,
                        leftIndicatorResults.getValues(), rightIndicatorResults.getValues()));
                  leftIndicatorResults.addAlgorithmEffectSize(VarghaDelaneyAEffectSize.calculate(rightAlgorithm,
                        leftIndicatorResults.getValues(), rightIndicatorResults.getValues()));

                  rightIndicatorResults.addAlgorithmEffectSize(CohensDEffectSize.calculate(leftAlgorithm,
                        rightIndicatorResults.getValues(), leftIndicatorResults.getValues()));
                  rightIndicatorResults.addAlgorithmEffectSize(CliffsDeltaEffectSize.calculate(leftAlgorithm,
                        rightIndicatorResults.getValues(), leftIndicatorResults.getValues()));
                  rightIndicatorResults.addAlgorithmEffectSize(VarghaDelaneyAEffectSize.calculate(leftAlgorithm,
                        rightIndicatorResults.getValues(), leftIndicatorResults.getValues()));
               }
            }
         }
      }
   }

   public SearchAnalyzer clear() {
      data.clear();
      return this;
   }

   public Map<String, List<NondominatedPopulation>> getData() {
      return data;
   }

   public Problem getProblem() {
      if(problem != null) {
         return problem;
      }
      if(problemClass != null) {
         try {
            if(problemArguments != null && problemArguments.length > 0) {
               final Class<?>[] paramTypes = new Class<?>[problemArguments.length];
               for(int i = 0; i < problemArguments.length; i++) {
                  paramTypes[i] = problemArguments[i].getClass();
               }
               return CastUtil.asClass(problemClass.getConstructor(paramTypes).newInstance(problemArguments), Problem.class);
            } else {
               return CastUtil.asClass(problemClass.getDeclaredConstructor().newInstance(), Problem.class);
            }
         } catch(final Exception e) {
            e.printStackTrace();
         }
      }
      return null;
   }

   public NondominatedPopulation getReferenceSet() {
      if(referenceSet != null) {
         return referenceSet;
      }
      if(referenceSetFile != null && referenceSetFile.exists()) {
         try {
            return PopulationUtil.readObjectives(referenceSetFile);
         } catch(final IOException e) {
            e.printStackTrace();
         }
      }
      return null;
   }

   public SearchAnalyzerResults getSearchAnalysis() {
      final SearchAnalyzerResults analyzerResults = new SearchAnalyzerResults();

      NondominatedPopulation refSet = getReferenceSet();
      if(refSet == null || refSet.isEmpty()) {
         refSet = new NondominatedPopulation(new ParetoDominanceComparator());
         for(final List<NondominatedPopulation> pops : data.values()) {
            for(final NondominatedPopulation pop : pops) {
               refSet.addAll(pop);
            }
         }
      }

      final Problem prob = getProblem();

      final List<Indicator> indicators = new ArrayList<>();
      final List<String> indicatorNames = new ArrayList<>();

      if(includeHypervolume) {
         indicators.add(new Hypervolume(prob, refSet));
         indicatorNames.add("Hypervolume");
      }
      if(includeGenerationalDistance) {
         indicators.add(new GenerationalDistance(prob, refSet));
         indicatorNames.add("GenerationalDistance");
      }
      if(includeInvertedGenerationalDistance) {
         indicators.add(new InvertedGenerationalDistance(prob, refSet));
         indicatorNames.add("InvertedGenerationalDistance");
      }
      if(includeAdditiveEpsilonIndicator) {
         indicators.add(new AdditiveEpsilonIndicator(prob, refSet));
         indicatorNames.add("AdditiveEpsilonIndicator");
      }
      if(includeContribution) {
         indicators.add(new Contribution(refSet));
         indicatorNames.add("Contribution");
      }
      if(includeSpacing) {
         indicators.add(new Spacing(prob));
         indicatorNames.add("Spacing");
      }
      if(includeMaximumParetoFrontError) {
         indicators.add(new MaximumParetoFrontError(prob, refSet));
         indicatorNames.add("MaximumParetoFrontError");
      }
      if(includeR1) {
         indicators.add(new R1Indicator(prob, 100, refSet));
         indicatorNames.add("R1");
      }
      if(includeR2) {
         indicators.add(new R2Indicator(prob, 100, refSet));
         indicatorNames.add("R2");
      }
      if(includeR3) {
         indicators.add(new R3Indicator(prob, 100, refSet));
         indicatorNames.add("R3");
      }

      for(final String algName : data.keySet()) {
         analyzerResults.add(new SearchAlgorithmResult(algName));
      }

      for(int k = 0; k < indicators.size(); k++) {
         final Indicator indicator = indicators.get(k);
         final String indName = indicatorNames.get(k);
         final IndicatorStatistics stats = new IndicatorStatistics(indicator);

         for(final Map.Entry<String, List<NondominatedPopulation>> entry : data.entrySet()) {
            stats.addAll(entry.getKey(), entry.getValue());
         }

         for(final String algName : data.keySet()) {
            final SearchAlgorithmResult algResult = analyzerResults.get(algName);
            if(algResult != null) {
               final double[] vals = stats.getValues(algName);
               final SearchIndicatorResult indResult = new SearchIndicatorResult(indName, vals != null ? vals : new double[0]);
               if(vals != null && vals.length > 0) {
                  indResult.setAggregateValue(stats.getMean(algName));
               }
               algResult.add(indResult);
            }
         }
      }

      if(isShowStatisticalSignificance()) {
         addEffectSizes(analyzerResults);
      }

      return analyzerResults;
   }

   public List<UnivariateStatistic> getStatistics() {
      return statistics;
   }

   public SearchAnalyzer includeAdditiveEpsilonIndicator() {
      this.includeAdditiveEpsilonIndicator = true;
      return this;
   }

   public SearchAnalyzer includeAllMetrics() {
      includeHypervolume();
      includeGenerationalDistance();
      includeInvertedGenerationalDistance();
      includeAdditiveEpsilonIndicator();
      includeContribution();
      includeSpacing();
      includeMaximumParetoFrontError();
      return this;
   }

   public SearchAnalyzer includeContribution() {
      this.includeContribution = true;
      return this;
   }

   public SearchAnalyzer includeGenerationalDistance() {
      this.includeGenerationalDistance = true;
      return this;
   }

   public SearchAnalyzer includeHypervolume() {
      this.includeHypervolume = true;
      return this;
   }

   public SearchAnalyzer includeInvertedGenerationalDistance() {
      this.includeInvertedGenerationalDistance = true;
      return this;
   }

   public SearchAnalyzer includeMaximumParetoFrontError() {
      this.includeMaximumParetoFrontError = true;
      return this;
   }

   public SearchAnalyzer includeR1() {
      this.includeR1 = true;
      return this;
   }

   public SearchAnalyzer includeR2() {
      this.includeR2 = true;
      return this;
   }

   public SearchAnalyzer includeR3() {
      this.includeR3 = true;
      return this;
   }

   public SearchAnalyzer includeSpacing() {
      this.includeSpacing = true;
      return this;
   }

   public boolean isShowAggregate() {
      return showAggregate;
   }

   public boolean isShowIndividualValues() {
      return showIndividualValues;
   }

   public boolean isShowStatisticalSignificance() {
      return showStatisticalSignificance;
   }

   public SearchAnalyzer loadAs(final String name, final File resultFile) throws IOException {
      final NondominatedPopulation pop = PopulationUtil.readObjectives(resultFile);
      add(name, pop);
      return this;
   }

   public SearchAnalyzer loadData(final File directory, final String prefix, final String suffix) throws IOException {
      if(directory != null && directory.isDirectory()) {
         final File[] files = directory.listFiles();
         if(files != null) {
            for(final File f : files) {
               if(f.getName().startsWith(prefix) && f.getName().endsWith(suffix)) {
                  final String name = f.getName().substring(prefix.length(), f.getName().length() - suffix.length());
                  loadAs(name, f);
               }
            }
         }
      }
      return this;
   }

   public SearchAnalyzer printAnalysis() {
      printAnalysis(System.out);
      return this;
   }

   public SearchAnalyzer printAnalysis(final PrintStream ps) {
      getSearchAnalysis().print(ps, isShowAggregate(), isShowStatisticalSignificance(), isShowIndividualValues(),
            getStatistics());
      return this;
   }

   public SearchAnalyzer saveAnalysis(final File file) throws IOException {
      file.getParentFile().mkdirs();
      try(PrintStream ps = new PrintStream(file)) {
         printAnalysis(ps);
      }
      return this;
   }

   public SearchAnalyzer saveAs(final String name, final File resultFile) throws IOException {
      final List<NondominatedPopulation> pops = data.get(name);
      if(pops != null) {
         final NondominatedPopulation merged = new NondominatedPopulation(new ParetoDominanceComparator());
         for(final NondominatedPopulation pop : pops) {
            merged.addAll(pop);
         }
         PopulationUtil.writeObjectives(resultFile, merged);
      }
      return this;
   }

   public SearchAnalyzer saveData(final File directory, final String prefix, final String suffix) throws IOException {
      for(final String name : data.keySet()) {
         saveAs(name, new File(directory, prefix + name + suffix));
      }
      return this;
   }

   public SearchAnalyzer saveIndicatorBoxPlots(final String directory) {
      return saveIndicatorBoxPlots(directory, null);
   }

   public SearchAnalyzer saveIndicatorBoxPlots(final String directory, final String baseName) {
      FileUtil.checkDirectory(directory);
      final SearchAnalyzerResults results = getSearchAnalysis();
      String fileName = baseName;
      if(fileName != null && !fileName.isEmpty()) {
         fileName += "_";
      } else {
         fileName = "";
      }
      for(final String indicator : results.getIndicators()) {
         SearchBoxPlot.saveIndicatorChart(indicator, results,
               FileUtil.createFile(directory, fileName + indicator + ".png"));
      }
      return this;
   }

   public SearchAnalyzer saveReferenceSet(final File file) throws IOException {
      final NondominatedPopulation refSet = getReferenceSet();
      if(refSet != null) {
         PopulationUtil.writeObjectives(file, refSet);
      }
      return this;
   }

   public SearchAnalyzer showAggregate() {
      this.showAggregate = true;
      return this;
   }

   public SearchAnalyzer showAll() {
      this.showAggregate = true;
      this.showStatisticalSignificance = true;
      this.showIndividualValues = true;
      return this;
   }

   public SearchAnalyzer showIndividualValues() {
      this.showIndividualValues = true;
      return this;
   }

   public SearchAnalyzer showStatistic(final UnivariateStatistic statistic) {
      if(statistic != null && !statistics.contains(statistic)) {
         statistics.add(statistic);
      }
      return this;
   }

   public SearchAnalyzer showStatisticalSignificance() {
      this.showStatisticalSignificance = true;
      return this;
   }

   public SearchAnalyzer usingProblemFactory(final ProblemFactory problemFactory) {
      this.problemFactory = problemFactory;
      return this;
   }

   public SearchAnalyzer withEpsilon(final double... epsilon) {
      this.epsilon = epsilon;
      return this;
   }

   public <S extends org.moeaframework.core.Solution> SearchAnalyzer withProblem(final ISearchProblem<S> problem) {
      if(problem != null) {
         this.problemClass = problem.getClass();
         this.problemArguments = new Object[] { problem.getFitnessFunction(), problem.getSolutionGenerator() };
      }
      return this;
   }

   public SearchAnalyzer withProblem(final Problem problem) {
      this.problem = problem;
      return this;
   }

   public SearchAnalyzer withProblem(final String problemName) {
      if(problemFactory != null) {
         this.problem = problemFactory.getProblem(problemName);
      }
      return this;
   }

   public SearchAnalyzer withProblemClass(final Class<?> problemClass, final Object... problemArguments) {
      this.problemClass = problemClass;
      this.problemArguments = problemArguments;
      return this;
   }

   public SearchAnalyzer withProblemClass(final String problemClassName, final Object... problemArguments)
         throws ClassNotFoundException {
      this.problemClass = Class.forName(problemClassName);
      this.problemArguments = problemArguments;
      return this;
   }

   public SearchAnalyzer withReferenceSet(final File referenceSetFile) {
      this.referenceSetFile = referenceSetFile;
      return this;
   }

   public SearchAnalyzer withReferenceSet(final NondominatedPopulation referenceSet) {
      this.referenceSet = referenceSet;
      return this;
   }

   public SearchAnalyzer withSameProblemAs(final SearchExecutor executor) {
      if(executor != null) {
         if(executor.getProblem() != null) {
            withProblem(executor.getProblem());
         } else if(executor.getProblemClass() != null) {
            withProblemClass(executor.getProblemClass(), executor.getProblemArguments());
         }
      }
      return this;
   }

   public SearchAnalyzer withSameProblemAs(final Instrumenter instrumenter) {
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

   public SearchAnalyzer withSameProblemAs(final SearchAnalyzer analyzer) {
      if(analyzer != null && analyzer.getProblem() != null) {
         withProblem(analyzer.getProblem());
      }
      return this;
   }

   public SearchAnalyzer withSignifianceLevel(final double significanceLevel) {
      return withSignificanceLevel(significanceLevel);
   }

   public SearchAnalyzer withSignificanceLevel(final double significanceLevel) {
      this.significanceLevel = significanceLevel;
      return this;
   }
}
