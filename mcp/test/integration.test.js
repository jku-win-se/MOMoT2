import test from 'node:test';
import assert from 'node:assert/strict';
import { buildKnownGoodStackFixture, executeMomotJob } from '../lib.js';

test('known-good stack fixture returns exit code 0', { timeout: 240000 }, async () => {
  if (process.env.RUN_INTEGRATION_TESTS !== '1') {
    return;
  }

  const fixture = await buildKnownGoodStackFixture();
  const result = await executeMomotJob({
    restBaseUrl: process.env.MOMOT_REST_BASE_URL || 'http://localhost:8080',
    scriptPath: fixture.scriptPath,
    filesBase64: fixture.filesBase64,
    requestTimeoutMs: 240000,
    retries: 1,
    retryDelayMs: 500,
    logTailLines: 60
  });

  assert.equal(result.success, true, result.summary);
  assert.equal(result.exitCode, 0, result.logTail || 'Missing log tail');
  assert.ok(Array.isArray(result.outputs), 'Expected outputs list in result envelope');
});
