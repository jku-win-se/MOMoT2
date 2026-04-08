import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import { setTimeout as delay } from 'node:timers/promises';
import { fileURLToPath } from 'node:url';
import JSZip from 'jszip';

const DEFAULT_REST_BASE_URL = 'http://localhost:8080';
const DEFAULT_RETRY_COUNT = 2;
const DEFAULT_RETRY_DELAY_MS = 500;
const DEFAULT_REQUEST_TIMEOUT_MS = 120000;
const DEFAULT_LOG_TAIL_LINES = 40;

export async function generateArtifactsFromEcore(input) {
  const ecoreContent = loadEcoreContent(input);
  const modelInfo = parseEcoreSummary(ecoreContent);

  const packageName = sanitizeJavaPackage(input.packageName || 'generated.momot.search');
  const className = sanitizeJavaIdentifier(input.className || 'GeneratedSearch');
  const scriptPath = normalizeZipPath(
    input.scriptPath || `src/${packageName.replace(/\./g, '/')}/${className}.momot`
  );
  const henshinPath = normalizeZipPath(input.henshinPath || 'model/generated.henshin');
  const ecorePath = normalizeZipPath(input.ecorePathInZip || 'model/generated.ecore');
  const modelPath = normalizeZipPath(input.modelPathInZip || 'model/input/model/model.xmi');
  const helperPath = normalizeZipPath(
    input.helperPathInZip || `src/${packageName.replace(/\./g, '/')}/${className}Helper.java`
  );

  const warnings = [];
  if (modelInfo.classNames.length === 0) {
    warnings.push('No EClass declarations found in Ecore; generated script uses conservative defaults.');
  }
  if (input.modelContent == null && input.modelPath == null && !input.allowMissingModelForGeneration) {
    warnings.push('No model instance provided. Execution will fail unless a valid model file is added.');
  }

  const objectiveText = Array.isArray(input.objectiveHints) && input.objectiveHints.length > 0
    ? input.objectiveHints.join('; ')
    : (input.problemDescription || 'Generated optimization scenario');

  const scriptContent = buildMomotScript({
    packageName,
    modelPath,
    henshinPath,
    objectiveText
  });

  const henshinContent = buildHenshinModule(modelInfo);
  const helperContent = input.includeJavaHelper
    ? buildJavaHelper({ packageName, className, objectiveText })
    : null;

  const generatedFiles = {
    [ecorePath]: Buffer.from(ecoreContent, 'utf8').toString('base64'),
    [henshinPath]: Buffer.from(henshinContent, 'utf8').toString('base64'),
    [scriptPath]: Buffer.from(scriptContent, 'utf8').toString('base64')
  };

  if (input.modelContent != null) {
    generatedFiles[modelPath] = Buffer.from(input.modelContent, 'utf8').toString('base64');
  } else if (input.modelPath != null) {
    const fileContent = fs.readFileSync(path.resolve(input.modelPath), 'utf8');
    generatedFiles[modelPath] = Buffer.from(fileContent, 'utf8').toString('base64');
  }

  if (helperContent != null) {
    generatedFiles[helperPath] = Buffer.from(helperContent, 'utf8').toString('base64');
  }

  const validation = validateGeneratedScenario({ generatedFiles, scriptPath, henshinPath, ecorePath, modelPath });

  return {
    success: validation.valid,
    warnings: [...warnings, ...validation.warnings],
    summary: validation.valid
      ? `Generated ${Object.keys(generatedFiles).length} artifacts for package ${packageName}.`
      : 'Generated artifacts contain validation issues.',
    scriptPath,
    generatedFiles,
    modelInfo,
    diagnostics: validation.diagnostics
  };
}

