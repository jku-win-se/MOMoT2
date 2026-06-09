# MOMoT — Headless REST Runner & Agent Tooling

[![Project Page](https://img.shields.io/badge/docs-GitHub%20Pages-blue)](https://jku-win-se.github.io/MOMoT2/)

MOMoT combines model transformation (EMF/Henshin) with search-based optimization to solve complex model-driven engineering tasks.

**Project page:** https://jku-win-se.github.io/MOMoT2/

---

## Repository branches

| Branch | Purpose | Get started |
| --- | --- | --- |
| **`standalone`** (this branch) | **Headless REST** runner, Docker, MCP server, E2E test suite | [Quick start](#quick-start) · [AGENTS.md](AGENTS.md) |
| **`main`** | Full **Eclipse IDE** distribution, all case-study examples, [update site](https://jku-win-se.github.io/MOMoT2/eclipse/updates/latest/develop/) | [Clone `main`](https://github.com/jku-win-se/MOMoT2/tree/main) · [README on main](https://github.com/jku-win-se/MOMoT2/blob/main/README.md) |

See the [branches guide](https://jku-win-se.github.io/MOMoT2/branches.html) on the project site for a detailed comparison.

---

## `standalone` — What this branch is

The **`standalone`** branch is a container-first, headless distribution. It omits the full Eclipse example wizards and IDE packaging in favor of:

- **Docker REST server** — zip-in / zip-out job execution (`POST /run`)
- **MCP server** (`mcp/`) — stdio JSON-RPC bridge for LLM agents
- **E2E test suite** (`test-suite/`) — four verified benchmarks (T01–T04)
- **Henshin validator CLI** (`tools/henshin-validator/`) — local rule validation without Docker
- **Minimal stack example** (`stack-example-minimal/`) — deterministic smoke test
- **Agent playbook** — [AGENTS.md](AGENTS.md) documents the full agent workflow

Use **`main`** if you need the Eclipse update site, UI plugins, or the full set of wizard-based case studies.

---

## Quick start

### Build and run the REST server

```bash
git clone -b standalone https://github.com/jku-win-se/MOMoT2.git
cd MOMoT2
docker build -t momot-headless -f Dockerfile.headless .
docker run --rm -p 8080:8080 momot-headless
```

Health check:

```text
http://localhost:8080/health
```

Swagger / OpenAPI:

```text
http://localhost:8080/docs
http://localhost:8080/openapi.json
```

### Smoke test (recommended)

Linux/macOS:

```bash
./scripts/run-minimal-rest-test.sh
```

Windows PowerShell:

```powershell
./scripts/run-minimal-rest-test.ps1
```

The script builds the image (unless skipped), starts the container, posts a deterministic stack-balancing job, and asserts `exit_code == 0`.

### MCP server

```bash
cd mcp
npm install
node server.js   # stdio JSON-RPC; relays to REST runner over HTTP
```

See [mcp/README.md](mcp/README.md) for tool schemas (`generate_artifacts_from_ecore`, `execute_momot_job`, `run_end_to_end`).

---

## Repository structure

| Path | Description |
| --- | --- |
| `plugins/` | MOMoT core, MOEA bridge, configuration language, headless runner |
| `headless/` | Headless runtime modules |
| `mcp/` | MCP server (Node.js, stdio transport) |
| `stack-example-minimal/` | Canonical stack load-balancing fixture |
| `test-suite/` | E2E benchmarks with expected Pareto fronts |
| `headless-example/` | REST-ready example job payloads |
| `tools/henshin-validator/` | CLI validator for `.henshin` rules |
| `doc/` | Architecture docs and validation runbooks |
| `Dockerfile.headless` | Production headless image (recommended) |
| `Dockerfile` | Alternate REST image build |

---

## REST API (summary)

| Endpoint | Description |
| --- | --- |
| `GET /health` | Readiness check |
| `POST /run?script=<path.momot>` | Execute job (body = raw `application/zip`) |

Important:

1. Request body must be a ZIP containing model files, `.henshin` rules, and the `.momot` script.
2. The `script` query parameter must exactly match a path inside the uploaded ZIP.
3. Response is a ZIP with `runner/exit_code.txt`, `runner/runner.log`, and `out/` artifacts.

---

## Documentation

| Document | Content |
| --- | --- |
| [AGENTS.md](AGENTS.md) | Agent playbook — MCP tools, validation tiers, repair loop |
| [doc/00-architecture-overview.md](doc/00-architecture-overview.md) | REST, MCP, Docker topology |
| [doc/08-validation-and-runbook.md](doc/08-validation-and-runbook.md) | Full validation runbook |
| [doc/09-minimal-test-case.md](doc/09-minimal-test-case.md) | Stack example walkthrough |
| [test-suite/README.md](test-suite/README.md) | Benchmark suite guide |
| [mcp/README.md](mcp/README.md) | MCP tool reference |
| [tools/henshin-validator/README.md](tools/henshin-validator/README.md) | CLI validator usage |

---

## `main` — Eclipse IDE distribution

The **`main`** branch provides the full Eclipse product:

- Install via update site: `https://jku-win-se.github.io/MOMoT2/eclipse/updates/latest/develop/`
- All case-study examples with IDE wizards (stack, CRA, modularization, TSE, …)
- GitHub Pages site with case-study documentation

```bash
git clone -b main https://github.com/jku-win-se/MOMoT2.git
cd MOMoT2
mvn clean install
```

See [README on main](https://github.com/jku-win-se/MOMoT2/blob/main/README.md) and [MIGRATION.md](https://github.com/jku-win-se/MOMoT2/blob/main/MIGRATION.md).

---

## Authors

MOMoT was developed by Martin Fleck ([@martin-fleck](https://github.com/martin-fleck)), Javier Troya ([@javitroya](https://github.com/javitroya)), and Manuel Wimmer ([@manuelWimmer](https://github.com/manuelWimmer)).

The MCP server and agent integration on the **standalone** branch were developed by MohammadHadi Dehghani ([@hadiDHD](https://github.com/hadiDHD)).

The MCP server and agent integration on the **standalone** branch were developed by MohammadHadi Dehghani ([@hadiDHD](https://github.com/hadiDHD)).
