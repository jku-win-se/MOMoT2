import test from 'node:test';
import assert from 'node:assert/strict';
import JSZip from 'jszip';
import {
  buildJobZip,
  generateArtifactsFromEcore,
  normalizeZipPath,
  parseResponseZip,
  validateGeneratedScenario
} from '../lib.js';

test('normalizeZipPath rejects traversal and drive letters', () => {
  assert.equal(normalizeZipPath('src/a.momot'), 'src/a.momot');
  assert.throws(() => normalizeZipPath('../evil.txt'));
  assert.throws(() => normalizeZipPath('C:/evil.txt'));
});

test('generateArtifactsFromEcore creates deterministic core files', async () => {
  const ecore = `<?xml version="1.0" encoding="UTF-8"?>\n<ecore:EPackage xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="stack" nsURI="http://example/stack/1.0">\n  <eClassifiers xsi:type="ecore:EClass" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" name="StackModel"/>\n</ecore:EPackage>`;

  const generated = await generateArtifactsFromEcore({
    ecoreContent: ecore,
    modelContent: '<root/>',
    packageName: 'demo.search',
    className: 'GeneratedSearch'
  });

  assert.equal(generated.success, true);
  assert.ok(generated.scriptPath.includes('GeneratedSearch.momot'));
  assert.ok(generated.generatedFiles['model/generated.ecore']);
  assert.ok(generated.generatedFiles['model/generated.henshin']);

  const validation = validateGeneratedScenario({
    generatedFiles: generated.generatedFiles,
    scriptPath: generated.scriptPath,
    henshinPath: 'model/generated.henshin',
    ecorePath: 'model/generated.ecore',
    modelPath: 'model/input/model/model.xmi'
  });

  assert.equal(validation.valid, true);
});

test('buildJobZip uses Linux-compatible entry paths', async () => {
  const zipBuffer = await buildJobZip({
    'src\\demo\\Search.momot': Buffer.from('search = {}', 'utf8').toString('base64'),
    'model\\input\\model\\model.xmi': Buffer.from('<xmi/>', 'utf8').toString('base64')
  });

  const zip = await JSZip.loadAsync(zipBuffer);
  const entries = Object.keys(zip.files).filter((name) => !zip.files[name].dir).sort();
  assert.deepEqual(entries, ['model/input/model/model.xmi', 'src/demo/Search.momot']);
});

test('parseResponseZip extracts exit code, outputs, and log tail', async () => {
  const zip = new JSZip();
  zip.file('runner/exit_code.txt', '0');
  zip.file('runner/request.json', JSON.stringify({ script: 'src/demo/Search.momot' }));
  zip.file('runner/runner.log', 'line1\nline2\nline3');
  zip.file('out/models/result.xmi', '<xmi/>');

  const response = await parseResponseZip(await zip.generateAsync({ type: 'nodebuffer' }), 2);
  assert.equal(response.exitCode, 0);
  assert.equal(response.request.script, 'src/demo/Search.momot');
  assert.deepEqual(response.outputs, ['out/models/result.xmi']);
  assert.equal(response.logTail, 'line2\nline3');
});
