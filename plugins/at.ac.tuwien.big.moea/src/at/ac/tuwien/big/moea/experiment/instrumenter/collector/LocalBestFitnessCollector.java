package at.ac.tuwien.big.moea.experiment.instrumenter.collector;

import at.ac.tuwien.big.moea.search.algorithm.local.LocalSearchAlgorithm;
import at.ac.tuwien.big.moea.util.AccumulatorUtil;

import java.io.Serializable;

import org.moeaframework.analysis.runtime.AttachPoint;
import org.moeaframework.analysis.runtime.Collector;
import org.moeaframework.analysis.series.ResultEntry;

public class LocalBestFitnessCollector implements Collector {

   private final LocalSearchAlgorithm<?> algorithm;

   public LocalBestFitnessCollector() {
      this(null);
   }

   public LocalBestFitnessCollector(final LocalSearchAlgorithm<?> algorithm) {
      this.algorithm = algorithm;
   }

   @Override
   public Collector attach(final Object object) {
      return new LocalBestFitnessCollector((LocalSearchAlgorithm<?>) object);
   }

   @Override
   public void collect(final ResultEntry entry) {
      if(algorithm == null) {
         return;
      }
      final Serializable bestFitness = (Serializable) algorithm.getBestFitness();
      if(bestFitness != null) {
         entry.getProperties().setString(AccumulatorUtil.Keys.LOCAL_BEST_FITNESS, bestFitness.toString());
      }
   }

   @Override
   public AttachPoint getAttachPoint() {
      return AttachPoint.isSubclass(LocalSearchAlgorithm.class)
            .and(AttachPoint.not(AttachPoint.isNestedIn(LocalSearchAlgorithm.class)));
   }
}
