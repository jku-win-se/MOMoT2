package at.ac.tuwien.big.moea.experiment.analyzer.kneepoint;

import at.ac.tuwien.big.moea.util.MathUtil;

import java.util.Arrays;
import java.util.Comparator;

import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.core.Solution;
import org.moeaframework.core.comparator.LexicographicalComparator;

/**
 * This class is adapted from the Fitness calculation class of
 * jMetalPlus: An enhanced version of the jMetal framework by marlonso
 * https://sourceforge.net/projects/jmetalbymarlonso/
 */
public class Fitness {
   private static final double BULGE_FITNESS_EPS = 10E-6;

   private static final double ANGLE_270 = 270.0;

   public static final String KEY_PROPER_UTILITY = "momot_proper_utility";

   public static final String KEY_FITNESS = "momot_fitness";

   public static final String KEY_PROPER_UTILITY_FITNESS = "momot_proper_utility_fitness";
   public static final String KEY_MINMAX_TRADEOFF_FITNESS = "momot_minmaxTradeoff_fitness";
   public static final String KEY_UTILITY_KNEE_FITNESS_2D = "momot_utilityKneeFitness2D";
   public static final String KEY_UTILITY_KNEE_FITNESS_HIGHER_DIMENSIONS = "momot_utilityKneeFitness2D";
   public static final String KEY_BEND_ANGLE_FITNESS = "momot_bendAngleFitness";
   public static final String KEY_OBJECTIVE_SUM_FITNESS = "momot_objectiveSumFitness";
   public static final String KEY_ANGLE_FITNESS = "momot_angleFitness";
   public static final String KEY_EGALITARIAN_FITNESS = "momot_egalitarian_fitness";
   public static final String KEY_MAXMIN_FITNESS = "momot_maxmin_fitness";
   public static final String KEY_CHEBYSHEV_FITNESS = "momot_chebyshev_fitness";
   public static final String KEY_BULGE_FITNESS = "momot_bulge_fitness";
   public static final String KEY_NASH_FITNESS = "momot_nash_fitness";
   public static final String KEY_COBB_DOUGLAS_FITNESS = "momot_cobb_douglas_fitness";
   public static final String KEY_WEIGHTED_SUM_FITNESS = "momot_weighted_sum_fitness";
   public static final String KEY_PNORM_FITNESS = "momot_pnorm_fitness";

   public static void assignFitness(final NondominatedPopulation population) {
      final Fitness fitness = new Fitness(population);
      fitness.objectiveSumFitness();
      fitness.egalitarianFitness();
      fitness.chebyshevFitness();
      fitness.cobbDouglasFitness();
      fitness.nashFitness();
      fitness.properUtility();
      fitness.pNormFitness(2.0);
   }

   public static void assignFitness(final NondominatedPopulation population, final double[] weights) {
      assignFitness(population);
      final Fitness fitness = new Fitness(population);
      fitness.weightedSumFitness(weights);
      fitness.maxMinFitness(weights);
      fitness.chebyshevFitness2(weights);
      fitness.cobbDouglasFitness(weights);
   }

   public static void assignFitness(final NondominatedPopulation population, final double[] weights,
         final double[] referencePoint) {
      assignFitness(population, weights);
      final Fitness fitness = new Fitness(population);
      fitness.chebyshevFitness(referencePoint, weights);
      fitness.nashFitness(referencePoint);
   }

   private final NondominatedPopulation nondominatedPopulation;

   private int nObj;

   public Fitness(final NondominatedPopulation nondominatedPopulation) {
      this.nondominatedPopulation = nondominatedPopulation;
      if(!nondominatedPopulation.isEmpty()) {
         this.nObj = nondominatedPopulation.get(0).getNumberOfObjectives();
      }
   }