export function validateGeneratedScenario({ generatedFiles, scriptPath, henshinPath, ecorePath, modelPath }) {
  const warnings = [];
  const diagnostics = {};

  for (const key of Object.keys(generatedFiles)) {
    try {
      normalizeZipPath(key);
    } catch (error) {
      diagnostics.invalidPath = error.message;
      return { valid: false, warnings, diagnostics };
    }
  }

  if (!generatedFiles[scriptPath]) {
    diagnostics.missingScript = `Missing script file at ${scriptPath}`;
    return { valid: false, warnings, diagnostics };
  }
  if (!generatedFiles[henshinPath]) {
    diagnostics.missingHenshin = `Missing Henshin module at ${henshinPath}`;
    return { valid: false, warnings, diagnostics };
  }
  if (!generatedFiles[ecorePath]) {
    diagnostics.missingEcore = `Missing Ecore file at ${ecorePath}`;
    return { valid: false, warnings, diagnostics };
  }
  if (!generatedFiles[modelPath]) {
    warnings.push(`Model path ${modelPath} is missing; execution may fail.`);
  }

  const scriptText = Buffer.from(generatedFiles[scriptPath], 'base64').toString('utf8');
  if (!scriptText.includes('search = {')) {
    diagnostics.invalidScript = 'Generated MOMoT script is missing search block.';
    return { valid: false, warnings, diagnostics };
  }
  if (!scriptText.includes(`file = "${modelPath}"`)) {
    warnings.push('Generated script does not reference expected model path.');
  }

  return { valid: true, warnings, diagnostics };
}

export async function executeMomotJob(input) {
  const restBaseUrl = normalizeBaseUrl(input.restBaseUrl || process.env.MOMOT_REST_BASE_URL || DEFAULT_REST_BASE_URL);
  const scriptPath = normalizeZipPath(input.scriptPath);
  const requestTimeoutMs = ensureInt(input.requestTimeoutMs, DEFAULT_REQUEST_TIMEOUT_MS);
  const retries = ensureInt(input.retries, DEFAULT_RETRY_COUNT);
  const retryDelayMs = ensureInt(input.retryDelayMs, DEFAULT_RETRY_DELAY_MS);
  const logTailLines = ensureInt(input.logTailLines, DEFAULT_LOG_TAIL_LINES);

  const filesBase64 = { ...(input.filesBase64 || {}) };
  if (!filesBase64[scriptPath]) {
    throw new Error(`Missing required script file in filesBase64: ${scriptPath}`);
  }

  const zipPayload = await buildJobZip(filesBase64);
  const diagnostics = {};
  const health = await checkRestHealth({ restBaseUrl, requestTimeoutMs, retries, retryDelayMs });
  diagnostics.health = health;

  if (!health.ok) {
    return {
      success: false,
      exitCode: -1,
      scriptPath,
      generatedFiles: Object.keys(filesBase64).sort(),
      warnings: [],
      summary: `REST endpoint unavailable at ${restBaseUrl}`,
      logTail: '',
      outputs: [],
      diagnostics: {
        ...diagnostics,
        rootCauseHint: 'REST unavailability. Start container and verify /health endpoint.'
      }
    };
  }

  const runResult = await postRunZip({ restBaseUrl, scriptPath, zipPayload, requestTimeoutMs, retries, retryDelayMs });
  const parsed = await parseResponseZip(runResult.responseZip, logTailLines);

  const rootCauseHint = deriveRootCauseHint(parsed);

  return {
    success: parsed.exitCode === 0,
    exitCode: parsed.exitCode,
    scriptPath,
    generatedFiles: Object.keys(filesBase64).sort(),
    warnings: parsed.warnings,
    summary: parsed.exitCode === 0
      ? `Execution succeeded with ${parsed.outputs.length} output artifact(s).`
      : `Execution failed with exit code ${parsed.exitCode}.`,
    logTail: parsed.logTail,
    outputs: parsed.outputs,
    diagnostics: {
      ...diagnostics,
      requestUrl: runResult.requestUrl,
      statusCode: runResult.statusCode,
      request: parsed.request,
      rootCauseHint
    }
  };
}

export async function runEndToEnd(input) {
  const generated = await generateArtifactsFromEcore(input);
  if (!generated.success) {
    return {
      success: false,
      exitCode: -1,
      scriptPath: generated.scriptPath,
      generatedFiles: Object.keys(generated.generatedFiles || {}).sort(),
      warnings: generated.warnings,
      summary: 'Artifact generation failed validation.',
      logTail: '',
      outputs: [],
      diagnostics: generated.diagnostics
    };
  }

  const execution = await executeMomotJob({
    restBaseUrl: input.restBaseUrl,
    scriptPath: generated.scriptPath,
    filesBase64: generated.generatedFiles,
    requestTimeoutMs: input.requestTimeoutMs,
    retries: input.retries,
    retryDelayMs: input.retryDelayMs,
    logTailLines: input.logTailLines
  });

  return {
    ...execution,
    warnings: [...generated.warnings, ...execution.warnings]
  };
}

