package at.ac.tuwien.big.momot.runner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.validation.Issue;

public final class MomotValidator {
   public static void main(final String[] args) {
      if(args.length == 0) {
         printUsage();
         return;
      }

      String momotPath = null;
      String projectRoot = null;
      String mode = null;

      for(int i = 0; i < args.length; i++) {
         if("--validate-structure".equals(args[i])) {
            mode = "structure";
            momotPath = args[++i];
         } else if("--validate-semantic".equals(args[i])) {
            mode = "semantic";
            momotPath = args[++i];
         } else if("--compile".equals(args[i])) {
            mode = "compile";
            momotPath = args[++i];
         } else if("--project-root".equals(args[i])) {
            projectRoot = args[++i];
         }
      }

      if(mode == null || momotPath == null) {
         System.err.println("Error: specify --validate-structure, --validate-semantic, or --compile with a .momot file.");
         printUsage();
         System.exit(1);
         return;
      }

      try {
         final Path scriptPath = Paths.get(momotPath).toAbsolutePath().normalize();
         if(!Files.exists(scriptPath)) {
            throw new IllegalArgumentException("Script not found: " + scriptPath);
         }
         if(!scriptPath.toString().toLowerCase(Locale.ROOT).endsWith(".momot")) {
            throw new IllegalArgumentException("Expected a .momot script but got: " + scriptPath.getFileName());
         }

         final Path resolvedProjectRoot = resolveProjectRoot(scriptPath, projectRoot);

         switch(mode) {
            case "structure" -> validateStructure(scriptPath);
            case "semantic" -> validateSemantic(scriptPath, resolvedProjectRoot);
            case "compile" -> compileScript(scriptPath, resolvedProjectRoot);
            default -> throw new IllegalArgumentException("Unknown mode: " + mode);
         }
      } catch(final Exception e) {
         System.err.println("Error: " + e.getMessage());
         e.printStackTrace(System.err);
         System.exit(1);
      }
   }

   private static Path resolveProjectRoot(final Path scriptPath, final String projectRoot) {
      if(projectRoot != null && !projectRoot.isBlank()) {
         return Paths.get(projectRoot).toAbsolutePath().normalize();
      }
      final Path parent = scriptPath.getParent();
      if(parent != null) {
         return parent;
      }
      return Paths.get(".").toAbsolutePath().normalize();
   }

   private static void validateStructure(final Path scriptPath) throws Exception {
      final MomotScriptCompiler.ValidationResult result = MomotScriptCompiler.validateStructure(scriptPath);
      if(result.hasErrors()) {
         printValidationResult(false, "structure", result);
         System.exit(1);
         return;
      }
      System.out.println("{ \"valid\": true, \"mode\": \"structure\", \"issues\": [] }");
   }

   private static void validateSemantic(final Path scriptPath, final Path projectRoot) throws Exception {
      final MomotScriptCompiler.ValidationResult result = MomotScriptCompiler.validateOnly(scriptPath, projectRoot);
      if(result.hasErrors()) {
         printValidationResult(false, "semantic", result);
         System.exit(1);
         return;
      }
      System.out.println("{ \"valid\": true, \"mode\": \"semantic\", \"issues\": [] }");
   }

   private static void compileScript(final Path scriptPath, final Path projectRoot) throws Exception {
      MomotScriptCompiler.prepareHeadlessValidation(projectRoot);
      final Path compileRoot = Files.createTempDirectory("momot-validator-");
      try {
         final MomotScriptCompiler.CompilationResult result = MomotScriptCompiler.compile(scriptPath, compileRoot);
         System.out.println("{ \"valid\": true, \"mode\": \"compile\", \"mainClass\": \""
               + escapeJson(result.mainClass()) + "\" }");
      } finally {
         deleteRecursively(compileRoot);
      }
   }

   private static void printValidationResult(final boolean valid, final String mode,
         final MomotScriptCompiler.ValidationResult result) {
      final List<Map<String, Object>> issues = new ArrayList<>();
      for(final Resource.Diagnostic parseError : result.parseErrors()) {
         issues.add(issueFromParseDiagnostic(parseError));
      }
      for(final Issue issue : result.issues()) {
         issues.add(issueFromValidation(issue));
      }
      System.out.println("{ \"valid\": " + valid + ", \"mode\": \"" + mode + "\", \"issues\": "
            + issuesToJson(issues) + " }");
   }

   private static Map<String, Object> issueFromParseDiagnostic(final Resource.Diagnostic diagnostic) {
      final Map<String, Object> issue = new LinkedHashMap<>();
      issue.put("severity", "ERROR");
      issue.put("message", diagnostic.getMessage());
      issue.put("line", diagnostic.getLine());
      issue.put("column", diagnostic.getColumn());
      return issue;
   }

   private static Map<String, Object> issueFromValidation(final Issue issue) {
      final Map<String, Object> mapped = new LinkedHashMap<>();
      mapped.put("severity", issue.getSeverity() == null ? "ERROR" : issue.getSeverity().toString());
      mapped.put("message", issue.getMessage());
      mapped.put("line", issue.getLineNumber());
      mapped.put("column", issue.getColumn());
      return mapped;
   }

   private static String issuesToJson(final List<Map<String, Object>> issues) {
      final StringBuilder out = new StringBuilder("[");
      for(int i = 0; i < issues.size(); i++) {
         if(i > 0) {
            out.append(", ");
         }
         final Map<String, Object> issue = issues.get(i);
         out.append("{ ")
               .append("\"severity\": \"").append(escapeJson(String.valueOf(issue.get("severity")))).append("\", ")
               .append("\"message\": \"").append(escapeJson(String.valueOf(issue.get("message")))).append("\", ")
               .append("\"line\": ").append(issue.get("line")).append(", ")
               .append("\"column\": ").append(issue.get("column"))
               .append(" }");
      }
      out.append("]");
      return out.toString();
   }

   private static String escapeJson(final String value) {
      if(value == null) {
         return "";
      }
      return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
   }

   private static void deleteRecursively(final Path root) {
      try {
         if(!Files.exists(root)) {
            return;
         }
         try(final var stream = Files.walk(root)) {
            stream.sorted((a, b) -> b.compareTo(a)).forEach(path -> {
               try {
                  Files.deleteIfExists(path);
               } catch(final Exception ignored) {
               }
            });
         }
      } catch(final Exception ignored) {
      }
   }

   private static void printUsage() {
      System.out.println("Usage:");
      System.out.println("  --validate-structure <file.momot>");
      System.out.println("      Parse the MOMoT script and report syntax errors.");
      System.out.println("  --validate-semantic <file.momot> [--project-root <dir>]");
      System.out.println("      Run full Xtext validation including file paths and OCL.");
      System.out.println("  --compile <file.momot> [--project-root <dir>]");
      System.out.println("      Generate Java from the script and compile it with javac.");
   }
}
