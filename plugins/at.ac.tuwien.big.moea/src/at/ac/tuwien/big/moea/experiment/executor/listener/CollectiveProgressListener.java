package at.ac.tuwien.big.moea.experiment.executor.listener;

import at.ac.tuwien.big.moea.experiment.executor.SearchExecutor;
import at.ac.tuwien.big.moea.experiment.instrumenter.collector.SimpleBestSolutionCollector;
import at.ac.tuwien.big.moea.print.ISolutionWriter;
import at.ac.tuwien.big.moea.util.AccumulatorUtil;
import at.ac.tuwien.big.moea.util.TextUtil;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.moeaframework.core.Solution;
import org.moeaframework.util.progress.ProgressEvent;
import org.moeaframework.util.progress.ProgressListener;

public class CollectiveProgressListener extends AbstractProgressListener implements ProgressListener {

   public abstract class AbstractEventColumn implements IColumn {
      private final String header;
      private final ProgressEvent event;

      public AbstractEventColumn(final ProgressEvent event, final String header) {
         this.event = event;
         this.header = header;
      }

      public ProgressEvent getEvent() {
         return event;
      }

      @Override
      public String getHeader() {
         return header;
      }

      @Override
      public int getWidth() {
         return Math.max(MAX_WIDTH, getHeader().length());
      }
   }

   public abstract class AbstractSolutionColumn<S extends Solution> implements IColumn {
      protected ISolutionWriter<S> printer;
      protected S solution;
      protected int index;

      public AbstractSolutionColumn(final S solution, final int index, final ISolutionWriter<S> printer) {
         this.solution = solution;
         this.index = index;
         this.printer = printer;
      }

      @Override
      public int getWidth() {
         return Math.max(MAX_WIDTH, getHeader().length());
      }
   }

   public class AccumulatorColumn implements IColumn {
      private final SearchExecutor executor;
      private final String key;
      private final String header;

      public AccumulatorColumn(final SearchExecutor executor, final String key) {
         this(executor, key, key);
      }

      public AccumulatorColumn(final SearchExecutor executor, final String key, final String header) {
         this.executor = executor;
         this.key = key;
         this.header = header;
      }

      public SearchExecutor getExecutor() {
         return executor;
      }

      @Override
      public String getHeader() {
         if(hasData()) {
            return header;
         }
         return "";
      }

      public String getKey() {
         return key;
      }

      @Override
      public String getLatestData() {
         if(hasData()) {
            return getLatestDataString();
         }
         return "";
      }

      protected String getLatestDataString() {
         final Object data = AccumulatorUtil.getLatestAccumulatorData(getExecutor(), getKey());
         return data != null ? data.toString() : "";
      }

      @Override
      public int getWidth() {
         return Math.max(MAX_WIDTH, getHeader().length());
      }

      protected boolean hasData() {
         return AccumulatorUtil.hasAccumulatorData(getExecutor(), getKey());
      }
   }

   public interface IColumn {
      String getHeader();

      String getLatestData();

      int getWidth();
   }

   public class SolutionConstraintColumn<S extends Solution> extends AbstractSolutionColumn<S> {

      public SolutionConstraintColumn(final S solution, final int index, final ISolutionWriter<S> printer) {
         super(solution, index, printer);
      }

      @Override
      public String getHeader() {
         return printer.getConstraintName(index);
      }

      @Override
      public String getLatestData() {
         return printer.getConstraintData(solution, index);
      }
   }

   public class SolutionObjectiveColumn<S extends Solution> extends AbstractSolutionColumn<S> {

      public SolutionObjectiveColumn(final S solution, final int index, final ISolutionWriter<S> printer) {
         super(solution, index, printer);
      }

      @Override
      public String getHeader() {
         return printer.getObjectiveName(index);
      }

      @Override
      public String getLatestData() {
         return printer.getObjectiveData(solution, index);
      }
   }

   public class SolutionVariableColumn<S extends Solution> extends AbstractSolutionColumn<S> {

      public SolutionVariableColumn(final S solution, final int index, final ISolutionWriter<S> printer) {
         super(solution, index, printer);
      }

      @Override
      public String getHeader() {
         return printer.getVariableName(index);
      }