   public void angleFitness(final double[][] extremePoints) {
      final int nExt = extremePoints.length;

      for(final Solution s : nondominatedPopulation) {
         final double[] angles = new double[nExt];
         final double[][] difference = new double[nExt][nExt];
         for(int i = 0; i < nExt; i++) {
            for(int j = 0; j < nExt; j++) {
               difference[i][j] = extremePoints[i][j] - s.getObjectiveValue(j);
            }
            double myNumerator = 0;
            double myDenominator = 0;
            for(int j = 0; j < nExt; j++) {
               if(i != j) {
                  myNumerator = myNumerator + Math.pow(difference[i][j], 2);
               } else {
                  myDenominator = difference[i][j];
               }
            }
            try {
               angles[i] = Math.atan(Math.sqrt(myNumerator) / myDenominator);
               if(Double.isInfinite(angles[i])) {
                  angles[i] = Double.MAX_VALUE;
               }
            } catch(final ArithmeticException e) {
               angles[i] = Double.MAX_VALUE;
            }
         }
         setFitness(s, MathUtil.max(angles), KEY_ANGLE_FITNESS);
      }
   }

   private double bendAngle(final Solution leftNeighbor, final Solution solution, final Solution rightNeighbor) {
      return ANGLE_270 - getLeftBendAngle(solution, leftNeighbor) - getRightBendAngle(solution, rightNeighbor);
   }

   public void bendAngleFitness() {
      if(nondominatedPopulation.isEmpty()) {
         return;
      }
      if(nObj > 2) {
         throw new RuntimeException("Bend-angle can only be calculated for two-dimensional problems");
      }

      nondominatedPopulation.sort(new LexicographicalComparator());

      final int size = nondominatedPopulation.size();
      final double[] alpha = new double[size];
      final double[] beta = new double[size];
      final double[] gamma = new double[size];
      final double[] delta = new double[size];

      alpha[0] = ANGLE_270 - getRightBendAngle(nondominatedPopulation.get(0), nondominatedPopulation.get(1));
      beta[0] = alpha[0];
      gamma[0] = ANGLE_270 - getRightBendAngle(nondominatedPopulation.get(0), nondominatedPopulation.get(2));
      delta[0] = gamma[0];
      setFitness(nondominatedPopulation.get(0), Math.max(alpha[0], gamma[0]), KEY_BEND_ANGLE_FITNESS);

      alpha[1] = bendAngle(nondominatedPopulation.get(0), nondominatedPopulation.get(1), nondominatedPopulation.get(2));
      beta[1] = alpha[1];
      gamma[1] = bendAngle(nondominatedPopulation.get(0), nondominatedPopulation.get(1), nondominatedPopulation.get(3));
      delta[1] = gamma[1];
      setFitness(nondominatedPopulation.get(1), Math.max(alpha[1], gamma[1]), KEY_BEND_ANGLE_FITNESS);

      alpha[size - 2] = bendAngle(nondominatedPopulation.get(size - 3), nondominatedPopulation.get(size - 2),
            nondominatedPopulation.get(size - 1));
      beta[size - 2] = bendAngle(nondominatedPopulation.get(size - 4), nondominatedPopulation.get(size - 2),
            nondominatedPopulation.get(size - 1));
      gamma[size - 2] = alpha[size - 2];
      delta[size - 2] = beta[size - 2];
      setFitness(nondominatedPopulation.get(size - 2), Math.max(alpha[size - 2], beta[size - 2]),
            KEY_BEND_ANGLE_FITNESS);

      alpha[size - 1] = ANGLE_270
            - getLeftBendAngle(nondominatedPopulation.get(size - 1), nondominatedPopulation.get(size - 2));
      beta[size - 1] = ANGLE_270
            - getLeftBendAngle(nondominatedPopulation.get(size - 1), nondominatedPopulation.get(size - 3));
      gamma[size - 1] = alpha[size - 1];
      delta[size - 1] = beta[size - 1];

      setFitness(nondominatedPopulation.get(size - 1), Math.max(alpha[size - 1], beta[size - 1]),
            KEY_BEND_ANGLE_FITNESS);

      for(int i = 2; i < size - 2; i++) {
         alpha[i] = bendAngle(nondominatedPopulation.get(i - 1), nondominatedPopulation.get(i),
               nondominatedPopulation.get(i + 1));
         beta[i] = bendAngle(nondominatedPopulation.get(i - 2), nondominatedPopulation.get(i),
               nondominatedPopulation.get(i + 1));
         gamma[i] = bendAngle(nondominatedPopulation.get(i - 1), nondominatedPopulation.get(i),
               nondominatedPopulation.get(i + 2));
         delta[i] = bendAngle(nondominatedPopulation.get(i - 2), nondominatedPopulation.get(i),
               nondominatedPopulation.get(i + 2));

         setFitness(nondominatedPopulation.get(i), MathUtil.max(alpha[i], beta[i], gamma[i], delta[i]),
               KEY_BEND_ANGLE_FITNESS);
      }
   }

