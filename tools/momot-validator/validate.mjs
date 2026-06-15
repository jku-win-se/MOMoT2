import { spawn, spawnSync } from 'child_process';
import { existsSync, mkdirSync, readdirSync, copyFileSync, rmSync, writeFileSync, readFileSync, mkdtempSync } from 'fs';
import { homedir, tmpdir } from 'os';
import { join, dirname, resolve, isAbsolute } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = resolve(__dirname, '..', '..');
const LIB_DIR = join(__dirname, 'lib');

const MAVEN_MODULES =
  'tooling/at.ac.tuwien.big.momot.tooling,plugins/at.ac.tuwien.big.moea,plugins/at.ac.tuwien.big.momot.core,plugins/at.ac.tuwien.big.momot.lang,plugins/at.ac.tuwien.big.momot.runner';

const EXTERNAL_JARS = [
  {
    name: 'org.eclipse.emf.henshin.model_1.8.0.202302121604.jar',
    url: 'https://download.eclipse.org/modeling/emft/henshin/updates/release/plugins/org.eclipse.emf.henshin.model_1.8.0.202302121604.jar'
  },
  {
    name: 'org.eclipse.emf.henshin.interpreter_1.8.0.202302121604.jar',
    url: 'https://download.eclipse.org/modeling/emft/henshin/updates/release/plugins/org.eclipse.emf.henshin.interpreter_1.8.0.202302121604.jar'
  },
  { name: 'nashorn-core-15.4.jar', url: 'https://repo1.maven.org/maven2/org/openjdk/nashorn/nashorn-core/15.4/nashorn-core-15.4.jar' },
  { name: 'asm-7.3.1.jar', url: 'https://repo1.maven.org/maven2/org/ow2/asm/asm/7.3.1/asm-7.3.1.jar' },
  { name: 'asm-commons-7.3.1.jar', url: 'https://repo1.maven.org/maven2/org/ow2/asm/asm-commons/7.3.1/asm-commons-7.3.1.jar' },
  { name: 'asm-tree-7.3.1.jar', url: 'https://repo1.maven.org/maven2/org/ow2/asm/asm-tree/7.3.1/asm-tree-7.3.1.jar' },
  { name: 'asm-util-7.3.1.jar', url: 'https://repo1.maven.org/maven2/org/ow2/asm/asm-util/7.3.1/asm-util-7.3.1.jar' },
  { name: 'asm-analysis-7.3.1.jar', url: 'https://repo1.maven.org/maven2/org/ow2/asm/asm-analysis/7.3.1/asm-analysis-7.3.1.jar' }
];

const OCL_BUNDLES = [
  'org.eclipse.ocl',
  'org.eclipse.ocl.common',
  'org.eclipse.ocl.ecore',
  'lpg.runtime.java'
];

const MAVEN_IMAGE = 'maven:3.9-eclipse-temurin-21';
const RUNTIME_IMAGE = 'eclipse-temurin:21-jdk';

function resolveMavenCommand() {
  const fromEnv = process.env.MAVEN_HOME
    ? join(process.env.MAVEN_HOME, 'bin', process.platform === 'win32' ? 'mvn.cmd' : 'mvn')
    : null;
  const candidates = [
    fromEnv,
    process.platform === 'win32' ? 'mvn.cmd' : 'mvn',
    join(homedir(), 'tools', 'apache-maven-3.9.6', 'bin', process.platform === 'win32' ? 'mvn.cmd' : 'mvn')
  ].filter(Boolean);
  for (const candidate of candidates) {
    if (candidate.includes('/') || candidate.includes('\\')) {
      if (existsSync(candidate)) return candidate;
    } else {
      return candidate;
    }
  }
  return 'mvn';
}

