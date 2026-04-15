package at.ac.tuwien.big.momot.core.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import at.ac.tuwien.big.momot.ModuleManager;
import at.ac.tuwien.big.momot.problem.unit.parameter.fix.FixValue;

import org.eclipse.emf.henshin.interpreter.Assignment;
import org.eclipse.emf.henshin.interpreter.InterpreterFactory;
import org.eclipse.emf.henshin.model.HenshinFactory;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Parameter;
import org.eclipse.emf.henshin.model.ParameterKind;
import org.eclipse.emf.henshin.model.Rule;
import org.junit.Test;

public class ModuleLoadingTest {

   private static final String IN_PARAMETER_NAME = "externalInput";
   private static final String VAR_PARAMETER_NAME = "internalBinding";

   private Rule createRuleWithInAndVarParameters() {
      final Rule rule = HenshinFactory.eINSTANCE.createRule();
      rule.setName("testRule");

      final Parameter in = HenshinFactory.eINSTANCE.createParameter();
      in.setName(IN_PARAMETER_NAME);
      in.setKind(ParameterKind.IN);
      rule.getParameters().add(in);

      final Parameter var = HenshinFactory.eINSTANCE.createParameter();
      var.setName(VAR_PARAMETER_NAME);
      var.setKind(ParameterKind.VAR);
      rule.getParameters().add(var);

      return rule;
   }

   private ModuleManager createModuleManager(final Rule rule) {
      final Module module = HenshinFactory.eINSTANCE.createModule();
      module.setName("testModule");
      module.getUnits().add(rule);

      final ModuleManager manager = new ModuleManager();
      manager.addModule(module);
      return manager;
   }

   @Test
   public void testAssignParameterValuesSkipsVarParameters() {
      final Rule rule = createRuleWithInAndVarParameters();
      final ModuleManager manager = createModuleManager(rule);

      final Parameter in = rule.getParameter(IN_PARAMETER_NAME);
      final Parameter var = rule.getParameter(VAR_PARAMETER_NAME);

      manager.setParameterValue(in, new FixValue<>("in-value"));
      manager.setParameterValue(var, new FixValue<>("var-value"));

      final Assignment assignment = InterpreterFactory.INSTANCE.createAssignment(rule, false);
      manager.assignParameterValues(assignment);

      assertEquals("in-value", assignment.getParameterValue(in));
      assertNull(assignment.getParameterValue(var));
   }

   @Test
   public void testClearNonSolutionParametersKeepsVarParameters() {
      final Rule rule = createRuleWithInAndVarParameters();
      final ModuleManager manager = createModuleManager(rule);

      final Parameter in = rule.getParameter(IN_PARAMETER_NAME);
      final Parameter var = rule.getParameter(VAR_PARAMETER_NAME);

      final Assignment assignment = InterpreterFactory.INSTANCE.createAssignment(rule, false);
      assignment.setParameterValue(in, "in-value");
      assignment.setParameterValue(var, "var-value");

      manager.addNonSolutionParameter(in);
      manager.addNonSolutionParameter(var);
      manager.clearNonSolutionParameters(assignment);

      assertNull(assignment.getParameterValue(in));
      assertEquals("var-value", assignment.getParameterValue(var));
   }

}
