package at.ac.tuwien.big.momot.runner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public final class RestServerMain {
   private static final int DEFAULT_PORT = 8080;
   private static final long DEFAULT_TIMEOUT_MS = Duration.ofMinutes(30).toMillis();

   private RestServerMain() {
   }

   public static void main(final String[] args) throws Exception {
      final int port = Integer.parseInt(System.getenv().getOrDefault("PORT", String.valueOf(DEFAULT_PORT)));
      final long timeoutMs = Long.parseLong(System.getenv().getOrDefault("MOMOT_TIMEOUT_MS", String.valueOf(DEFAULT_TIMEOUT_MS)));
      final HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
      final ExecutorService executor = Executors.newCachedThreadPool();
      server.setExecutor(executor);
      server.createContext("/", new DocsRedirectHandler());
      server.createContext("/docs", new DocsHandler());
      server.createContext("/openapi.json", new OpenApiHandler());
      server.createContext("/health", new HealthHandler());
      server.createContext("/run", new RunHandler(timeoutMs));
      server.start();
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         server.stop(0);
         executor.shutdownNow();
      }));
      System.out.println("MOMoT REST runner listening on port " + port);
   }

   private static final class DocsRedirectHandler implements HttpHandler {
      @Override
      public void handle(final HttpExchange exchange) throws IOException {
         if(!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Method Not Allowed");
            return;
         }
         final String path = exchange.getRequestURI().getPath();
         if(path == null || !"/".equals(path)) {
            sendText(exchange, 404, "Not Found");
            return;
         }
         exchange.getResponseHeaders().add("Location", "/docs");
         exchange.sendResponseHeaders(302, -1);
         exchange.close();
      }
   }

   private static final class DocsHandler implements HttpHandler {
      @Override
      public void handle(final HttpExchange exchange) throws IOException {
         if(!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Method Not Allowed");
            return;
         }
         sendBytes(exchange, 200, swaggerUiHtml().getBytes(StandardCharsets.UTF_8), "text/html; charset=utf-8");
      }
   }

   private static final class OpenApiHandler implements HttpHandler {
      @Override
      public void handle(final HttpExchange exchange) throws IOException {
         if(!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Method Not Allowed");
            return;
         }
         sendJson(exchange, 200, openApiJson());
      }
   }

   private static final class HealthHandler implements HttpHandler {
      @Override
      public void handle(final HttpExchange exchange) throws IOException {
         if(!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Method Not Allowed");
            return;
         }
         sendJson(exchange, 200, "{\"status\":\"ok\"}");
      }
   }

   private static final class RunHandler implements HttpHandler {
      private final long timeoutMs;

      private RunHandler(final long timeoutMs) {
         this.timeoutMs = timeoutMs;
      }

      @Override
      public void handle(final HttpExchange exchange) throws IOException {
         if(!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Method Not Allowed");
            return;
         }

         final Map<String, String> query = parseQuery(exchange.getRequestURI());
         String mainClass = query.get("mainClass");
         String jarName = firstNonBlank(query.get("jar"), query.get("program"), "program.jar");
         String scriptName = query.get("script");
         if(scriptName == null && jarName != null && jarName.toLowerCase().endsWith(".momot")) {
            scriptName = jarName;
            jarName = null;
         }

         if((mainClass == null || mainClass.trim().isEmpty()) && (scriptName == null || scriptName.trim().isEmpty())) {
            sendText(exchange, 400, "Missing required query parameter: mainClass (or provide script=<file.momot>)");
            return;
         }

         final Path jobDir = Files.createTempDirectory("momot-rest-job-");
         final Path workDir = jobDir.resolve("work");
         final Path outDir = jobDir.resolve("out");
         final Path runnerDir = jobDir.resolve("runner");
         Files.createDirectories(workDir);
         Files.createDirectories(outDir);
         Files.createDirectories(runnerDir);

         final Path logFile = runnerDir.resolve("runner.log");
         final Path exitCodeFile = runnerDir.resolve("exit_code.txt");
         final Path requestFile = runnerDir.resolve("request.json");
         Files.writeString(requestFile, "{\"mainClass\":\"" + mainClass + "\",\"jar\":\"" + jarName + "\"}", StandardCharsets.UTF_8);

         int exitCode = 1;
         String errorMessage = null;
         try {
            unzip(exchange.getRequestBody(), workDir);
            final Path jarPath;
            if(scriptName != null && !scriptName.trim().isEmpty()) {
               final Path scriptPath = resolveWithinWorkDir(workDir, scriptName);
               if(!Files.exists(scriptPath)) {
                  throw new IOException("Script not found in uploaded archive: " + scriptName);
               }
               final MomotScriptCompiler.CompilationResult compilation = MomotScriptCompiler
                     .compile(scriptPath, runnerDir.resolve("compile"));
               Files.writeString(runnerDir.resolve("compile.log"), compilation.compileLog(), StandardCharsets.UTF_8);
               jarPath = compilation.jarPath();
               if(mainClass == null || mainClass.trim().isEmpty()) {
                  mainClass = compilation.mainClass();
               }
            } else {
               jarPath = resolveWithinWorkDir(workDir, jarName);
               if(!Files.exists(jarPath)) {
                  throw new IOException("Jar not found in uploaded archive: " + jarName);
               }
            }

            if(mainClass == null || mainClass.trim().isEmpty()) {
               throw new IOException("Unable to determine main class for execution.");
            }

            Files.writeString(requestFile,
                  "{\"mainClass\":\"" + mainClass + "\",\"jar\":\""
                        + (jarName == null ? "<compiled-from-script>" : jarName)
                        + "\",\"script\":\"" + (scriptName == null ? "" : scriptName) + "\"}",
                  StandardCharsets.UTF_8);
            exitCode = runProgram(jobDir, workDir, outDir, jarPath, mainClass, logFile, timeoutMs);
         } catch(final Exception exception) {
            errorMessage = exception.getMessage();
            final StringWriter stackTraceWriter = new StringWriter();
            exception.printStackTrace(new PrintWriter(stackTraceWriter));
            Files.writeString(logFile,
               (errorMessage == null ? exception.toString() : errorMessage) + System.lineSeparator()
                  + stackTraceWriter,
               StandardCharsets.UTF_8);
            Files.writeString(exitCodeFile, Integer.toString(exitCode), StandardCharsets.UTF_8);
         }

         final byte[] responseZip = buildResponseZip(jobDir);
         exchange.getResponseHeaders().add("Content-Type", "application/zip");
         exchange.sendResponseHeaders(200, responseZip.length);
         try(OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(responseZip);
         } finally {
            cleanup(jobDir);
         }
      }

      private int runProgram(final Path jobDir, final Path workDir, final Path outDir, final Path jarPath, final String mainClass,
            final Path logFile, final long timeoutMs) throws IOException, InterruptedException {
         final String javaExecutable = Paths.get(System.getProperty("java.home"), "bin", isWindows() ? "java.exe" : "java").toString();
         final List<String> command = new ArrayList<>();
         command.add(javaExecutable);
         command.add("--add-opens");
         command.add("java.base/java.util=ALL-UNNAMED");
         command.add("-cp");
         command.add(System.getProperty("java.class.path"));
         command.add("at.ac.tuwien.big.momot.runner.RunnerMain");
         command.add("--jar");
         command.add(jarPath.toString());
         command.add("--mainClass");
         command.add(mainClass);
         command.add("--workdir");
         command.add(workDir.toString());
         command.add("--out");
         command.add(outDir.toString());

         final ProcessBuilder processBuilder = new ProcessBuilder(command);
         processBuilder.directory(workDir.toFile());
         processBuilder.redirectErrorStream(true);
         final Process process = processBuilder.start();

         final Thread logThread = new Thread(() -> copyProcessOutput(process.getInputStream(), logFile));
         logThread.setDaemon(true);
         logThread.start();

         final boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
         final int exitCode;
         if(!finished) {
            process.destroyForcibly();
            exitCode = 124;
         } else {
            exitCode = process.exitValue();
         }

         joinQuietly(logThread);
         Files.writeString(logFile.getParent().resolve("exit_code.txt"), Integer.toString(exitCode), StandardCharsets.UTF_8);
         return exitCode;
      }
   }

   private static void copyProcessOutput(final InputStream inputStream, final Path logFile) {
      try(InputStream in = inputStream; OutputStream out = Files.newOutputStream(logFile)) {
         final byte[] buffer = new byte[8192];
         int read;
         while((read = in.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
         }
      } catch(final IOException exception) {
         try {
            Files.writeString(logFile, exception.toString(), StandardCharsets.UTF_8);
         } catch(final IOException ignored) {
            // best effort
         }
      }
   }

   private static void unzip(final InputStream inputStream, final Path destination) throws IOException {
      try(ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
         ZipEntry entry;
         while((entry = zipInputStream.getNextEntry()) != null) {
            final String normalizedEntryName = entry.getName().replace('\\', '/');
            final Path resolved = destination.resolve(normalizedEntryName).normalize();
            if(!resolved.startsWith(destination)) {
               throw new IOException("Zip entry escapes target directory: " + entry.getName());
            }
            if(entry.isDirectory()) {
               Files.createDirectories(resolved);
            } else {
               final Path parent = resolved.getParent();
               if(parent != null) {
                  Files.createDirectories(parent);
               }
               Files.copy(zipInputStream, resolved);
            }
            zipInputStream.closeEntry();
         }
      }
   }

   private static Path resolveWithinWorkDir(final Path workDir, final String relativePath) throws IOException {
      final Path resolved = workDir.resolve(relativePath).normalize();
      if(!resolved.startsWith(workDir)) {
         throw new IOException("Path escapes work directory: " + relativePath);
      }
      return resolved;
   }

   private static byte[] buildResponseZip(final Path jobDir) throws IOException {
      final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
      try(ZipOutputStream zipOutputStream = new ZipOutputStream(byteStream)) {
         final Path runnerDir = jobDir.resolve("runner");
         final Path outDir = jobDir.resolve("out");
         final Path workOutDir = jobDir.resolve("work").resolve("out");
         if(Files.exists(runnerDir)) {
            addTree(zipOutputStream, runnerDir, "runner");
         }
         if(Files.exists(outDir) && hasRegularFiles(outDir)) {
            addTree(zipOutputStream, outDir, "out");
         } else if(Files.exists(workOutDir) && hasRegularFiles(workOutDir)) {
            addTree(zipOutputStream, workOutDir, "out");
         }
      }
      return byteStream.toByteArray();
   }

   private static boolean hasRegularFiles(final Path root) throws IOException {
      if(Files.isRegularFile(root)) {
         return true;
      }
      if(!Files.exists(root)) {
         return false;
      }
      try(java.util.stream.Stream<Path> stream = Files.walk(root)) {
         return stream.anyMatch(Files::isRegularFile);
      }
   }

   private static void addTree(final ZipOutputStream zipOutputStream, final Path root, final String prefix) throws IOException {
      if(Files.isRegularFile(root)) {
         addFile(zipOutputStream, root, prefix + "/" + root.getFileName().toString());
         return;
      }
      Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
         @Override
         public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            addFile(zipOutputStream, file, prefix + "/" + root.relativize(file).toString().replace('\\', '/'));
            return FileVisitResult.CONTINUE;
         }
      });
   }

   private static void addFile(final ZipOutputStream zipOutputStream, final Path file, final String entryName) throws IOException {
      final ZipEntry entry = new ZipEntry(entryName);
      zipOutputStream.putNextEntry(entry);
      Files.copy(file, zipOutputStream);
      zipOutputStream.closeEntry();
   }

   private static Map<String, String> parseQuery(final URI uri) {
      final Map<String, String> query = new HashMap<>();
      final String rawQuery = uri.getRawQuery();
      if(rawQuery == null || rawQuery.isEmpty()) {
         return query;
      }
      final String[] parts = rawQuery.split("&");
      for(final String part : parts) {
         final int equalsIndex = part.indexOf('=');
         if(equalsIndex < 0) {
            query.put(decode(part), "");
         } else {
            query.put(decode(part.substring(0, equalsIndex)), decode(part.substring(equalsIndex + 1)));
         }
      }
      return query;
   }

   private static String decode(final String value) {
      return URLDecoder.decode(value, StandardCharsets.UTF_8);
   }

   private static String firstNonBlank(final String first, final String second, final String fallback) {
      if(first != null && !first.trim().isEmpty()) {
         return first;
      }
      if(second != null && !second.trim().isEmpty()) {
         return second;
      }
      return fallback;
   }

   private static void sendJson(final HttpExchange exchange, final int statusCode, final String body) throws IOException {
      sendBytes(exchange, statusCode, body.getBytes(StandardCharsets.UTF_8), "application/json; charset=utf-8");
   }

   private static void sendText(final HttpExchange exchange, final int statusCode, final String body) throws IOException {
      sendBytes(exchange, statusCode, body.getBytes(StandardCharsets.UTF_8), "text/plain; charset=utf-8");
   }

   private static void sendBytes(final HttpExchange exchange, final int statusCode, final byte[] body, final String contentType) throws IOException {
      exchange.getResponseHeaders().add("Content-Type", contentType);
      exchange.sendResponseHeaders(statusCode, body.length);
      try(OutputStream outputStream = exchange.getResponseBody()) {
         outputStream.write(body);
      }
   }

   private static void cleanup(final Path jobDir) {
      try {
         if(Files.exists(jobDir)) {
            Files.walkFileTree(jobDir, new SimpleFileVisitor<Path>() {
               @Override
               public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                  Files.deleteIfExists(file);
                  return FileVisitResult.CONTINUE;
               }

               @Override
               public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                  Files.deleteIfExists(dir);
                  return FileVisitResult.CONTINUE;
               }
            });
         }
      } catch(final IOException exception) {
         // best effort cleanup
      }
   }

   private static void joinQuietly(final Thread thread) throws InterruptedException {
      thread.join(TimeUnit.SECONDS.toMillis(10));
   }

   private static boolean isWindows() {
      return System.getProperty("os.name", "").toLowerCase().contains("win");
   }

    private static String swaggerUiHtml() {
         return """
                  <!doctype html>
                  <html lang="en">
                  <head>
                     <meta charset="utf-8"/>
                     <meta name="viewport" content="width=device-width, initial-scale=1"/>
                     <title>MOMoT REST API Docs</title>
                     <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@5/swagger-ui.css"/>
                     <style>
                        body { margin: 0; background: #fafafa; }
                        #swagger-ui { max-width: 1200px; margin: 0 auto; }
                     </style>
                  </head>
                  <body>
                     <div id="swagger-ui"></div>
                     <script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js"></script>
                     <script>
                        window.ui = SwaggerUIBundle({
                           url: '/openapi.json',
                           dom_id: '#swagger-ui',
                           deepLinking: true,
                           displayRequestDuration: true,
                           presets: [SwaggerUIBundle.presets.apis],
                           layout: 'BaseLayout'
                        });
                     </script>
                  </body>
                  </html>
                  """;
    }

    private static String openApiJson() {
         return """
                  {
                     "openapi": "3.0.3",
                     "info": {
                        "title": "MOMoT Headless REST Runner",
                        "version": "1.0.0",
                        "description": "Zip-in/zip-out API for running MOMoT jobs in Docker."
                     },
                     "paths": {
                        "/health": {
                           "get": {
                              "summary": "Health check",
                              "responses": {
                                 "200": {
                                    "description": "Service is ready",
                                    "content": {
                                       "application/json": {
                                          "schema": {
                                             "type": "object",
                                             "properties": {
                                                "status": { "type": "string", "example": "ok" }
                                             },
                                             "required": ["status"]
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        },
                        "/run": {
                           "post": {
                              "summary": "Execute a MOMoT run",
                              "description": "Upload a zip as raw binary body (application/zip). Use either script=<relative/path.momot> or mainClass=<fqcn> query parameter.",
                              "parameters": [
                                 {
                                    "name": "script",
                                    "in": "query",
                                    "required": false,
                                    "schema": { "type": "string" },
                                    "description": "Relative path to .momot file inside uploaded zip."
                                 },
                                 {
                                    "name": "mainClass",
                                    "in": "query",
                                    "required": false,
                                    "schema": { "type": "string" },
                                    "description": "Java main class to run (if not compiling from script)."
                                 },
                                 {
                                    "name": "jar",
                                    "in": "query",
                                    "required": false,
                                    "schema": { "type": "string", "default": "program.jar" },
                                    "description": "Relative jar path inside uploaded zip when using mainClass mode."
                                 }
                              ],
                              "requestBody": {
                                 "required": true,
                                 "content": {
                                    "application/zip": {
                                       "schema": {
                                          "type": "string",
                                          "format": "binary"
                                       }
                                    }
                                 }
                              },
                              "responses": {
                                 "200": {
                                    "description": "Run result bundle",
                                    "content": {
                                       "application/zip": {
                                          "schema": {
                                             "type": "string",
                                             "format": "binary"
                                          }
                                       }
                                    }
                                 },
                                 "400": {
                                    "description": "Invalid request"
                                 }
                              }
                           }
                        }
                     }
                  }
                  """;
    }
}
