package at.ac.tuwien.big.moea.search.algorithm.provider;

import org.moeaframework.algorithm.Algorithm;
import org.moeaframework.core.TypedProperties;
import org.moeaframework.problem.Problem;
import org.moeaframework.core.spi.AlgorithmFactory;

public class DynamicAlgorithmFactory extends AlgorithmFactory {

   private final DynamicAlgorithmProvider dynamicProvider = new DynamicAlgorithmProvider();

   @Override
   public synchronized Algorithm getAlgorithm(final String name, final TypedProperties properties, final Problem problem) {
      final Algorithm algorithm = dynamicProvider.getAlgorithm(name, properties, problem);
      if(algorithm != null) {
         return algorithm;
      }
      return super.getAlgorithm(name, properties, problem);
   }
}
