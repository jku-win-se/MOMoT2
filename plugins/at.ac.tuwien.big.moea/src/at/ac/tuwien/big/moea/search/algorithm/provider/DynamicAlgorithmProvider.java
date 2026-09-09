package at.ac.tuwien.big.moea.search.algorithm.provider;

import java.util.HashMap;
import java.util.Map;

import org.moeaframework.algorithm.Algorithm;
import org.moeaframework.core.TypedProperties;
import org.moeaframework.problem.Problem;
import org.moeaframework.core.spi.AlgorithmProvider;

public class DynamicAlgorithmProvider extends AlgorithmProvider {

   private static Map<String, IRegisteredAlgorithm<?>> registeredAlgorithms = new HashMap<>();

   public static void clear() {
      registeredAlgorithms.clear();
   }

   public static IRegisteredAlgorithm<?> getAlgorithm(final String name) {
      return registeredAlgorithms.get(name);
   }

   public static boolean registerAlgorithm(final String name, final IRegisteredAlgorithm<?> registeredAlgorithm) {
      if(registeredAlgorithms.containsKey(name)) {
         return false;
      }
      registeredAlgorithms.put(name, registeredAlgorithm);
      return true;
   }

   public static IRegisteredAlgorithm<?> removeAlgorithm(final String name) {
      return registeredAlgorithms.remove(name);
   }

   @Override
   public Algorithm getAlgorithm(final String name, final TypedProperties properties, final Problem problem) {
      final IRegisteredAlgorithm<?> registered = getAlgorithm(name);
      if(registered == null) {
         return null;
      }
      final Algorithm algorithm = registered.createAlgorithm();
      if(algorithm != null) {
         if(!algorithm.getProblem().getName().equals(problem.getName())) {
            System.err.println("Algorithm retrieved for wrong problem: " + algorithm.getProblem().getName() + " vs "
                  + problem.getName());
         }
      }
      return algorithm;
   }
}
