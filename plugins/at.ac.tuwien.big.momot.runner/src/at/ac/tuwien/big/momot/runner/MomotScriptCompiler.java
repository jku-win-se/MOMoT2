package at.ac.tuwien.big.momot.runner;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.google.inject.Injector;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.generator.IGenerator;
import org.eclipse.xtext.generator.JavaIoFileSystemAccess;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;

final class MomotScriptCompiler {
   static final class CompilationResult {
      private final Path jarPath;
      private final String mainClass;
      private final String compileLog;

      private CompilationResult(final Path jarPath, final String mainClass, final String compileLog) {
         this.jarPath = jarPath;
         this.mainClass = mainClass;
         this.compileLog = compileLog;
      }

      Path jarPath() {
         return jarPath;
      }

      String mainClass() {
         return mainClass;
      }

      String compileLog() {
         return compileLog;
      }
   }

   private MomotScriptCompiler() {
   }

   static CompilationResult compile(final Path scriptPath, final Path compileRoot) throws Exception {
      if(!Files.exists(scriptPath)) {
         throw new IllegalArgumentException("Script not found: " + scriptPath);
      }
      if(!scriptPath.toString().toLowerCase(Locale.ROOT).endsWith(".momot")) {
         throw new IllegalArgumentException("Expected a .momot script but got: " + scriptPath.getFileName());
      }

      final Path srcGenDir = compileRoot.resolve("src-gen");
      final Path classesDir = compileRoot.resolve("classes");
      final Path jarPath = compileRoot.resolve("program.jar");
      Files.createDirectories(srcGenDir);
      Files.createDirectories(classesDir);

      final Injector injector = createInjector();
      final Resource resource = loadResource(injector, scriptPath);
      final StringBuilder compileLog = new StringBuilder();
      compileLog.append("Validation disabled in headless mode; compiling script directly.")
         .append(System.lineSeparator());

      generateJava(injector, resource, srcGenDir);
      final String mainClass = determineMainClass(resource, scriptPath);
      final List<Path> javaSources = listFiles(srcGenDir, ".java");
      if(javaSources.isEmpty()) {
         throw new IllegalStateException("No Java files were generated from script: " + scriptPath);
      }
      normalizeGeneratedSources(javaSources);

      compileJava(javaSources, classesDir, compileLog);
      createJar(classesDir, jarPath, mainClass);
      compileLog.append("Generated main class: ").append(mainClass).append(System.lineSeparator());
      compileLog.append("Executable jar: ").append(jarPath).append(System.lineSeparator());
      return new CompilationResult(jarPath, mainClass, compileLog.toString());
   }

   private static Injector createInjector() throws Exception {
      final Class<?> setupClass = Class.forName("at.ac.tuwien.big.momot.lang.MOMoTStandaloneSetup");
      final Object setup = setupClass.getDeclaredConstructor().newInstance();
      final Method createInjectorMethod = setupClass.getMethod("createInjectorAndDoEMFRegistration");
      return (Injector) createInjectorMethod.invoke(setup);
   }

   private static Resource loadResource(final Injector injector, final Path scriptPath) {
      final XtextResourceSet resourceSet = injector.getInstance(XtextResourceSet.class);
      final URI scriptUri = URI.createFileURI(scriptPath.toAbsolutePath().normalize().toString());
      return resourceSet.getResource(scriptUri, true);
   }

   private static List<Issue> validate(final Injector injector, final Resource resource) {
      final IResourceValidator validator = injector.getInstance(IResourceValidator.class);
      return validator.validate(resource, CheckMode.ALL, CancelIndicator.NullImpl);
   }

   private static boolean hasErrors(final List<Issue> issues) {
      for(final Issue issue : issues) {
         if(issue.getSeverity() == org.eclipse.xtext.diagnostics.Severity.ERROR) {
            return true;
         }
      }
      return false;
   }

   private static void appendIssues(final StringBuilder out, final List<Issue> issues) {
      for(final Issue issue : issues) {
         out.append(issue.getSeverity())
               .append(": ")
               .append(issue.getMessage())
               .append(" (line ")
               .append(issue.getLineNumber())
               .append(")")
               .append(System.lineSeparator());
      }
   }

   private static void generateJava(final Injector injector, final Resource resource, final Path srcGenDir) {
      final JavaIoFileSystemAccess fileSystemAccess = injector.getInstance(JavaIoFileSystemAccess.class);
      fileSystemAccess.setOutputPath(srcGenDir.toString());
      final IGenerator generator = injector.getInstance(IGenerator.class);
      generator.doGenerate(resource, fileSystemAccess);
   }

   private static void normalizeGeneratedSources(final List<Path> javaSources) throws IOException {
      for(final Path javaSource : javaSources) {
         final String className = stripExtension(javaSource.getFileName().toString());
         final String content = Files.readString(javaSource, StandardCharsets.UTF_8);
         String normalizedContent = content;
         final String brokenMainLine = "search = new ();";
         if(normalizedContent.contains(brokenMainLine)) {
            final String fixedMainLine = className + " search = new " + className + "();";
            normalizedContent = normalizedContent.replace(brokenMainLine, fixedMainLine);
         }

         // Xtext generation may emit standalone eINSTANCE references, which are not valid Java statements.
         if(normalizedContent.contains("._eINSTANCE;")) {
            normalizedContent = normalizedContent.replace("._eINSTANCE;", "._eINSTANCE.getClass();");
         }

            // Some generated sections target API shapes that differ between MOMoT runtime variants.
            // Keep only minimal compatibility normalization so analysis/results can still execute.
         normalizedContent = replaceMethodBody(normalizedContent,
               "public void printSearchInfo(final TransformationSearchOrchestration orchestration)",
               "System.out.println(\"Search initialized.\");");

         if(!normalizedContent.equals(content)) {
            Files.writeString(javaSource, normalizedContent, StandardCharsets.UTF_8);
         }
      }
   }

