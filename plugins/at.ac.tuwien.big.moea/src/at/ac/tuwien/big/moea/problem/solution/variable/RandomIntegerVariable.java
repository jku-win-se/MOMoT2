package at.ac.tuwien.big.moea.problem.solution.variable;

import at.ac.tuwien.big.moea.util.TextUtil;
import at.ac.tuwien.big.moea.util.random.RandomInteger;

import org.moeaframework.core.variable.Variable;
import org.moeaframework.core.variable.RealVariable;

public class RandomIntegerVariable implements Variable {

   private static final long serialVersionUID = -8144298676316291939L;

   private final RandomInteger randomInteger;
   private Integer value;
   private String name;

   public RandomIntegerVariable(final int lowerBound, final int upperBound) {
      randomInteger = new RandomInteger(lowerBound, upperBound);
      randomize();
   }

   public RandomIntegerVariable(final int value, final int lowerBound, final int upperBound) {
      randomInteger = new RandomInteger(lowerBound, upperBound);
      setValue(value);
   }

   @Override
   public RandomIntegerVariable copy() {
      final RandomIntegerVariable copy = new RandomIntegerVariable(getValue(), randomInteger.getLowerBound(), randomInteger.getUpperBound());
      copy.setName(getName());
      return copy;
   }

   @Override
   public boolean equals(final Object obj) {
      if(this == obj) {
         return true;
      }
      if(obj == null) {
         return false;
      }
      if(getClass() != obj.getClass()) {
         return false;
      }
      final RandomIntegerVariable other = (RandomIntegerVariable) obj;
      if(value == null) {
         if(other.value != null) {
            return false;
         }
      } else if(!value.equals(other.value)) {
         return false;
      }
      return true;
   }

   public int getLowerBound() {
      return randomInteger.getLowerBound();
   }

   public int getUpperBound() {
      return randomInteger.getUpperBound();
   }

   public int getValue() {
      return value;
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
      return "int[" + getLowerBound() + ", " + getUpperBound() + "]";
   }

   @Override
   public String encode() {
      return String.valueOf(getValue());
   }

   @Override
   public void decode(final String value) {
      setValue(Integer.parseInt(value));
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (value == null ? 0 : value.hashCode());
      return result;
   }

   @Override
   public void randomize() {
      this.value = randomInteger.nextRandom();
   }

   public void setValue(final int value) {
      if(value < getLowerBound() || value > getUpperBound()) {
         throw new IllegalArgumentException("Value must be between lower and upper bound.");
      }
      this.value = value;
   }

   public String toRangeString() {
      return TextUtil.toRangeString(this);
   }

   public RealVariable toRealVariable() {
      final RealVariable var = new RealVariable(randomInteger.getLowerBound(), randomInteger.getUpperBound() - 1);
      var.setValue(getValue());
      return var;
   }

   @Override
   public String toString() {
      return "" + getValue();
   }
}