   public void bulgeFitness() {
      final Solution[] chim = obtainCHIM();

      final double[] minima = new double[nObj];
      final double[] maxima = new double[nObj];

      for(int obj = 0; obj < nObj; obj++) {
         minima[obj] = Double.MAX_VALUE;
         maxima[obj] = Double.MIN_VALUE;

         for(int ext = 0; ext < nObj; ext++) {
            maxima[obj] = Math.max(maxima[obj], chim[ext].getObjectiveValue(obj));
            minima[obj] = Math.min(minima[obj], chim[ext].getObjectiveValue(obj));
         }
      }

      bulgeFitness(minima, maxima);
   }

   public void bulgeFitness(final double[] minima, final double[] maxima) {
      for(int curSol = 0; curSol < nondominatedPopulation.size(); curSol++) {
         double sum = 0.0;
         for(int obj = 0; obj < nObj; obj++) {
            if(Math.abs(minima[obj] - maxima[obj]) < BULGE_FITNESS_EPS) {
               sum += (nondominatedPopulation.get(curSol).getObjectiveValue(obj) - minima[obj])
                     / (maxima[obj] - minima[obj]);
            }
         }
         final double fitness = (-1 + sum) / Math.sqrt(nObj);
         setFitness(nondominatedPopulation.get(curSol), fitness, KEY_BULGE_FITNESS);
      }
   }

   public void chebyshevFitness() {
      chebyshevFitness(MathUtil.colMin(writeObjectivesToMatrix(nondominatedPopulation)));
   }

   public void chebyshevFitness(final double[] aspirationPoint) {
      for(final Solution s : nondominatedPopulation) {
         setFitness(s, MathUtil.max(MathUtil.subtract(s.getObjectiveValues(), aspirationPoint)), KEY_CHEBYSHEV_FITNESS);
      }
   }

   public void chebyshevFitness(final double[] aspirationPoint, final double[] weights) {
      for(final Solution s : nondominatedPopulation) {
         setFitness(s, MathUtil.max(MathUtil.multiply(weights, MathUtil.subtract(s.getObjectiveValues(), aspirationPoint))),
               KEY_CHEBYSHEV_FITNESS);
      }
   }

   public void chebyshevFitness2(final double[] weights) {
      chebyshevFitness(MathUtil.colMin(writeObjectivesToMatrix(nondominatedPopulation)), weights);
   }

   public void cobbDouglasFitness() {
      for(final Solution s : nondominatedPopulation) {
         double product = s.getObjectiveValue(0);
         for(int o = 1; o < nObj; o++) {
            product *= s.getObjectiveValue(o);
         }
         setFitness(s, product, KEY_COBB_DOUGLAS_FITNESS);
      }
   }

   public void cobbDouglasFitness(final double[] weights) {
      for(final Solution s : nondominatedPopulation) {
         double product = 1.0;
         for(int o = 0; o < nObj; o++) {
            product *= Math.pow(s.getObjectiveValue(o), weights[o]);
         }
         setFitness(s, product, KEY_COBB_DOUGLAS_FITNESS);
      }
   }

   public void convergenceDiversityRanking(final Comparator<Solution> convergence,
         final Comparator<Solution> diversity) {
      convergenceDiversityRanking(convergence, diversity, 1.0, 1.0);
   }

