# MOMoT — Marrying Search-based Optimization and Model Transformation Technology

[![Project Page](https://img.shields.io/badge/docs-GitHub%20Pages-blue)](https://jku-win-se.github.io/MOMoT2/)

MOMoT combines model-driven engineering (EMF/Henshin) with search-based optimization (MOEA Framework) to solve complex problems on the model level.

**Project page:** https://jku-win-se.github.io/MOMoT2/

---

## Repository branches

This repository has two active branches. They target different workflows but share the same core technology.

| Branch | Purpose | Get started |
| --- | --- | --- |
| **`main`** (this branch) | Full **Eclipse IDE** distribution — plugins, wizards, all case-study examples, [update site](https://jku-win-se.github.io/MOMoT2/eclipse/updates/latest/develop/) | [Install in Eclipse](#install-in-eclipse) · [Build](#build) |
| **`standalone`** | **Headless REST** runner, Docker, MCP server for AI agents, E2E test suite | [Clone `standalone`](https://github.com/jku-win-se/MOMoT2/tree/standalone) · [AGENTS.md](https://github.com/jku-win-se/MOMoT2/blob/standalone/AGENTS.md) |

See the [branches guide](https://jku-win-se.github.io/MOMoT2/branches.html) on the project site for a detailed comparison.

---

## `main` — Full Eclipse distribution

### Install in Eclipse

Use **Help → Install New Software…** and add:

```text
https://jku-win-se.github.io/MOMoT2/eclipse/updates/latest/develop/
```

Xtext and Henshin dependencies are resolved automatically from the update site.

### Repository layout

| Path | Description |
| --- | --- |
| `plugins/` | MOMoT core, MOEA bridge, configuration language, UI |
| `features/` | Eclipse feature definitions |
| `examples/` | Case-study examples (stack, CRA, modularization, TSE, …) |
| `releng/` | Update site and release engineering |
| `docs/` | GitHub Pages site (Jekyll) + published p2 repository |
| `tooling/` | Eclipse target platform (2026-03) |
| `scripts/` | Build helpers, `deploy.sh` for update-site publishing |

### Build

Requires Java 17+ and Maven with Tycho:

```bash
git clone -b main https://github.com/jku-win-se/MOMoT2.git
cd MOMoT2
mvn clean install
```

### Publish update site

After a successful build:

```bash
bash scripts/deploy.sh
```

This copies `releng/at.ac.tuwien.big.momot.update/target/repository/` into `docs/eclipse/updates/latest/develop/` and pushes to `main`. GitHub Pages serves the site from the `/docs` folder.

### Migration

This branch was modernized for Eclipse 2026-03 / Tycho 4.x. See [MIGRATION.md](MIGRATION.md).

---

## `standalone` — Headless REST + agents

The **`standalone`** branch is the right choice for Docker/CI execution and LLM agent integration. It includes:

- Docker REST server (`Dockerfile.headless`) — zip-in / zip-out on `POST /run`
- MCP server (`mcp/`) — stdio JSON-RPC bridge for AI tooling
- E2E test suite (`test-suite/`) — four verified benchmark problems
- Henshin validator CLI (`tools/henshin-validator/`)
- Minimal stack example (`stack-example-minimal/`)

```bash
git clone -b standalone https://github.com/jku-win-se/MOMoT2.git
cd MOMoT2
docker build -t momot-headless -f Dockerfile.headless .
docker run -p 8080:8080 momot-headless
```

Full documentation: [AGENTS.md](https://github.com/jku-win-se/MOMoT2/blob/standalone/AGENTS.md) · [Architecture overview](https://github.com/jku-win-se/MOMoT2/blob/standalone/doc/00-architecture-overview.md)

---

## Authors

MOMoT was developed by Martin Fleck ([@martin-fleck](https://github.com/martin-fleck)), Javier Troya ([@javitroya](https://github.com/javitroya)), and Manuel Wimmer ([@manuelWimmer](https://github.com/manuelWimmer)).

The MCP server and agent integration on the **standalone** branch were developed by MohammadHadi Dehghani ([@hadiDHD](https://github.com/hadiDHD)).

Original project: http://martin-fleck.github.io/momot/