      @Override
      public String getLatestData() {
         return printer.getVariableData(solution, index);
      }
   }

   private static final int FITNESS_DECIMAL_PLACES = 10;

   private static final int FACTO = 100;

   private static final int DECIMAL_PLACES = 2;

   private static final int MAX_WIDTH = 20;

   private boolean firstCall = true;

   private final PrintStream stream;

   private final String separator;

   private ISolutionWriter<? extends Solution> printer;

   private boolean printObjectives = true;

   private boolean printConstraints = true;

   private boolean printSimpleAggregate = true;

   private boolean spaceColumns = false;

   public CollectiveProgressListener() {
      this(System.out, "\t");
   }

   public CollectiveProgressListener(final PrintStream stream) {
      this(stream, "\t");
   }

   public CollectiveProgressListener(final PrintStream stream, final String separator) {
      this.stream = stream;
      this.separator = separator;
   }

   public CollectiveProgressListener(final String separator) {
      this(System.out, separator);
   }

   private IColumn createAccumulatorColumn(final ProgressEvent event, final String key, final String header) {
      return new AccumulatorColumn((SearchExecutor) event.getExecutor(), key, header);
   }

   private IColumn createEvaluationColumn(final ProgressEvent event) {
      return new AbstractEventColumn(event, "Evaluation") {
         @Override
         public String getLatestData() {
            return getEvent().getCurrentNFE() + "/" + getEvent().getMaxNFE();
         }
      };
   }

   private IColumn createPercentCompleteColumn(final ProgressEvent event) {
      return new AbstractEventColumn(event, "%-Complete") {
         @Override
         public String getLatestData() {
            final double percent = getEvent().getMaxNFE() > 0 ? ((double) getEvent().getCurrentNFE() / getEvent().getMaxNFE()) * FACTO : 0.0;
            return TextUtil.toString(percent, DECIMAL_PLACES) + "%";
         }

         @Override
         public int getWidth() {
            return getHeader().length();
         }
      };
   }