   public void convergenceDiversityRanking(final Comparator<Solution> convergence, final Comparator<Solution> diversity,
         final double c, final double d) {
      nondominatedPopulation.sort(convergence);
      for(int rank = 0; rank < nondominatedPopulation.size(); rank++) {
         nondominatedPopulation.get(rank).setAttribute("momot_convergence_distance_rank", c * rank);
      }

      nondominatedPopulation.sort(diversity);
      for(int rank = 0; rank < nondominatedPopulation.size(); rank++) {
         nondominatedPopulation.get(rank).setAttribute("momot_convergence_distance_rank",
               (Double) nondominatedPopulation.get(rank).getAttribute("momot_convergence_distance_rank") + d * rank);
      }
   }

   public void egalitarianFitness() {
      for(final Solution s : nondominatedPopulation) {
         setFitness(s, MathUtil.max(s.getObjectiveValues()), KEY_EGALITARIAN_FITNESS);
      }
   }

   private double expectedMarginalUtility(final int i) {
      return marginalUtilityKneeIntegralFunction(i, getWeightForEqualUtility(i - 1, i + 1))
            - marginalUtilityKneeIntegralFunction(i, getWeightForEqualUtility(i - 1, i))
            + marginalUtilityKneeIntegralFunction(i, getWeightForEqualUtility(i, i + 1))
            - marginalUtilityKneeIntegralFunction(i, getWeightForEqualUtility(i - 1, i + 1));
   }

   private double[] getBestObjectives() {
      final double[] best = new double[nObj];
      for(int i = 0; i < nObj; i++) {
         best[i] = Double.POSITIVE_INFINITY;
      }

      for(final Solution solution : nondominatedPopulation) {
         for(int i = 0; i < nObj; i++) {
            if(solution.getObjectiveValue(i) < best[i]) {
               best[i] = solution.getObjectiveValue(i);
            }
         }
      }
      return best;
   }

   public Double getFitness(final Solution solution) {
      return (Double) solution.getAttribute(KEY_FITNESS);
   }

   private double getLeftBendAngle(final Solution solution, final Solution leftNeighbor) {
      return Math.toDegrees(Math.atan((solution.getObjectiveValue(0) - leftNeighbor.getObjectiveValue(0))
            / (leftNeighbor.getObjectiveValue(1) - solution.getObjectiveValue(1))));
   }

   public NondominatedPopulation getNondominatedPopulation() {
      return nondominatedPopulation;
   }

   public Double getProperUtility(final Solution solution) {
      return (Double) solution.getAttribute(KEY_PROPER_UTILITY);
   }

   private double getRightBendAngle(final Solution solution, final Solution rightNeighbor) {
      return Math.toDegrees(Math.atan((solution.getObjectiveValue(1) - rightNeighbor.getObjectiveValue(1))
            / (rightNeighbor.getObjectiveValue(0) - solution.getObjectiveValue(0))));
   }

   private double getWeightForEqualUtility(final int i, final int j) {
      final Solution sol1 = nondominatedPopulation.get(i);
      final Solution sol2 = nondominatedPopulation.get(j);

      if(sol1.getNumberOfObjectives() != sol2.getNumberOfObjectives()) {
         throw new RuntimeException("Solutions do not have the same number of objectives");
      }

      return (sol2.getObjectiveValue(1) - sol1.getObjectiveValue(1))
            / (sol1.getObjectiveValue(0) - sol2.getObjectiveValue(0) + sol2.getObjectiveValue(1) - sol1.getObjectiveValue(1));
   }

   public double[] getWorstObjectives() {
      final double[] worst = new double[nObj];
      for(int i = 0; i < nObj; i++) {
         worst[i] = Double.NEGATIVE_INFINITY;
      }

      for(final Solution solution : nondominatedPopulation) {
         for(int i = 0; i < nObj; i++) {
            if(solution.getObjectiveValue(i) > worst[i]) {
               worst[i] = solution.getObjectiveValue(i);
            }
         }
      }
      return worst;
   }

