# MOMoT MCP — Headless REST Runner & Model Context Protocol Platform

[![Project Page](https://img.shields.io/badge/docs-GitHub%20Pages-blue)](https://jku-win-se.github.io/MOMoT-MCP/)
[![License](https://img.shields.io/badge/license-MIT%2FEPL-green.svg)](#)

MOMoT-MCP combines model transformation (EMF/Henshin) with search-based optimization ([MOEA Framework 5.1](https://github.com/MOEAFramework/MOEAFramework/releases/tag/v5.1)) to solve complex model-driven engineering tasks. This repository hosts the container-first, headless distribution of MOMoT, featuring a robust **Model Context Protocol (MCP)** server and automated agent pipelines designed specifically for LLM-driven optimization and synthesis.

**Project Documentation & Guides:** https://jku-win-se.github.io/MOMoT-MCP/

---

## Key Capabilities

- **Docker REST Server:** Fast zip-in / zip-out job execution (`POST /run`) to compile and solve scripts without any Eclipse IDE dependencies.
- **Model Context Protocol (MCP) Server:** Stdio JSON-RPC bridge that allows LLM agents (like Cursor, Claude, etc.) to perform artifact generation, run local validations, and launch Docker-backed optimization jobs.
- **Smart Agent Coordinator System:** Autonomous human-in-the-loop (HITL) workflow that enables agents to generate metamodels, create instance profiles, write Henshin rules, compose MOMoT scripts, and repair compile/logic errors iteratively.
- **Local CLI Validators:** Lightweight Node-based wrappers for Henshin, MOMoT, Ecore, and XMI that validate files instantly without starting Docker.
- **E2E Test & Benchmark Suite:** Five verified multi-objective benchmarks (T01–T05) with expected Pareto fronts for regression and verification checks.

> ℹ️ **Looking for the Eclipse IDE plugin?** If you need the full Eclipse update site, UI plugins, or the full set of graphical wizards, please visit the parent repository at [jku-win-se/MOMoT2](https://github.com/jku-win-se/MOMoT2).

---

## Quick Start

### 1. Run the Headless REST Runner (Docker)

```bash
git clone https://github.com/jku-win-se/MOMoT-MCP.git
cd MOMoT-MCP
docker build -t momot-headless -f Dockerfile.headless .
docker run --rm -p 8080:8080 momot-headless
```

- **Health Check:** `http://localhost:8080/health`
- **Interactive API Docs (Swagger):** `http://localhost:8080/docs`

### 2. Run the Smoke Test

Verify the setup locally using our automated test script:

- **Linux / macOS:** `./scripts/run-minimal-rest-test.sh`
- **Windows PowerShell:** `./scripts/run-minimal-rest-test.ps1`

The script starts the container, posts a deterministic stack-balancing job, and asserts execution completion.

### 3. Start the MCP Server

```bash
cd mcp
npm install
node server.js
```

The MCP server listens on stdio (JSON-RPC) and translates LLM agent tool calls into local CLI validations and remote REST container executions.

---

## MCP Tool Surface

MOMoT-MCP exposes 12 highly specialized tools to LLM agents:

| Tool Name | Description |
|-----------|-------------|
| `detect_artifacts` | Scans workspace and user prompts, outputting an ordered Generation Plan. |
| `generate_ecore` | Creates structurally and semantically valid `.ecore` metamodels. |
| `generate_xmi` | Creates valid initial `.xmi` model instances matching any Ecore metamodel. |
| `generate_henshin` | Generates valid `.henshin` transformation rules based on any Ecore metamodel. |
| `generate_momot` | Composes a declarative `.momot` search script wiring rules, models, and objectives. |
| `generate_java_helper` | Builds custom Java fitness modules for complex, non-OCL metrics. |
| `validate_ecore` | Semantically and structurally validates `.ecore` metamodels. |
| `validate_xmi` | Programmatically and semantically validates `.xmi` model instances. |
| `validate_henshin` | Structural, semantic, and dry-run execution validator for Henshin rules. |
| `validate_momot` | Structural, semantic, and compiler check validator for MOMoT scripts. |
| `validate_java_helper` | Verifies packages, structural signatures, and inheritance of custom Java helpers. |
| `execute_momot_job` | Compiles and executes a complete ZIP payload on the Docker REST runner. |

Refer to [mcp/README.md](mcp/README.md) for full JSON schemas and payload examples.

---

## Directory Layout

| Path | Description |
| --- | --- |
| `mcp/` | Model Context Protocol server (Node.js, stdio) |
| `docs/` | Comprehensive architectural wikis, playbooks, and runbooks (GitHub Pages) |
| `tools/` | CLI validators for `.henshin`, `.momot`, `.ecore`, and `.xmi` files |
| `test-suite/` | E2E benchmarks (T01-T05) with reference Pareto fronts |
| `stack-example-minimal/` | Canonical stack load-balancing example payload |
| `plugins/` | Core MOMoT compiler, MOEA 5.1 bridge, and headless runner modules |
| `headless/` | Headless runtime wrapper modules |
| `Dockerfile.headless` | Primary production REST headless Docker image |

---

## REST API Summary

| Endpoint | Method | Input | Output |
|---|---|---|---|
| `/health` | `GET` | None | `{ "status": "UP", "health": { "ok": true } }` |
| `/run?script=<path>` | `POST` | Raw Binary `application/zip` | Job completion zip package |

1. **Upload format:** Requests to `/run` must send the zip package as the raw binary request body.
2. **Script parameter:** The `script` query parameter must exactly match the relative path of the `.momot` script inside the uploaded zip.
3. **Response package:** Returns a zip containing the execution output directory (`out/`), compiler logs (`runner/runner.log`), and exit status (`runner/exit_code.txt`).

---

## Authors & Contributors

MOMoT is authored by Martin Fleck ([@martin-fleck](https://github.com/martin-fleck)), Javier Troya ([@javitroya](https://github.com/javitroya)), and Manuel Wimmer ([@manuelWimmer](https://github.com/manuelWimmer)).

The headless REST runner, MCP server, automated local validators, E2E benchmark harness, and the agent coordinator system on the **MOMoT-MCP** platform were developed by MohammadHadi Dehghani ([@hadiDHD](https://github.com/hadiDHD)).