function getLocalJavaMajor() {
  const output = spawnSync('java', ['-version'], { encoding: 'utf8' });
  const versionText = `${output.stderr || ''}${output.stdout || ''}`;
  const match = versionText.match(/version "(\d+)/);
  return match ? Number(match[1]) : 0;
}

function isDockerAvailable() {
  const result = spawnSync('docker', ['info'], { encoding: 'utf8' });
  return result.status === 0;
}

function toDockerMountPath(hostPath) {
  return resolve(hostPath);
}

function needsShell(command) {
  return process.platform === 'win32' && /\.(cmd|bat)$/i.test(command);
}

function runCommand(command, args, options = {}) {
  return new Promise((resolvePromise, reject) => {
    const proc = spawn(command, args, {
      stdio: 'inherit',
      shell: needsShell(command),
      ...options
    });
    proc.on('close', (code) => {
      if (code === 0) resolvePromise();
      else reject(new Error(`${command} ${args.join(' ')} failed with code ${code}`));
    });
    proc.on('error', reject);
  });
}

function findLatestJar(dir) {
  if (!existsSync(dir)) return null;
  const jars = readdirSync(dir).filter((f) => f.endsWith('.jar') && !f.endsWith('-sources.jar') && !f.endsWith('-javadoc.jar'));
  if (jars.length === 0) return null;
  jars.sort();
  return join(dir, jars[jars.length - 1]);
}

function copyGlob(sourceDir, pattern, destDir = LIB_DIR, { excludeSources = false } = {}) {
  if (!existsSync(sourceDir)) return;
  for (const file of readdirSync(sourceDir)) {
    if (!pattern.test(file)) continue;
    if (excludeSources && (file.endsWith('-sources.jar') || file.endsWith('-javadoc.jar'))) continue;
    copyFileSync(join(sourceDir, file), join(destDir, file));
  }
}

function findOclBundle(bundleName, m2Root = join(homedir(), '.m2')) {
  const base = join(m2Root, 'repository', 'p2', 'osgi', 'bundle', bundleName);
  if (!existsSync(base)) return null;
  const versions = readdirSync(base).sort();
  for (let i = versions.length - 1; i >= 0; i--) {
    const jar = findLatestJar(join(base, versions[i]));
    if (jar) return jar;
  }
  return null;
}

function stripJarSignatures(jarPath) {
  const workDir = mkdtempSync(join(tmpdir(), 'momot-strip-'));
  try {
    const extract = spawnSync('jar', ['xf', jarPath], { cwd: workDir, stdio: 'pipe' });
    if (extract.status !== 0) return;

    const metaInf = join(workDir, 'META-INF');
    if (existsSync(metaInf)) {
      for (const file of readdirSync(metaInf)) {
        if (file.endsWith('.SF') || file.endsWith('.RSA') || file.endsWith('.DSA')) {
          rmSync(join(metaInf, file), { force: true });
        }
      }
    }

    const manifest = join(metaInf, 'MANIFEST.MF');
    const rebuilt = `${jarPath}.new`;
    const packArgs = existsSync(manifest)
      ? ['cfm', rebuilt, manifest, '-C', workDir, '.']
      : ['cf', rebuilt, '-C', workDir, '.'];
    // jar repacks from workDir root; exclude the temp .new file if present
    const pack = spawnSync('jar', packArgs, { stdio: 'pipe' });
    if (pack.status === 0) {
      copyFileSync(rebuilt, jarPath);
      rmSync(rebuilt, { force: true });
    }
  } finally {
    rmSync(workDir, { recursive: true, force: true });
  }
}

async function stripAllJarSignatures(dir = LIB_DIR) {
  console.log('Stripping JAR signatures (prevents Eclipse classpath conflicts)...');
  for (const file of readdirSync(dir)) {
    if (file.endsWith('.jar')) {
      stripJarSignatures(join(dir, file));
    }
  }
}

async function downloadJar(jar, destDir = LIB_DIR) {
  const dest = join(destDir, jar.name);
  if (existsSync(dest)) return;
  console.log(`Downloading ${jar.name}...`);
  const response = await fetch(jar.url);
  if (!response.ok) throw new Error(`Failed to download ${jar.name}: ${response.statusText}`);
  const buffer = Buffer.from(await response.arrayBuffer());
  writeFileSync(dest, buffer);
}

function isSetupComplete() {
  if (!existsSync(LIB_DIR)) return false;
  const jars = readdirSync(LIB_DIR).filter((f) => f.endsWith('.jar'));
  return jars.some((f) => f.startsWith('at.ac.tuwien.big.momot.runner'));
}

function resetLibDir() {
  if (existsSync(LIB_DIR)) {
    rmSync(LIB_DIR, { recursive: true, force: true });
  }
  mkdirSync(LIB_DIR, { recursive: true });
}

async function assembleLibFromBuild({ mvn, m2Root = join(homedir(), '.m2') }) {
  resetLibDir();

  const pluginTargets = [
    ['plugins/at.ac.tuwien.big.momot.runner/target', /^at\.ac\.tuwien\.big\.momot\.runner-.*\.jar$/],
    ['plugins/at.ac.tuwien.big.moea/target', /^at\.ac\.tuwien\.big\.moea-.*\.jar$/],
    ['plugins/at.ac.tuwien.big.momot.core/target', /^at\.ac\.tuwien\.big\.momot\.core-.*\.jar$/],
    ['plugins/at.ac.tuwien.big.momot.lang/target', /^at\.ac\.tuwien\.big\.momot\.lang-.*\.jar$/]
  ];

  for (const [relDir, pattern] of pluginTargets) {
    copyGlob(join(REPO_ROOT, relDir), pattern, LIB_DIR, { excludeSources: true });
  }

  const moeaLib = join(REPO_ROOT, 'plugins/at.ac.tuwien.big.moea/lib');
  if (existsSync(moeaLib)) {
    for (const file of readdirSync(moeaLib)) {
      if (file.endsWith('.jar') && file !== 'guava-18.0.jar') {
        copyFileSync(join(moeaLib, file), join(LIB_DIR, file));
      }
    }
  }

  console.log('Copying runtime dependencies...');
  const depDir = join(homedir(), '.momot-validator-deps');
  if (existsSync(depDir)) rmSync(depDir, { recursive: true, force: true });
  mkdirSync(depDir, { recursive: true });

  await runCommand(mvn, [
    '-pl', 'plugins/at.ac.tuwien.big.momot.runner',
    '-DskipTests=true',
    '-DincludeScope=runtime',
    'dependency:copy-dependencies',
    `-DoutputDirectory=${depDir}`
  ], { cwd: REPO_ROOT });

  for (const file of readdirSync(depDir)) {
    if (file.endsWith('.jar') && !file.endsWith('-sources.jar') && !file.endsWith('-javadoc.jar')) {
      if (file === 'guava-18.0.jar') continue;
      copyFileSync(join(depDir, file), join(LIB_DIR, file));
    }
  }
  rmSync(depDir, { recursive: true, force: true });

  console.log('Copying OCL bundles from Maven repository...');
  for (const bundle of OCL_BUNDLES) {
    const jar = findOclBundle(bundle, m2Root);
    if (!jar) {
      throw new Error(`Missing OCL bundle ${bundle} in Maven repository — run the Maven build first.`);
    }
    const destName = jar.split(/[\\/]/).pop();
    copyFileSync(jar, join(LIB_DIR, destName));
  }

  console.log('Downloading Henshin and Nashorn dependencies...');
  for (const jar of EXTERNAL_JARS) {
    await downloadJar(jar);
  }

  await stripAllJarSignatures();
}

async function setupLocal({ compat = false } = {}) {
  const javaMajor = getLocalJavaMajor();
  if (javaMajor < 17) {
    throw new Error(`JDK 17+ required (found Java ${javaMajor || 'unknown'}).`);
  }
  if (!compat && javaMajor < 21) {
    throw new Error(`Local JDK 21+ required (found Java ${javaMajor}). Use --docker or let setup use Java 17 compatibility mode.`);
  }

  const mvn = resolveMavenCommand();
  const mvnArgs = [
    '-pl', MAVEN_MODULES,
    '-am',
    '-Declipse.release=latest',
    '-DskipTests=true',
    '-Dxtend.skip=false',
    'package'
  ];
  if (compat) {
    mvnArgs.splice(mvnArgs.length - 1, 0, '-Dmaven.compiler.release=17');
    console.log(`Building MOMoT plugins with Maven (${mvn}) using Java 17 compatibility mode...`);
  } else {
    console.log(`Building MOMoT plugins with Maven (${mvn})...`);
  }

  await runCommand(mvn, mvnArgs, { cwd: REPO_ROOT });
  await assembleLibFromBuild({ mvn });
  writeFileSync(join(LIB_DIR, '.build-mode'), compat ? 'compat-17' : 'jdk-21');
  console.log('MOMoT validator setup complete.');
}

function dockerAssembleScript() {
  return `set -eux
cd /src
mvn -pl ${MAVEN_MODULES} -am -Declipse.release=latest -DskipTests=true -Dxtend.skip=false package
rm -rf tools/momot-validator/lib
mkdir -p tools/momot-validator/lib
cp plugins/at.ac.tuwien.big.momot.runner/target/at.ac.tuwien.big.momot.runner-*.jar tools/momot-validator/lib/
cp plugins/at.ac.tuwien.big.moea/target/at.ac.tuwien.big.moea-*.jar tools/momot-validator/lib/
for f in plugins/at.ac.tuwien.big.moea/lib/*.jar; do
  base="$(basename "$f")"
  if [ "$base" != "guava-18.0.jar" ]; then cp "$f" tools/momot-validator/lib/; fi
done
cp plugins/at.ac.tuwien.big.momot.core/target/at.ac.tuwien.big.momot.core-*.jar tools/momot-validator/lib/
cp plugins/at.ac.tuwien.big.momot.lang/target/at.ac.tuwien.big.momot.lang-*.jar tools/momot-validator/lib/
mvn -pl plugins/at.ac.tuwien.big.momot.runner -DskipTests=true -DincludeScope=runtime dependency:copy-dependencies -DoutputDirectory=tools/momot-validator/.deps
find tools/momot-validator/.deps -maxdepth 1 -type f -name '*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' ! -name 'guava-18.0.jar' -exec cp {} tools/momot-validator/lib/ \\;
rm -rf tools/momot-validator/.deps
for bundle in ${OCL_BUNDLES.join(' ')}; do
  jar_path="$(find /root/.m2/repository/p2/osgi/bundle/$bundle -name '*.jar' | sort -V | tail -n 1)"
  if [ -z "$jar_path" ]; then
    echo "Missing required OCL bundle jar for $bundle" >&2
    exit 1
  fi
  cp "$jar_path" tools/momot-validator/lib/
done
base_url="https://download.eclipse.org/modeling/emft/henshin/updates/release/plugins"
curl -fsSL "$base_url/org.eclipse.emf.henshin.model_1.8.0.202302121604.jar" -o tools/momot-validator/lib/org.eclipse.emf.henshin.model_1.8.0.202302121604.jar
curl -fsSL "$base_url/org.eclipse.emf.henshin.interpreter_1.8.0.202302121604.jar" -o tools/momot-validator/lib/org.eclipse.emf.henshin.interpreter_1.8.0.202302121604.jar
maven_base="https://repo1.maven.org/maven2"
curl -fsSL "$maven_base/org/openjdk/nashorn/nashorn-core/15.4/nashorn-core-15.4.jar" -o tools/momot-validator/lib/nashorn-core-15.4.jar
curl -fsSL "$maven_base/org/ow2/asm/asm/7.3.1/asm-7.3.1.jar" -o tools/momot-validator/lib/asm-7.3.1.jar
curl -fsSL "$maven_base/org/ow2/asm/asm-commons/7.3.1/asm-commons-7.3.1.jar" -o tools/momot-validator/lib/asm-commons-7.3.1.jar
curl -fsSL "$maven_base/org/ow2/asm/asm-tree/7.3.1/asm-tree-7.3.1.jar" -o tools/momot-validator/lib/asm-tree-7.3.1.jar
curl -fsSL "$maven_base/org/ow2/asm/asm-util/7.3.1/asm-util-7.3.1.jar" -o tools/momot-validator/lib/asm-util-7.3.1.jar
curl -fsSL "$maven_base/org/ow2/asm/asm-analysis/7.3.1/asm-analysis-7.3.1.jar" -o tools/momot-validator/lib/asm-analysis-7.3.1.jar
for jar_file in tools/momot-validator/lib/*.jar; do
  work_dir="$(mktemp -d)"
  (cd "$work_dir" && jar xf "$jar_file")
  rm -f "$work_dir"/META-INF/*.SF "$work_dir"/META-INF/*.RSA "$work_dir"/META-INF/*.DSA
  if [ -f "$work_dir/META-INF/MANIFEST.MF" ]; then
    (cd "$work_dir" && jar cfm "$jar_file.new" META-INF/MANIFEST.MF .)
  else
    (cd "$work_dir" && jar cf "$jar_file.new" .)
  fi
  mv "$jar_file.new" "$jar_file"
  rm -rf "$work_dir"
done
`;
}

async function setupDocker() {
  if (!isDockerAvailable()) {
    throw new Error('Docker is not available. Start Docker Desktop or install JDK 21 locally.');
  }

  const mount = toDockerMountPath(REPO_ROOT);
  console.log(`Building MOMoT plugins inside Docker (${MAVEN_IMAGE})...`);
  await runCommand('docker', [
    'run', '--rm',
    '-v', `${mount}:/src`,
    '-w', '/src',
    MAVEN_IMAGE,
    'bash', '-lc', dockerAssembleScript()
  ]);
  writeFileSync(join(LIB_DIR, '.build-mode'), 'docker-21');
  console.log('MOMoT validator setup complete.');
}

function getSetupStrategy({ forceDocker = false, forceLocal = false } = {}) {
  if (forceDocker) return 'docker';
  if (forceLocal) return getLocalJavaMajor() >= 21 ? 'local' : 'local-compat';
  const javaMajor = getLocalJavaMajor();
  if (javaMajor >= 21) return 'local';
  if (javaMajor >= 17) return 'local-compat';
  return 'unavailable';
}

function getRunStrategy({ forceDocker = false, forceLocal = false } = {}) {
  if (forceDocker) return 'docker';
  if (forceLocal) return 'local';
  const javaMajor = getLocalJavaMajor();
  const buildMode = existsSync(join(LIB_DIR, '.build-mode'))
    ? readFileSync(join(LIB_DIR, '.build-mode'), 'utf8').trim()
    : '';

  if (buildMode === 'docker-21' && javaMajor < 21) {
    return isDockerAvailable() ? 'docker' : 'unavailable';
  }
  if (javaMajor >= 17) return 'local';
  return isDockerAvailable() ? 'docker' : 'unavailable';
}

async function setup({ forceDocker = false, forceLocal = false } = {}) {
  const strategy = getSetupStrategy({ forceDocker, forceLocal });
  if (strategy === 'docker') {
    if (!isDockerAvailable()) {
      throw new Error('Docker is not available. Start Docker Desktop or use local setup with Java 17+.');
    }
    console.log('Using Docker for setup.');
    await setupDocker();
    return;
  }
  if (strategy === 'local-compat') {
    console.log(`Local Java ${getLocalJavaMajor()} detected — building with Java 17 compatibility mode.`);
    await setupLocal({ compat: true });
    return;
  }
  if (strategy === 'local') {
    await setupLocal({ compat: false });
    return;
  }
  throw new Error('JDK 17+ is required. Install Temurin 17 or newer.');
}

function normalizeValidatorArgs(args) {
  return args.map((arg, index, all) => {
    if (arg.startsWith('--')) return arg;
    const prev = index > 0 ? all[index - 1] : '';
    if (prev === '--project-root' || prev === '--validate-structure' || prev === '--validate-semantic' || prev === '--compile') {
      return isAbsolute(arg) ? resolve(arg) : resolve(process.cwd(), arg);
    }
    return arg;
  });
}

function dockerizeValidatorArgs(args) {
  return normalizeValidatorArgs(args).map((arg) => {
    if (arg.startsWith('--')) return arg;
    return arg.replace(/\\/g, '/');
  });
}

function buildClasspath() {
  const sep = process.platform === 'win32' ? ';' : ':';
  return readdirSync(LIB_DIR)
    .filter((file) => file.endsWith('.jar'))
    .map((file) => join(LIB_DIR, file))
    .join(sep);
}

async function runLocal(args) {
  const normalizedArgs = normalizeValidatorArgs(args);
  const classpath = buildClasspath();
  const java = spawn('java', ['-cp', classpath, 'at.ac.tuwien.big.momot.runner.MomotValidator', ...normalizedArgs]);

  java.stdout.on('data', (data) => process.stdout.write(data));
  java.stderr.on('data', (data) => process.stderr.write(data));

  return new Promise((resolvePromise) => {
    java.on('close', (code) => resolvePromise(code ?? 1));
  });
}

async function runDocker(args) {
  if (!isDockerAvailable()) {
    throw new Error('Docker is not available. Install JDK 21 locally or start Docker Desktop.');
  }

  const mount = toDockerMountPath(REPO_ROOT);
  const dockerArgs = dockerizeValidatorArgs(args);
  const quotedArgs = dockerArgs.map((arg) => `'${arg.replace(/'/g, `'\\''`)}'`).join(' ');
  const command = `java -cp "/src/tools/momot-validator/lib/*" at.ac.tuwien.big.momot.runner.MomotValidator ${quotedArgs}`; // glob works in Linux container

  return new Promise((resolvePromise, reject) => {
    const proc = spawn('docker', [
      'run', '--rm',
      '-v', `${mount}:/src`,
      '-w', '/src',
      RUNTIME_IMAGE,
      'bash', '-lc', command
    ], { stdio: 'inherit', shell: process.platform === 'win32' });
    proc.on('close', (code) => resolvePromise(code ?? 1));
    proc.on('error', reject);
  });
}

async function run(args, { forceDocker = false, forceLocal = false } = {}) {
  const strategy = getRunStrategy({ forceDocker, forceLocal });
  if (strategy === 'docker') {
    console.log('Running validator inside Docker (JDK 21).');
    return runDocker(args);
  }
  if (strategy === 'unavailable') {
    throw new Error(
      'Cannot run the validator with Java ' + getLocalJavaMajor() +
      '. Install JDK 21+, start Docker Desktop, or rebuild with: node validate.mjs --setup'
    );
  }
  return runLocal(args);
}

const rawArgs = process.argv.slice(2);
const forceDocker = rawArgs.includes('--docker');
const forceLocal = rawArgs.includes('--local');
const args = rawArgs.filter((arg) => arg !== '--docker' && arg !== '--local');

if (args.includes('--setup')) {
  await setup({ forceDocker, forceLocal });
} else {
  if (!isSetupComplete()) {
    console.log('Validator lib/ not found. Running setup...');
    await setup({ forceDocker, forceLocal });
  }
  const exitCode = await run(args, { forceDocker, forceLocal });
  process.exit(exitCode);
}
