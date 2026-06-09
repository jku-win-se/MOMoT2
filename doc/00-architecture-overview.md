# Architecture Overview

This document provides a visual overview of the MOMoT 2.0 standalone architecture, covering the runtime topology, communication protocol, MCP tooling, build modules, and Docker image structure.

## Standalone Architecture

The standalone deployment has three layers: an LLM or user client communicates with the MCP server over stdio (JSON-RPC), the MCP server translates tool calls into HTTP requests against the REST server running inside a Docker container, and the REST server compiles and executes `.momot` scripts via the MOMoT engine.

![Architecture Overview](../images/20260410_architecture_overview.png)

The REST server exposes four endpoints: `/health` for liveness checks, `/docs` for interactive Swagger UI, `/openapi.json` for the OpenAPI spec, and `/run` for job execution. The MOMoT engine auto-registers Ecore packages from the working directory at startup.

## Zip-in / Zip-out Protocol

All job execution follows a zip-in / zip-out protocol. The client packages model files, Henshin rules, and the `.momot` script into a ZIP archive, sends it as a binary POST to `/run`, and receives a ZIP response containing the exit code, logs, and output artifacts.

![Zip Protocol Sequence](../images/20260410_zip_protocol_sequence.png)

The response ZIP always contains `runner/exit_code.txt` (0 on success), `runner/request.json` (execution metadata), `runner/runner.log`, and optionally `runner/compile.log`. Output artifacts produced by the search appear under `out/`.

For the full validation runbook, see [08-validation-and-runbook.md](08-validation-and-runbook.md).

## MCP Server Tools

The MCP server (`mcp/server.js`, v1.1.0) exposes three public tools: `generate_artifacts_from_ecore`, `execute_momot_job`, and `run_end_to_end`.

![MCP Tools](../images/20260410_mcp_tools.png)

The key tool is `execute_momot_job`, which builds the ZIP payload, checks REST health, posts to `/run`, and parses the response. `run_end_to_end` composes `generate_artifacts_from_ecore` and `execute_momot_job` into a single call. Setting `knownGoodFixture=true` on `run_end_to_end` runs the stack example for smoke testing.

## Maven Build Modules

The Docker build compiles a subset of Maven modules. The `momot.runner` module (packaging: jar) is the REST/CLI entrypoint. It depends on `momot.core` and `big.moea` at compile time, and on `momot.lang` (packaging: eclipse-plugin) only for its jar artifact copied into the runtime classpath. The `momot.tooling` module provides the Eclipse target platform definition needed by `momot.lang`.

![Maven Modules](../images/20260410_maven_modules.png)

Note: `momot.lang` uses pre-generated Xtend sources from `xtend-gen/` and is built with `-Dxtend.skip=true` to avoid requiring a full Tycho target platform resolution during the Docker build.

## Docker Image Structure

The Docker image uses a two-stage multi-stage build. Stage 1 (BUILD) compiles the Maven modules, copies jars, downloads external dependencies (OCL, Henshin, Nashorn, ASM), and strips jar signatures. Stage 2 (RUNTIME) copies the assembled plugin directory onto a clean `eclipse-temurin:21-jdk` base and sets the entrypoint.

![Docker Build Stages](../images/20260410_docker_layers.png)

The base image is pinned to `maven:3.9-eclipse-temurin-21` to avoid class file version incompatibilities with the Xtext/ASM toolchain.

## Diagram source

All diagrams are generated from PlantUML sources embedded in `local-scripts/Generate-Diagrams.ps1` and rendered via a local [Kroki](https://kroki.io/) instance. To regenerate:

```powershell
docker run -d --name kroki-temp -p 8084:8000 yuzutech/kroki
.\local-scripts\Generate-Diagrams.ps1
docker rm -f kroki-temp
```
