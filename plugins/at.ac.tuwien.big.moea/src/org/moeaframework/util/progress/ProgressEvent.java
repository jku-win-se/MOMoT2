package org.moeaframework.util.progress;

import java.io.Serializable;

public class ProgressEvent implements Serializable {
   private static final long serialVersionUID = 1L;

   private final Object executor;
   private final int currentSeed;
   private final int totalSeeds;
   private final int currentNFE;
   private final int maxNFE;
   private final double elapsedTime;
   private final double remainingTime;

   public ProgressEvent(final Object executor, final int currentSeed, final int totalSeeds, final int currentNFE,
         final int maxNFE, final double elapsedTime, final double remainingTime) {
      this.executor = executor;
      this.currentSeed = currentSeed;
      this.totalSeeds = totalSeeds;
      this.currentNFE = currentNFE;
      this.maxNFE = maxNFE;
      this.elapsedTime = elapsedTime;
      this.remainingTime = remainingTime;
   }

   public int getCurrentNFE() {
      return currentNFE;
   }

   public int getCurrentSeed() {
      return currentSeed;
   }

   public double getElapsedTime() {
      return elapsedTime;
   }

   public Object getExecutor() {
      return executor;
   }

   public int getMaxNFE() {
      return maxNFE;
   }

   public double getRemainingTime() {
      return remainingTime;
   }

   public int getTotalSeeds() {
      return totalSeeds;
   }
}
