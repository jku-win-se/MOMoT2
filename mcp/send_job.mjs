import fs from 'node:fs';
import path from 'node:path';
import { executeMomotJob } from './lib.js';

async function run() {
  const mcpDir = process.cwd();
  const repoRoot = path.join(mcpDir, '..');
  const baseDir = path.join(repoRoot, 'stack-example-minimal');
  
  const files = {
    'model/stack.ecore': fs.readFileSync(path.join(baseDir, 'model/stack.ecore')).toString('base64'),
    'model/stack.henshin': fs.readFileSync(path.join(baseDir, 'model/stack.henshin')).toString('base64'),
    'model/input/model/model_five_stacks.xmi': fs.readFileSync(path.join(baseDir, 'model/input/model/model_five_stacks.xmi')).toString('base64'),
    'src/at/ac/tuwien/big/momot/examples/stack/StackSearchExample.momot': fs.readFileSync(path.join(baseDir, 'src/at/ac/tuwien/big/momot/examples/stack/StackSearchExample.momot')).toString('base64')
  };

  const input = {
    restBaseUrl: 'http://localhost:8081',
    scriptPath: 'src/at/ac/tuwien/big/momot/examples/stack/StackSearchExample.momot',
    filesBase64: files
  };

  try {
    const result = await executeMomotJob(input);
    console.log(JSON.stringify(result, null, 2));
  } catch (error) {
    console.error(error);
    process.exit(1);
  }
}

run();
