package at.ac.tuwien.big.moea.experiment.executor.listener;

import at.ac.tuwien.big.moea.experiment.executor.SearchExecutor;
import at.ac.tuwien.big.moea.experiment.instrumenter.SearchInstrumenter;

import java.util.ArrayList;
import java.util.List;

import org.moeaframework.analysis.collector.Accumulator;
import org.moeaframework.util.progress.ProgressEvent;
import org.moeaframework.util.progress.ProgressListener;

public class AccumulatorProgressListener extends AbstractProgressListener implements ProgressListener {

   private final List<Accumulator> accumulators = new ArrayList<>();

   public List<Accumulator> getAccumulators() {
      return accumulators;
   }

   public AccumulatorProgressListener reset() {
      accumulators.clear();
      return this;
   }

   @Override
   public void update(final ProgressEvent event) {
      if(isSeedFinished(event) || isFinished(event)) {
         if(event.getExecutor() instanceof SearchExecutor) {
            final SearchExecutor executor = (SearchExecutor) event.getExecutor();
            if(executor.getInstrumenter() instanceof SearchInstrumenter) {
               final SearchInstrumenter instrumenter = (SearchInstrumenter) executor.getInstrumenter();
               accumulators.add(instrumenter.getLastAccumulator());
            }
         }
      }
   }
}