   private IColumn createSeedColumn(final ProgressEvent event) {
      return new AbstractEventColumn(event, "Seed") {
         @Override
         public String getLatestData() {
            return getEvent().getCurrentSeed() + "/" + getEvent().getTotalSeeds();
         }

         @Override
         public int getWidth() {
            return getHeader().length();
         }
      };
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   private List<IColumn> createSolutionColumns(final ProgressEvent event) {
      final ArrayList<IColumn> columns = new ArrayList<>();
      if(getSolutionPrinter() == null) {
         return columns;
      }

      final SearchExecutor executor = event.getExecutor() instanceof SearchExecutor ? (SearchExecutor) event.getExecutor() : null;
      final Solution bestSolution = AccumulatorUtil.getLatestBestSolution(executor);
      if(bestSolution == null) {
         return columns;
      }

      if(printSimpleAggregate) {
         columns.add(new AccumulatorColumn(executor, AccumulatorUtil.Keys.SIMPLE_BEST_SOLUTION,
               "Aggregated Fitness") {
            @Override
            protected String getLatestDataString() {
               final Solution bestSolution = AccumulatorUtil.getLatestBestSolution(getExecutor());
               if(bestSolution == null) {
                  return "";
               }

               return TextUtil.toString(SimpleBestSolutionCollector.calculateAggregatedFitness(bestSolution),
                     FITNESS_DECIMAL_PLACES);
            }
         });
      }

      if(printObjectives) {
         for(int i = 0; i < bestSolution.getNumberOfObjectives(); i++) {
            columns.add(new SolutionObjectiveColumn(bestSolution, i, getSolutionPrinter()));
         }
      }

      if(printConstraints) {
         for(int i = 0; i < bestSolution.getNumberOfConstraints(); i++) {
            columns.add(new SolutionConstraintColumn(bestSolution, i, getSolutionPrinter()));
         }
      }
      return columns;
   }

   private IColumn createTimeColumn(final ProgressEvent event) {
      return new AbstractEventColumn(event, "Elapsed/Remaining") {
         @Override
         public String getLatestData() {
            return TextUtil.toString(getEvent().getElapsedTime(), 2) + "s/"
                  + TextUtil.toString(getEvent().getRemainingTime(), 2) + "s";
         }
      };
   }

   private List<IColumn> getColumns(final ProgressEvent event) {
      final List<IColumn> columns = new ArrayList<>();
      columns.add(createSeedColumn(event));
      columns.add(createPercentCompleteColumn(event));
      columns.add(createEvaluationColumn(event));
      columns.add(createTimeColumn(event));
      columns.addAll(createSolutionColumns(event));
      columns.add(createAccumulatorColumn(event, AccumulatorUtil.Keys.POPULATION_SIZE, "PopulationSize"));
      columns.add(createAccumulatorColumn(event, AccumulatorUtil.Keys.INDICATOR_ADDITIVE_EPSILON, "AdditiveEpsilon"));
      columns.add(createAccumulatorColumn(event, AccumulatorUtil.Keys.INDICATOR_CONTRIBUTION, "Contribution"));
      columns.add(createAccumulatorColumn(event, AccumulatorUtil.Keys.INDICATOR_GENERATIONAL_DISTANCE,
            "GenerationalDistance"));
      columns.add(createAccumulatorColumn(event, AccumulatorUtil.Keys.INDICATOR_HYPERVOLUME, "Hypervolume"));
      columns.add(createAccumulatorColumn(event, AccumulatorUtil.Keys.INDICATOR_INVERTED_GENERATIONAL_DISTANCE,
            "InvertedGenerationalDistance"));
      columns.add(createAccumulatorColumn(event, AccumulatorUtil.Keys.INDICATOR_SPACING, "Spacing"));
      columns.add(createAccumulatorColumn(event, AccumulatorUtil.Keys.ARCHIVE_SIZE, "ArchiveSize"));
      columns.add(createAccumulatorColumn(event, AccumulatorUtil.Keys.NUMBER_OF_DOMINATING_IMPROVEMENTS,
            "#DominatingImprovements"));
      columns.add(createAccumulatorColumn(event, AccumulatorUtil.Keys.NUMBER_OF_IMPROVEMENTS, "#Improvements"));
      columns.add(createAccumulatorColumn(event, AccumulatorUtil.Keys.NUMBER_OF_RESTARTS, "#Restarts"));
      return columns;
   }

   public PrintStream getPrintStream() {
      return stream;
   }

   public String getSeparator() {
      return separator;
   }

   public ISolutionWriter<? extends Solution> getSolutionPrinter() {
      return printer;
   }

   private void printData(final List<IColumn> columns) {
      String separator = "";
      for(final IColumn column : columns) {
         final String latestData = column.getLatestData();
         if(!latestData.isEmpty()) {
            getPrintStream().print(separator);
            if(spaceColumns) {
               getPrintStream().printf("%-" + column.getWidth() + "s", latestData);
            } else {
               getPrintStream().print(latestData);
            }
            separator = getSeparator();
         }
      }
      getPrintStream().println();
      getPrintStream().flush();
   }

   private void printHeader(final List<IColumn> columns) {
      if(!firstCall) {
         return;
      }

      String separator = "";
      for(final IColumn column : columns) {
         final String header = column.getHeader();
         if(!header.isEmpty()) {
            getPrintStream().print(separator);
            getPrintStream().printf("%-" + column.getWidth() + "s", header);
            separator = getSeparator();
         }
      }
      getPrintStream().println();
      getPrintStream().flush();
      firstCall = false;
   }

   public CollectiveProgressListener setSolutionPrinter(final ISolutionWriter<? extends Solution> printer) {
      return setSolutionPrinter(printer, true, true, true);
   }

   public CollectiveProgressListener setSolutionPrinter(final ISolutionWriter<? extends Solution> printer,
         final boolean printSimpleAggregate, final boolean printObjectives, final boolean printConstraints) {
      this.printer = printer;
      this.printSimpleAggregate = printSimpleAggregate;
      this.printObjectives = printObjectives;
      this.printConstraints = printConstraints;
      return this;
   }

   public CollectiveProgressListener setSpaceColumns(final boolean spaceColumns) {
      this.spaceColumns = spaceColumns;
      return this;
   }

   @Override
   public void update(final ProgressEvent event) {
      final List<IColumn> columns = getColumns(event);
      printHeader(columns);
      printData(columns);
   }
}
