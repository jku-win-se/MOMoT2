package at.ac.tuwien.big.moea.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.moeaframework.core.Solution;
import org.moeaframework.core.population.NondominatedPopulation;

public final class PopulationUtil {

   private PopulationUtil() {}

   public static NondominatedPopulation readObjectives(final File file) throws IOException {
      final NondominatedPopulation population = new NondominatedPopulation();
      if(file == null || !file.exists()) {
         return population;
      }
      try(BufferedReader reader = new BufferedReader(new FileReader(file))) {
         String line;
         while((line = reader.readLine()) != null) {
            line = line.trim();
            if(line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
               continue;
            }
            final String[] tokens = line.split("\\s+");
            final double[] objectives = new double[tokens.length];
            for(int i = 0; i < tokens.length; i++) {
               objectives[i] = Double.parseDouble(tokens[i]);
            }
            final Solution solution = new Solution(0, objectives.length);
            solution.setObjectiveValues(objectives);
            population.add(solution);
         }
      }
      return population;
   }

   public static void writeObjectives(final File file, final Iterable<Solution> population) throws IOException {
      if(file.getParentFile() != null) {
         file.getParentFile().mkdirs();
      }
      try(PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file)))) {
         for(final Solution solution : population) {
            for(int i = 0; i < solution.getNumberOfObjectives(); i++) {
               if(i > 0) {
                  writer.print(" ");
               }
               writer.print(solution.getObjectiveValue(i));
            }
            writer.println();
         }
      }
   }
}
