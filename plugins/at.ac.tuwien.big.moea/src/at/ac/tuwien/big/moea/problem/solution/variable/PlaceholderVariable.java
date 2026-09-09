package at.ac.tuwien.big.moea.problem.solution.variable;

import org.moeaframework.core.variable.Variable;

public class PlaceholderVariable implements IPlaceholderVariable {
   private static final long serialVersionUID = -3707613316442716902L;

   private String name;

   @Override
   public Variable copy() {
      final PlaceholderVariable copy = new PlaceholderVariable();
      copy.setName(getName());
      return copy;
   }

   @Override
   public String getName() {
      return name;
   }

   public void setName(final String name) {
      this.name = name;
   }

   @Override
   public String getDefinition() {
      return "Placeholder";
   }

   @Override
   public String encode() {
      return "";
   }

   @Override
   public void decode(final String value) {}

   @Override
   public void randomize() {}

   @Override
   public String toString() {
      return "-Placeholder-\n";
   }
}
