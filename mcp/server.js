import { z } from 'zod';
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { executeMomotJob, generateArtifactsFromEcore, runEndToEnd, buildKnownGoodStackFixture, validateHenshin } from './lib.js';

const server = new McpServer({ name: 'momot-mcp', version: '1.1.0' });

const generationSchema = {
  ecoreContent: z.string().optional(),
  ecorePath: z.string().optional(),
  modelContent: z.string().optional(),
  modelPath: z.string().optional(),
  problemDescription: z.string().optional(),
  objectiveHints: z.array(z.string()).default([]),
  packageName: z.string().optional(),
  className: z.string().optional(),
  scriptPath: z.string().optional(),
  henshinPath: z.string().optional(),
  ecorePathInZip: z.string().optional(),
  modelPathInZip: z.string().optional(),
  includeJavaHelper: z.boolean().default(false),
  helperPathInZip: z.string().optional(),
  allowMissingModelForGeneration: z.boolean().default(false)
};

const executeSchema = {
  restBaseUrl: z.string().optional(),
  scriptPath: z.string(),
  filesBase64: z.record(z.string()),
  requestTimeoutMs: z.number().int().positive().optional(),
  retries: z.number().int().nonnegative().optional(),
  retryDelayMs: z.number().int().nonnegative().optional(),
  logTailLines: z.number().int().positive().optional()
};

server.tool('generate_artifacts_from_ecore', generationSchema, async (input) => {
  if (!input.ecoreContent && !input.ecorePath) {
    throw new Error('Provide either ecoreContent or ecorePath.');
  }
  const result = await generateArtifactsFromEcore(input);
  return { content: [{ type: 'text', text: JSON.stringify(result, null, 2) }] };
});

server.tool('execute_momot_job', executeSchema, async (input) => {
  const result = await executeMomotJob(input);
  return { content: [{ type: 'text', text: JSON.stringify(result, null, 2) }] };
});

server.tool(
  'run_end_to_end',
  {
    ...generationSchema,
    restBaseUrl: z.string().optional(),
    requestTimeoutMs: z.number().int().positive().optional(),
    retries: z.number().int().nonnegative().optional(),
    retryDelayMs: z.number().int().nonnegative().optional(),
    logTailLines: z.number().int().positive().optional(),
    knownGoodFixture: z.boolean().default(false)
  },
  async (input) => {
    let result;
    if (input.knownGoodFixture) {
      const fixture = await buildKnownGoodStackFixture();
      result = await executeMomotJob({
        restBaseUrl: input.restBaseUrl,
        scriptPath: fixture.scriptPath,
        filesBase64: fixture.filesBase64,
        requestTimeoutMs: input.requestTimeoutMs,
        retries: input.retries,
        retryDelayMs: input.retryDelayMs,
        logTailLines: input.logTailLines
      });
    } else {
      if (!input.ecoreContent && !input.ecorePath) {
        throw new Error('Provide either ecoreContent or ecorePath, or set knownGoodFixture=true.');
      }
      result = await runEndToEnd(input);
    }

    return { content: [{ type: 'text', text: JSON.stringify(result, null, 2) }] };
  }
);

server.tool(
  'validate_henshin',
  {
    henshinPath: z.string().describe('Path to the .henshin file (absolute or CWD-relative).'),
    mode: z.enum(['structure', 'semantic', 'apply']).default('structure').describe(
      '"structure": XMI parse only, no metamodel needed. ' +
      '"semantic": resolve type refs against metamodel. ' +
      '"apply": execute a rule against a model instance.'
    ),
    metamodelPath: z.string().optional().describe('Path to the .ecore file (required for semantic and apply modes).'),
    modelPath: z.string().optional().describe('Path to the .xmi model instance (required for apply mode).'),
    ruleName: z.string().optional().describe('Name of the rule to apply (required for apply mode).'),
    parameters: z.record(z.string()).default({}).describe('Rule parameter values as a string map, e.g. { "amount": "3" }.')
  },
  async (input) => {
    const result = await validateHenshin(input);
    return { content: [{ type: 'text', text: JSON.stringify(result, null, 2) }] };
  }
);

await server.connect(new StdioServerTransport());
