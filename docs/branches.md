---
title: Repository Branches
layout: index
---

### Overview

[jku-win-se/MOMoT2](https://github.com/jku-win-se/MOMoT2) hosts two active development lines. Choose the branch that matches how you want to work with MOMoT.

| | **`main`** | **`standalone`** |
| --- | --- | --- |
| **Purpose** | Full Eclipse IDE distribution | Headless REST runner + AI/agent tooling |
| **Primary audience** | Eclipse developers, researchers reproducing case studies | CI pipelines, Docker deployments, LLM agents |
| **Install** | [Eclipse update site]({{ site.baseurl }}/eclipse/updates/) | `docker build` + REST `/run` API |
| **Examples** | All wizard-based and TSE examples in-repo | `stack-example-minimal`, `test-suite`, `headless-example` |
| **Build** | `mvn clean install` (full Tycho reactor) | `docker build -f Dockerfile.headless .` |
| **Docs** | This site, case studies, [MIGRATION.md](https://github.com/jku-win-se/MOMoT2/blob/main/MIGRATION.md) | [AGENTS.md](https://github.com/jku-win-se/MOMoT2/blob/standalone/AGENTS.md), `doc/` runbooks |

### `main` — Full Eclipse distribution

The **`main`** branch is the complete MOMoT Eclipse product:

- **Plugins & features** — core engine, MOEA integration, MOMoT configuration language (Xtext), UI, branding, example wizards
- **Examples** — stack balancing, CRA, class modularization, EMF refactor, restructuring, TSE benchmarks, and more
- **Update site** — published from `docs/` to GitHub Pages; install in Eclipse via **Help → Install New Software…**
- **Migration notes** — see [MIGRATION.md](https://github.com/jku-win-se/MOMoT2/blob/main/MIGRATION.md) for the Eclipse 2026-03 / Tycho 4.x modernization

**Clone and build:**

```bash
git clone -b main https://github.com/jku-win-se/MOMoT2.git
cd MOMoT2
mvn clean install
```

**Eclipse update site URL:**

`{{ site.url }}{{ site.baseurl }}/eclipse/updates/latest/develop/`

After a local build, publish a refreshed site with:

```bash
bash scripts/deploy.sh
```

### `standalone` — Headless REST + MCP

The **`standalone`** branch is a slim, container-first distribution for running MOMoT without the Eclipse IDE:

- **Docker REST runner** — zip-in / zip-out job execution on `POST /run`
- **MCP server** (`mcp/`) — bridges LLM tool calls to the REST runner over stdio JSON-RPC
- **Test suite** — four end-to-end benchmarks (`test-suite/T01`–`T04`) with Henshin validation tiers
- **Henshin validator CLI** — fast local rule validation without Docker (`tools/henshin-validator/`)
- **Agent playbook** — [AGENTS.md](https://github.com/jku-win-se/MOMoT2/blob/standalone/AGENTS.md) documents the full agent workflow

**Quick start:**

```bash
git clone -b standalone https://github.com/jku-win-se/MOMoT2.git
cd MOMoT2
docker build -t momot-headless -f Dockerfile.headless .
docker run -p 8080:8080 momot-headless
curl http://localhost:8080/health
```

**Smoke test (stack example):**

```bash
./scripts/run-minimal-rest-test.sh   # Linux/macOS
./scripts/run-minimal-rest-test.ps1  # Windows
```

**MCP server:**

```bash
cd mcp && npm install && node server.js
```

Key documentation on `standalone`:

- [doc/00-architecture-overview.md](https://github.com/jku-win-se/MOMoT2/blob/standalone/doc/00-architecture-overview.md) — REST, MCP, Docker topology
- [doc/08-validation-and-runbook.md](https://github.com/jku-win-se/MOMoT2/blob/standalone/doc/08-validation-and-runbook.md) — validation runbook
- [mcp/README.md](https://github.com/jku-win-se/MOMoT2/blob/standalone/mcp/README.md) — MCP tool reference
- [test-suite/README.md](https://github.com/jku-win-se/MOMoT2/blob/standalone/test-suite/README.md) — benchmark suite

### Which branch should I use?

| Goal | Branch |
| --- | --- |
| Install MOMoT as Eclipse plugins and run examples from the IDE | **`main`** |
| Publish or consume the Eclipse update site | **`main`** |
| Run MOMoT in Docker / CI without Eclipse | **`standalone`** |
| Integrate MOMoT with an LLM agent via MCP | **`standalone`** |
| Validate Henshin rules locally with the CLI validator | **`standalone`** |
| Reproduce published case studies with wizards | **`main`** |

Both branches share the same core search engine concepts (EMF, Henshin, MOEA, `.momot` scripts). The **`standalone`** branch trims the Eclipse UI and example surface area in favor of headless execution; **`main`** retains the full research distribution and GitHub Pages site.