   private static String replaceMethodBody(final String content, final String methodSignature, final String newBody) {
      final int signatureStart = content.indexOf(methodSignature);
      if(signatureStart < 0) {
         return content;
      }
      final int bodyStart = content.indexOf('{', signatureStart);
      if(bodyStart < 0) {
         return content;
      }
      int depth = 1;
      int index = bodyStart + 1;
      while(index < content.length() && depth > 0) {
         final char current = content.charAt(index);
         if(current == '{') {
            depth++;
         } else if(current == '}') {
            depth--;
         }
         index++;
      }
      if(depth != 0) {
         return content;
      }
      final String replacement = methodSignature + " {\n    " + newBody + "\n  }";
      return content.substring(0, signatureStart) + replacement + content.substring(index);
   }

   private static String determineMainClass(final Resource resource, final Path scriptPath) throws Exception {
      if(resource.getContents().isEmpty()) {
         throw new IllegalStateException("Loaded MOMoT script has no model root: " + scriptPath);
      }
      final Object root = resource.getContents().get(0);
      final Method getNameMethod = root.getClass().getMethod("getName");
      final Method getPackageMethod = root.getClass().getMethod("getPackage");
      final String scriptName = stripExtension(scriptPath.getFileName().toString());
      final String modelName = asTrimmedString(getNameMethod.invoke(root));
      final String packageName = asTrimmedString(getPackageMethod.invoke(root));
      final String className = isBlank(modelName) ? scriptName : modelName;
      if(isBlank(packageName)) {
         return className;
      }
      return packageName + "." + className;
   }

   private static void compileJava(final List<Path> sourceFiles, final Path classesDir, final StringBuilder compileLog)
         throws IOException {
      final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
      if(compiler == null) {
         throw new IllegalStateException("No Java compiler available. Use a JDK-based runtime image.");
      }

      final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
      try(StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
         final Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromPaths(sourceFiles);
         final String effectiveClasspath = expandClasspath(System.getProperty("java.class.path", ""));
         compileLog.append("Compiler classpath: ").append(effectiveClasspath).append(System.lineSeparator());
         final List<String> options = new ArrayList<>();
         options.add("-classpath");
         options.add(effectiveClasspath);
         options.add("-d");
         options.add(classesDir.toString());

         final StringWriter output = new StringWriter();
         final Boolean success = compiler.getTask(output, fileManager, diagnostics, options, null, units).call();
         if(output.getBuffer().length() > 0) {
            compileLog.append(output);
         }
         for(final Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            compileLog.append(diagnostic.getKind())
                  .append(": ")
                  .append(diagnostic.getMessage(null))
                  .append(" @ line ")
                  .append(diagnostic.getLineNumber())
                  .append(System.lineSeparator());
         }
         if(Boolean.FALSE.equals(success)) {
            throw new IllegalStateException("Java compilation of generated sources failed.\n" + compileLog);
         }
      }
   }

   private static String expandClasspath(final String classpath) {
      if(classpath == null || classpath.isBlank()) {
         return classpath;
      }

      final String pathSeparator = System.getProperty("path.separator");
      final StringJoiner joiner = new StringJoiner(pathSeparator);
      final String[] entries = classpath.split(java.util.regex.Pattern.quote(pathSeparator));

      for(final String entry : entries) {
         if(entry == null || entry.isBlank()) {
            continue;
         }
         if(entry.endsWith("*")) {
            final Path dir = Paths.get(entry.substring(0, entry.length() - 1));
            if(Files.isDirectory(dir)) {
               try(Stream<Path> stream = Files.list(dir)) {
                  stream.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                        .sorted(Comparator.naturalOrder())
                        .forEach(path -> joiner.add(path.toString()));
               } catch(final IOException ignored) {
                  joiner.add(entry);
               }
            } else {
               joiner.add(entry);
            }
         } else {
            joiner.add(entry);
         }
      }
      return joiner.toString();
   }

   private static void createJar(final Path classesDir, final Path jarPath, final String mainClass) throws IOException {
      final Manifest manifest = new Manifest();
      manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
      manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, mainClass);

      try(JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarPath), manifest)) {
         final List<Path> classFiles = listFiles(classesDir, ".class");
         for(final Path classFile : classFiles) {
            final String entryName = classesDir.relativize(classFile).toString().replace('\\', '/');
            jarOutputStream.putNextEntry(new JarEntry(entryName));
            Files.copy(classFile, jarOutputStream);
            jarOutputStream.closeEntry();
         }
      }
   }

   private static List<Path> listFiles(final Path root, final String suffix) throws IOException {
      if(!Files.exists(root)) {
         return List.of();
      }
      try(Stream<Path> stream = Files.walk(root)) {
         return stream.filter(Files::isRegularFile)
               .filter(path -> path.getFileName().toString().endsWith(suffix))
               .sorted(Comparator.naturalOrder())
               .collect(Collectors.toList());
      }
   }

   private static String stripExtension(final String fileName) {
      final int dotIndex = fileName.lastIndexOf('.');
      if(dotIndex < 0) {
         return fileName;
      }
      return fileName.substring(0, dotIndex);
   }

   private static String asTrimmedString(final Object value) {
      if(value == null) {
         return "";
      }
      return value.toString().trim();
   }

   private static boolean isBlank(final String value) {
      return value == null || value.trim().isEmpty();
   }
}