   private double marginalUtilityKneeIntegralFunction(final int i, final double alpha) {
      final double f1x = nondominatedPopulation.get(i).getObjectiveValue(0);
      final double f2x = nondominatedPopulation.get(i).getObjectiveValue(1);
      final double f1y = nondominatedPopulation.get(i - 1).getObjectiveValue(0);
      final double f2y = nondominatedPopulation.get(i - 1).getObjectiveValue(1);

      final double alphaSquare = 1.0 / 2.0 * Math.pow(alpha, 2);

      return alphaSquare * (f1x - f1y) + (1 - alphaSquare) * (f2x - f2y);
   }

   public void maxMinFitness(final double[] weights) {
      for(final Solution s : nondominatedPopulation) {
         setFitness(s, MathUtil.max(MathUtil.multiply(weights, s.getObjectiveValues())), KEY_MAXMIN_FITNESS);
      }
   }

   public void minmaxTradeoffFitness() {
      final double[] best = getBestObjectives();
      final double[] worst = getWorstObjectives();

      for(final Solution solution : nondominatedPopulation) {
         final double[] objectives = solution.getObjectiveValues();
         setFitness(solution,
               MathUtil.max(MathUtil.divide(MathUtil.subtract(objectives, best), MathUtil.subtract(worst, objectives))),
               KEY_MINMAX_TRADEOFF_FITNESS);
      }
   }

   public void nashFitness() {
      nashFitness(MathUtil.colMax(writeObjectivesToMatrix(nondominatedPopulation)));
   }

   public void nashFitness(final double[] disagreementPoint) {
      for(final Solution s : nondominatedPopulation) {
         double fitness = 1.0;
         for(int o = 0; o < nObj; o++) {
            fitness *= disagreementPoint[o] - s.getObjectiveValue(o);
         }
         setFitness(s, fitness, KEY_NASH_FITNESS);
      }
   }

   public void objectiveSumFitness() {
      for(final Solution s : nondominatedPopulation) {
         double value = 0.0;
         for(int i = 0; i < s.getNumberOfObjectives(); i++) {
            value += s.getObjectiveValue(i);
         }
         setFitness(s, value, KEY_OBJECTIVE_SUM_FITNESS);
      }
   }

   public Solution[] obtainCHIM() {
      final Solution[] chim = new Solution[nObj];

      final LexicographicalComparator comparator = new LexicographicalComparator();
      for(int obj = 0; obj < nObj; obj++) {
         Solution bestKnown = nondominatedPopulation.get(0);
         Solution candidateSolution;
         int flag;
         for(int i = 1; i < nondominatedPopulation.size(); i++) {
            candidateSolution = nondominatedPopulation.get(i);
            flag = comparator.compare(bestKnown, candidateSolution);
            if(flag == +1) {
               bestKnown = candidateSolution;
            }
         }
         chim[obj] = bestKnown;
      }

      return chim;
   }

   public void pNormFitness(final double norm) {
      pNormFitness(getBestObjectives(), norm);
   }

   public void pNormFitness(final double[] utopiaPoint, final double norm) {
      for(final Solution s : nondominatedPopulation) {
         setFitness(s, MathUtil.pNorm(MathUtil.subtract(utopiaPoint, s.getObjectiveValues()), norm), KEY_PNORM_FITNESS);
      }
   }

   public void properUtility() {
      for(final Solution solution : nondominatedPopulation) {
         setProperUtility(solution, 0.0);
      }

      for(int cur = 0; cur < nondominatedPopulation.size(); cur++) {
         final Solution current = nondominatedPopulation.get(cur);

         for(int oth = cur + 1; oth < nondominatedPopulation.size(); oth++) {
            final Solution other = nondominatedPopulation.get(oth);

            final double[] diff = MathUtil.subtract(nondominatedPopulation.get(cur).getObjectiveValues(),
                  nondominatedPopulation.get(oth).getObjectiveValues());
            final double max = MathUtil.max(diff);
            final double min = MathUtil.min(diff);

            if(max > 0 && min < 0) {
               setProperUtility(current, Math.max(getProperUtility(current), -max / min));
               setProperUtility(other, Math.max(getProperUtility(other), -min / max));
               setFitness(current, getProperUtility(current), KEY_PROPER_UTILITY_FITNESS);
               setFitness(other, getProperUtility(other), KEY_PROPER_UTILITY_FITNESS);
            }
         }
      }
   }