export async function buildJobZip(filesBase64) {
  const zip = new JSZip();
  for (const entryPath of Object.keys(filesBase64).sort()) {
    const normalized = normalizeZipPath(entryPath);
    const data = Buffer.from(filesBase64[entryPath], 'base64');
    zip.file(normalized, data, { binary: true });
  }
  return zip.generateAsync({
    type: 'nodebuffer',
    compression: 'DEFLATE',
    compressionOptions: { level: 6 }
  });
}

export async function parseResponseZip(responseBuffer, logTailLines = DEFAULT_LOG_TAIL_LINES) {
  const zip = await JSZip.loadAsync(responseBuffer);
  const entries = Object.keys(zip.files).filter((name) => !zip.files[name].dir).sort();
  const getText = async (name) => {
    if (!zip.files[name]) {
      return null;
    }
    return zip.files[name].async('text');
  };

  const exitCodeRaw = await getText('runner/exit_code.txt');
  const requestRaw = await getText('runner/request.json');
  const runnerLog = await getText('runner/runner.log');
  const compileLog = await getText('runner/compile.log');

  const parsedRequest = safeParseJson(requestRaw);
  const exitCode = Number.isFinite(Number(exitCodeRaw)) ? Number(exitCodeRaw) : -1;
  const outputs = entries.filter((entry) => entry.startsWith('out/'));
  const warnings = [];
  if (!entries.includes('runner/exit_code.txt')) {
    warnings.push('Response zip is missing runner/exit_code.txt.');
  }
  if (!entries.includes('runner/runner.log')) {
    warnings.push('Response zip is missing runner/runner.log.');
  }

  const mergedLog = [compileLog || '', runnerLog || ''].filter(Boolean).join('\n');
  return {
    exitCode,
    request: parsedRequest,
    outputs,
    warnings,
    logTail: tailLines(mergedLog, logTailLines),
    allEntries: entries
  };
}

export function normalizeZipPath(entryPath) {
  if (typeof entryPath !== 'string' || entryPath.trim().length === 0) {
    throw new Error('Zip entry path must be a non-empty string.');
  }
  let normalized = entryPath.replace(/\\/g, '/').replace(/^\/+/, '');
  normalized = path.posix.normalize(normalized);
  if (normalized === '.' || normalized.startsWith('../') || normalized.includes('/../')) {
    throw new Error(`Path traversal is not allowed: ${entryPath}`);
  }
  if (normalized.includes(':')) {
    throw new Error(`Drive letters are not allowed in zip entry paths: ${entryPath}`);
  }
  return normalized;
}

export function parseEcoreSummary(ecoreContent) {
  const nsUriMatch = ecoreContent.match(/nsURI\s*=\s*"([^"]+)"/);
  const packageMatch = ecoreContent.match(/<[^>]*EPackage[^>]*\sname\s*=\s*"([^"]+)"/);
  const classNames = Array.from(ecoreContent.matchAll(/<[^>]*EClass[^>]*\sname\s*=\s*"([^"]+)"/g)).map((m) => m[1]);

  return {
    packageName: packageMatch ? packageMatch[1] : 'generated',
    nsURI: nsUriMatch ? nsUriMatch[1] : 'http://generated/1.0',
    classNames: unique(classNames)
  };
}

function loadEcoreContent(input) {
  if (input.ecoreContent != null) {
    if (input.ecoreContent.trim().length === 0) {
      throw new Error('ecoreContent is empty.');
    }
    return input.ecoreContent;
  }
  if (input.ecorePath != null) {
    const resolved = path.resolve(input.ecorePath);
    return fs.readFileSync(resolved, 'utf8');
  }
  throw new Error('Provide either ecoreContent or ecorePath.');
}

