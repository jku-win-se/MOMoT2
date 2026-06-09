import { spawn } from 'child_process';
import { existsSync, mkdirSync, writeFileSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';
import fetch from 'node-fetch';

const __dirname = dirname(fileURLToPath(import.meta.url));
const LIB_DIR = join(__dirname, 'lib');
const SRC_DIR = join(__dirname, 'src');

const JARS = [
  {
    name: 'org.eclipse.emf.henshin.model_1.8.0.202302121604.jar',
    url: 'https://download.eclipse.org/modeling/emft/henshin/updates/release/plugins/org.eclipse.emf.henshin.model_1.8.0.202302121604.jar'
  },
  {
    name: 'org.eclipse.emf.henshin.interpreter_1.8.0.202302121604.jar',
    url: 'https://download.eclipse.org/modeling/emft/henshin/updates/release/plugins/org.eclipse.emf.henshin.interpreter_1.8.0.202302121604.jar'
  },
  {
    name: 'org.eclipse.emf.common_2.30.0.jar',
    url: 'https://repo1.maven.org/maven2/org/eclipse/emf/org.eclipse.emf.common/2.30.0/org.eclipse.emf.common-2.30.0.jar'
  },
  {
    name: 'org.eclipse.emf.ecore_2.36.0.jar',
    url: 'https://repo1.maven.org/maven2/org/eclipse/emf/org.eclipse.emf.ecore/2.36.0/org.eclipse.emf.ecore-2.36.0.jar'
  },
  {
    name: 'org.eclipse.emf.ecore.xmi_2.37.0.jar',
    url: 'https://repo1.maven.org/maven2/org/eclipse/emf/org.eclipse.emf.ecore.xmi/2.37.0/org.eclipse.emf.ecore.xmi-2.37.0.jar'
  },
  {
    name: 'nashorn-core-15.4.jar',
    url: 'https://repo1.maven.org/maven2/org/openjdk/nashorn/nashorn-core/15.4/nashorn-core-15.4.jar'
  },
  {
    name: 'asm-9.5.jar',
    url: 'https://repo1.maven.org/maven2/org/ow2/asm/asm/9.5/asm-9.5.jar'
  },
  {
    name: 'asm-commons-9.5.jar',
    url: 'https://repo1.maven.org/maven2/org/ow2/asm/asm-commons/9.5/asm-commons-9.5.jar'
  },
  {
    name: 'asm-tree-9.5.jar',
    url: 'https://repo1.maven.org/maven2/org/ow2/asm/asm-tree/9.5/asm-tree-9.5.jar'
  },
  {
    name: 'asm-util-9.5.jar',
    url: 'https://repo1.maven.org/maven2/org/ow2/asm/asm-util/9.5/asm-util-9.5.jar'
  }
];

async function setup() {
  if (!existsSync(LIB_DIR)) mkdirSync(LIB_DIR);

  for (const jar of JARS) {
    const jarPath = join(LIB_DIR, jar.name);
    if (!existsSync(jarPath)) {
      console.log(`Downloading ${jar.name}...`);
      const response = await fetch(jar.url);
      if (!response.ok) throw new Error(`Failed to download ${jar.name}: ${response.statusText}`);
      const buffer = Buffer.from(await response.arrayBuffer());
      writeFileSync(jarPath, buffer);
    }
  }

  // Compile Java validator
  console.log('Compiling HenshinValidator.java...');
  const classpath = join(LIB_DIR, '*');
  const javac = spawn('javac', ['-cp', classpath, '-d', LIB_DIR, join(SRC_DIR, 'HenshinValidator.java')]);

  return new Promise((resolve, reject) => {
    javac.on('close', (code) => {
      if (code === 0) {
        console.log('Compilation successful.');
        resolve();
      } else {
        reject(new Error(`javac failed with code ${code}`));
      }
    });
  });
}

async function run(args) {
  const sep = process.platform === 'win32' ? ';' : ':';
  const classpath = join(LIB_DIR, '*') + sep + LIB_DIR;
  const java = spawn('java', ['-cp', classpath, 'HenshinValidator', ...args]);

  java.stdout.on('data', (data) => process.stdout.write(data));
  java.stderr.on('data', (data) => process.stderr.write(data));

  java.on('close', (code) => process.exit(code));
}

const args = process.argv.slice(2);
if (args.includes('--setup')) {
  await setup();
} else {
  if (!existsSync(join(LIB_DIR, 'HenshinValidator.class'))) {
    await setup();
  }
  await run(args);
}
