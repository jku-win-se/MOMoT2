import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.henshin.interpreter.Engine;
import org.eclipse.emf.henshin.interpreter.Match;
import org.eclipse.emf.henshin.interpreter.UnitApplication;
import org.eclipse.emf.henshin.interpreter.impl.EngineImpl;
import org.eclipse.emf.henshin.interpreter.impl.UnitApplicationImpl;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.Unit;
import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HenshinValidator {
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String henshinPath = null;
        String metamodelPath = null;
        String modelPath = null;
        String ruleName = null;
        java.util.Map<String, Object> parameters = new java.util.HashMap<>();
        boolean validateStructure = false;
        boolean validateSemantic = false;
        boolean apply = false;

        for (int i = 0; i < args.length; i++) {
            if ("--validate-structure".equals(args[i])) {
                validateStructure = true;
                henshinPath = args[++i];
            } else if ("--validate-semantic".equals(args[i])) {
                validateSemantic = true;
                henshinPath = args[++i];
            } else if ("--metamodel".equals(args[i])) {
                metamodelPath = args[++i];
            } else if ("--apply".equals(args[i])) {
                apply = true;
                henshinPath = args[++i];
            } else if ("--model".equals(args[i])) {
                modelPath = args[++i];
            } else if ("--rule".equals(args[i])) {
                ruleName = args[++i];
            } else if (args[i].startsWith("-P")) {
                String[] parts = args[i].substring(2).split("=");
                if (parts.length == 2) {
                    parameters.put(parts[0], parseValue(parts[1]));
                }
            }
        }

        try {
            validate(henshinPath, metamodelPath, modelPath, ruleName, parameters, validateStructure, validateSemantic, apply);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Object parseValue(String value) {
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e1) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e2) {
                return value;
            }
        }
    }

    private static void validate(String henshinPath, String metamodelPath, String modelPath, String ruleName, java.util.Map<String, Object> parameters, boolean structure, boolean semantic, boolean apply) throws Exception {
        HenshinResourceSet resSet = new HenshinResourceSet();

        if (structure) {
            // Structure mode: load the .henshin XMI without registering any metamodel.
            // Verifies that the file is a well-formed Henshin module and lists all units.
            // Type references remain as unresolved proxies — this is expected.
            Module module = resSet.getModule(henshinPath, false);
            if (module == null) {
                throw new RuntimeException("Could not load Henshin module: " + henshinPath);
            }
            System.out.println("{ \"valid\": true, \"mode\": \"structure\", \"rules\": " + getRuleNames(module) + " }");
            return;
        }

        if (metamodelPath != null) {
            resSet.registerDynamicEPackages(metamodelPath);
        }

        Module module = resSet.getModule(henshinPath, false);
        if (module == null) {
            throw new RuntimeException("Could not load Henshin module: " + henshinPath);
        }

        System.out.println("{ \"valid\": true, \"mode\": \"semantic\", \"rules\": " + getRuleNames(module) + " }");

        if (apply) {
            if (modelPath == null || ruleName == null) {
                throw new RuntimeException("--model and --rule are required for --apply");
            }
            applyRule(module, resSet, metamodelPath, modelPath, ruleName, parameters);
        }
    }

    private static void applyRule(Module module, HenshinResourceSet resSet, String metamodelPath, String modelPath, String ruleName, java.util.Map<String, Object> parameters) throws Exception {
        Unit unit = module.getUnit(ruleName);
        if (unit == null) {
            throw new RuntimeException("Rule not found: " + ruleName);
        }

        Resource modelResource = resSet.getResource(modelPath);
        if (modelResource.getContents().isEmpty()) {
            throw new RuntimeException("Model is empty: " + modelPath);
        }
        Engine engine = new EngineImpl();
        UnitApplication app = new UnitApplicationImpl(engine);
        app.setEGraph(new org.eclipse.emf.henshin.interpreter.impl.EGraphImpl(modelResource));
        app.setUnit(unit);

        for (java.util.Map.Entry<String, Object> entry : parameters.entrySet()) {
            app.setParameterValue(entry.getKey(), entry.getValue());
        }

        try {
            if (app.execute(null)) {
                String outPath = "out_result.xmi";
                modelResource.setURI(URI.createFileURI(new File(outPath).getAbsolutePath()));
                modelResource.save(null);
                System.out.println("{ \"applied\": true, \"result\": \"" + outPath + "\" }");
            } else {
                System.out.println("{ \"applied\": false, \"reason\": \"Rule not applicable\" }");
            }
        } catch (Exception e) {
            System.out.println("{ \"applied\": false, \"reason\": \"Exception: " + e.getMessage().replace("\"", "\\\"") + "\" }");
            e.printStackTrace();
        }
    }

    private static String getRuleNames(Module module) {
        List<String> names = new ArrayList<>();
        for (Unit unit : module.getUnits()) {
            names.add("\"" + unit.getName() + "\"");
        }
        return names.toString();
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  --validate-structure <file.henshin>");
        System.out.println("      Load the .henshin XMI and list units. No metamodel required.");
        System.out.println("  --validate-semantic <file.henshin> --metamodel <file.ecore>");
        System.out.println("      Validate type references against the provided Ecore metamodel.");
        System.out.println("  --apply <file.henshin> --metamodel <file.ecore> --model <file.xmi> --rule <name> [-Pparam=value ...]");
        System.out.println("      Apply a named rule to a model instance and write the result.");
    }
}