function buildMomotScript({ packageName, modelPath, henshinPath, objectiveText }) {
  return [
    `package ${packageName}`,
    '',
    'import at.ac.tuwien.big.momot.search.fitness.dimension.TransformationLengthDimension',
    '',
    'search = {',
    '   model = {',
    `      file = "${modelPath}"`,
    '   }',
    '   solutionLength = 8',
    '   transformations = {',
    `      modules = [ "${henshinPath}" ]`,
    '   }',
    '   fitness = {',
    '      objectives = {',
    `         Objective : minimize { 0.0 } // ${escapeInlineComment(objectiveText)}`,
    '         SolutionLength : minimize new TransformationLengthDimension',
    '      }',
    '   }',
    '   algorithms = {',
    '      Random : moea.createRandomSearch()',
    '   }',
    '}',
    '',
    'experiment = {',
    '   populationSize = 20',
    '   maxEvaluations = 200',
    '   nrRuns = 1',
    '}',
    '',
    'results = {',
    '   models = {',
    '      outputDirectory = "out/models/"',
    '   }',
    '   objectives = {',
    '      outputFile = "out/objectives.txt"',
    '      printOutput',
    '   }',
    '}',
    ''
  ].join('\n');
}

function buildHenshinModule(modelInfo) {
  const nsURI = modelInfo.nsURI || 'http://generated/1.0';
  const className = modelInfo.classNames[0] || 'Root';

  return [
    '<?xml version="1.0" encoding="UTF-8"?>',
    '<henshin:Module xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:henshin="http://www.eclipse.org/emf/2011/Henshin" name="Generated">',
    `  <imports href="${escapeXml(nsURI)}#/"/>`,
    `  <units xsi:type="henshin:Rule" name="noop_${escapeXml(className)}">`,
    '    <lhs/>',
    '    <rhs/>',
    '  </units>',
    '</henshin:Module>',
    ''
  ].join('\n');
}

function buildJavaHelper({ packageName, className, objectiveText }) {
  return [
    `package ${packageName};`,
    '',
    `public final class ${className}Helper {`,
    `  public static final String OBJECTIVE_HINT = "${escapeJavaString(objectiveText)}";`,
    '',
    `  private ${className}Helper() {`,
    '  }',
    '}',
    ''
  ].join('\n');
}

async function checkRestHealth({ restBaseUrl, requestTimeoutMs, retries, retryDelayMs }) {
  const healthUrl = `${restBaseUrl}/health`;
  for (let attempt = 0; attempt <= retries; attempt += 1) {
    try {
      const response = await fetchWithTimeout(healthUrl, { method: 'GET' }, requestTimeoutMs);
      if (response.ok) {
        return { ok: true, statusCode: response.status };
      }
      if (attempt === retries) {
        return { ok: false, statusCode: response.status, body: await response.text() };
      }
    } catch (error) {
      if (attempt === retries) {
        return { ok: false, error: String(error) };
      }
    }
    await delay(retryDelayMs);
  }
  return { ok: false, error: 'Unknown health check failure.' };
}

async function postRunZip({ restBaseUrl, scriptPath, zipPayload, requestTimeoutMs, retries, retryDelayMs }) {
  const requestUrl = `${restBaseUrl}/run?script=${encodeURIComponent(scriptPath)}`;
  let lastError = null;
  for (let attempt = 0; attempt <= retries; attempt += 1) {
    try {
      const response = await fetchWithTimeout(
        requestUrl,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/zip' },
          body: zipPayload
        },
        requestTimeoutMs
      );
      if (!response.ok) {
        const bodyText = await response.text();
        lastError = new Error(`REST /run failed with status ${response.status}: ${bodyText}`);
        if (attempt === retries) {
          throw lastError;
        }
      } else {
        const arrayBuffer = await response.arrayBuffer();
        return {
          requestUrl,
          statusCode: response.status,
          responseZip: Buffer.from(arrayBuffer)
        };
      }
    } catch (error) {
      lastError = error;
      if (attempt === retries) {
        throw error;
      }
    }
    await delay(retryDelayMs);
  }
  throw lastError || new Error('REST /run request failed.');
}

async function fetchWithTimeout(url, options, timeoutMs) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    return await fetch(url, { ...options, signal: controller.signal });
  } finally {
    clearTimeout(timer);
  }
}