   public void properUtility(final double tradeoff) {
      properUtility();
      for(final Solution sol : nondominatedPopulation) {
         setProperUtility(sol, Math.max(getProperUtility(sol), tradeoff));
         setFitness(sol, getProperUtility(sol), KEY_PROPER_UTILITY_FITNESS);
      }
   }

   public void setFitness(final Solution solution, final Double fitness) {
      solution.setAttribute(KEY_FITNESS, fitness);
   }

   public void setFitness(final Solution solution, final Double fitness, final String key) {
      setFitness(solution, fitness);
      solution.setAttribute(key, fitness);
   }

   public void setProperUtility(final Solution solution, final Double fitness) {
      solution.setAttribute(KEY_PROPER_UTILITY, fitness);
   }

   public void utilityKneeFitness() {
      if(nondominatedPopulation.isEmpty()) {
         return;
      }

      if(nObj == 2) {
         utilityKneeFitness2D();
      }
   }

   private void utilityKneeFitness2D() {
      nondominatedPopulation.sort(new LexicographicalComparator());

      setFitness(nondominatedPopulation.get(0), Double.MAX_VALUE, KEY_UTILITY_KNEE_FITNESS_2D);
      setFitness(nondominatedPopulation.get(nondominatedPopulation.size() - 1), Double.MAX_VALUE,
            KEY_UTILITY_KNEE_FITNESS_2D);

      for(int i = 1; i < nondominatedPopulation.size() - 1; i++) {
         final Solution current = nondominatedPopulation.get(i);
         setFitness(current, expectedMarginalUtility(i), KEY_UTILITY_KNEE_FITNESS_2D);
      }
   }

   public void utilityKneeFitnessHigherDimensions(final double[][] lambda) {
      for(int i = 0; i < nondominatedPopulation.size(); i++) {
         setFitness(nondominatedPopulation.get(i), 0.0);
      }

      for(final double[] element : lambda) {
         final double[] fitness = new double[nondominatedPopulation.size()];
         double minFitness = Double.MAX_VALUE;
         int indexMinFitness = 0;
         for(int j = 0; j < nondominatedPopulation.size(); j++) {
            fitness[j] = 0.0;
            for(int k = 0; k < element.length; k++) {
               fitness[j] += nondominatedPopulation.get(j).getObjectiveValue(k) * element[k];
            }
            if(fitness[j] < minFitness) {
               minFitness = fitness[j];
               indexMinFitness = j;
            }
         }
         Arrays.sort(fitness);
         final Solution update = nondominatedPopulation.get(indexMinFitness);
         setFitness(update, getFitness(update) + fitness[1] - minFitness, KEY_UTILITY_KNEE_FITNESS_HIGHER_DIMENSIONS);
      }
   }

   public void weightedSumFitness(final double[] weights) {
      for(final Solution s : nondominatedPopulation) {
         setFitness(s, MathUtil.sum(MathUtil.multiply(s.getObjectiveValues(), weights)), KEY_WEIGHTED_SUM_FITNESS);
      }
   }

   public double[] writeFitnessToArray(final NondominatedPopulation population) {
      final double[] fitness = new double[population.size()];
      int index = 0;
      for(final Solution s : population) {
         fitness[index++] = getFitness(s);
      }
      return fitness;
   }

   public double[][] writeObjectivesToMatrix(final NondominatedPopulation population) {
      if(population.size() == 0) {
         return null;
      }
      double[][] objectives;
      objectives = new double[population.size()][population.get(0).getNumberOfObjectives()];
      for(int i = 0; i < population.size(); i++) {
         for(int j = 0; j < population.get(0).getNumberOfObjectives(); j++) {
            objectives[i][j] = population.get(i).getObjectiveValue(j);
         }
      }
      return objectives;
   }
}
