import { z } from 'zod';
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { executeMomotJob, generateArtifactsFromEcore, runEndToEnd, buildKnownGoodStackFixture } from './lib.js';

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

// Backward-compatible aliases.
server.tool(
  'momot_generate',
  {
    prompt: z.string().optional(),
    packageName: z.string().optional(),
    className: z.string().optional(),
    modelPath: z.string(),
    henshinModules: z.array(z.string()).default([])
  },
  async ({ prompt, packageName, className, modelPath, henshinModules }) => {
    const lines = [];
    if (prompt) {
      lines.push(`// ${prompt}`);
    }
    if (className) {
      lines.push(`// scaffold for ${className}`);
    }
    lines.push(`package ${packageName || 'momot.search'}`);
    lines.push('');
    lines.push('search = {');
    lines.push('  model = {');
    lines.push(`    file = "${modelPath}"`);
    lines.push('  }');
    lines.push('  transformations = {');
    lines.push(`    modules = [ ${henshinModules.map((m) => `"${m}"`).join(', ')} ]`);
    lines.push('  }');
    lines.push('}');
    return { content: [{ type: 'text', text: lines.join('\n') }] };
  }
);

server.tool('momot_validate', { scriptContent: z.string() }, async ({ scriptContent }) => {
  return {
    content: [{ type: 'text', text: JSON.stringify({ valid: scriptContent.trim().length > 0 }, null, 2) }]
  };
});

server.tool(
  'momot_run',
  {
    scriptPath: z.string().optional().default('job.momot'),
    scriptContent: z.string(),
    filesBase64: z.record(z.string()).default({}),
    restBaseUrl: z.string().optional()
  },
  async ({ scriptPath, scriptContent, filesBase64, restBaseUrl }) => {
    const runFiles = {
      ...filesBase64,
      [scriptPath]: Buffer.from(scriptContent, 'utf8').toString('base64')
    };
    const result = await executeMomotJob({ restBaseUrl, scriptPath, filesBase64: runFiles });
    return { content: [{ type: 'text', text: JSON.stringify(result, null, 2) }] };
  }
);

await server.connect(new StdioServerTransport());