function deriveRootCauseHint(parsed) {
  const log = (parsed.logTail || '').toLowerCase();
  if (parsed.exitCode === 0) {
    return 'Execution succeeded.';
  }
  if (log.includes('script not found in uploaded archive')) {
    return 'Payload and script query mismatch. Ensure script path equals zip entry path.';
  }
  if (log.includes('compilation') || log.includes('error:')) {
    return 'Generated MOMoT/Java compile issue. Inspect compile.log and script syntax.';
  }
  if (log.includes('noclassdeffounderror') || log.includes('classnotfoundexception')) {
    return 'Runtime classpath gap. Verify Docker image dependencies.';
  }
  if (log.includes('epackage') || log.includes('nsuri') || log.includes('cannot create resource')) {
    return 'Metamodel or model URI mismatch. Verify Ecore nsURI and model references.';
  }
  return 'Algorithm/runtime semantic issue. Inspect runner.log for failing phase.';
}

function normalizeBaseUrl(baseUrl) {
  return String(baseUrl).replace(/\/+$/, '');
}

function ensureInt(value, fallback) {
  if (value == null) {
    return fallback;
  }
  const parsed = Number(value);
  if (!Number.isFinite(parsed) || parsed < 0) {
    return fallback;
  }
  return Math.floor(parsed);
}

function tailLines(text, lineCount) {
  const lines = String(text || '').split(/\r?\n/);
  return lines.slice(Math.max(0, lines.length - lineCount)).join('\n').trim();
}

function safeParseJson(value) {
  if (!value) {
    return null;
  }
  try {
    return JSON.parse(value);
  } catch {
    return { raw: value };
  }
}

function unique(values) {
  return Array.from(new Set(values));
}

function escapeXml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/"/g, '&quot;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

function escapeInlineComment(value) {
  return String(value || '').replace(/[\r\n]/g, ' ').replace(/\*\//g, '* /');
}

function escapeJavaString(value) {
  return String(value)
    .replace(/\\/g, '\\\\')
    .replace(/"/g, '\\"')
    .replace(/\r/g, '\\r')
    .replace(/\n/g, '\\n');
}

function sanitizeJavaPackage(value) {
  const cleaned = String(value)
    .split('.')
    .map((segment) => sanitizeJavaIdentifier(segment || 'generated').toLowerCase())
    .filter(Boolean)
    .join('.');
  return cleaned || 'generated.momot.search';
}

function sanitizeJavaIdentifier(value) {
  const raw = String(value || 'Generated').replace(/[^A-Za-z0-9_]/g, '_');
  const first = /^[A-Za-z_]/.test(raw) ? raw : `_${raw}`;
  return first.length > 0 ? first : 'Generated';
}

export async function buildKnownGoodStackFixture() {
  const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
  const scriptPath = 'src/at/ac/tuwien/big/momot/examples/stack/StackSearchExample.momot';
  const headlessRoot = path.join(repoRoot, 'headless-example', 'job-minimal');
  const useHeadlessFixture = fs.existsSync(path.join(headlessRoot, scriptPath));
  const fixtureRoot = useHeadlessFixture ? headlessRoot : path.join(repoRoot, 'stack-example-minimal');
  const modelRoot = useHeadlessFixture ? path.join(fixtureRoot, 'model') : path.join(fixtureRoot, 'model');
  const scriptRoot = fixtureRoot;
  const files = {
    'model/stack.ecore': fs.readFileSync(path.join(modelRoot, 'stack.ecore')).toString('base64'),
    'model/stack.henshin': fs.readFileSync(path.join(modelRoot, 'stack.henshin')).toString('base64'),
    'model/input/model/model_five_stacks.xmi': fs.readFileSync(
      path.join(modelRoot, 'input', 'model', 'model_five_stacks.xmi')
    ).toString('base64'),
    [scriptPath]: fs.readFileSync(
      path.join(scriptRoot, scriptPath)
    ).toString('base64')
  };

  return { scriptPath, filesBase64: files };
}

export function maybeCreateDebugDir(enabled) {
  if (!enabled) {
    return null;
  }
  const temp = path.join(os.tmpdir(), `momot-mcp-debug-${Date.now()}-${crypto.randomUUID()}`);
  fs.mkdirSync(temp, { recursive: true });
  return temp;
}
