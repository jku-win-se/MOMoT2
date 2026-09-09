# MOMoT MCP — Headless & Agentic Model-Driven Optimization

Welcome to the official documentation for the **MOMoT-MCP** platform! 

This repository combines Model-Driven Engineering (EMF/Ecore, Henshin Graph Transformations) with Multi-Objective Evolutionary Algorithms ([MOEA Framework 5.1](https://github.com/MOEAFramework/MOEAFramework/releases/tag/v5.1)) to solve complex optimization problems. It has been modernized into a **container-first, headless deployment** equipped with a **Model Context Protocol (MCP)** server, making it fully accessible to LLM coding agents (like Cursor, Claude, and more) as well as automated CI/CD pipelines.

---

## What is MOMoT?

**MOMoT** (Model-Driven Optimization via Transformation) allows you to define optimization problems at a high level of abstraction using model transformations as search operators:
1. **Metamodels (`.ecore`)** define the structure and constraints of your system.
2. **Model Instances (`.xmi`)** represent individual system configurations.
3. **Graph Transformation Rules (`.henshin`)** define the valid structural changes (rules) that can be applied to transition from one system state to another.
4. **OCL Expressions or Java Helpers** define the multi-objective fitness functions.
5. **Declarative Search Scripts (`.momot`)** configure the search algorithm, objectives, population size, max evaluations, and rules to execute.

---

## Component Architecture

MOMoT-MCP is composed of three interconnected layers:

```
┌─────────────────┐       JSON-RPC       ┌─────────────────┐
│   LLM Agent /   │ ───────────────────> │   MCP Server    │
│   User Client   │ <─────────────────── │   (mcp/Node)    │
└─────────────────┘                      └─────────────────┘
                                                  │
                                                  │ HTTP REST (POST /run)
                                                  ▼
┌──────────────────────────────────────────────────────────────────┐
│                      Docker Headless Container                   │
│                                                                  │
│  ┌───────────────────────┐             ┌──────────────────────┐  │
│  │   REST Server (JVM)   │ ──────────> │   MOMoT Core (OSGi)  │  │
│  └───────────────────────┘             └──────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
```

1. **Headless REST Runner (Docker):** A lightweight Java REST server that executes MOMoT search jobs packaged as zip files (`POST /run`), compiling and running scripts programmatically without requiring an Eclipse GUI.
2. **Model Context Protocol (MCP) Server:** A Node.js bridge that exposes programmatic tools for parsing metamodels, auto-generating transformations, validating scripts, and running search executions.
3. **CLI Validators:** Extremely fast command-line utilities for checking Ecore, XMI, Henshin, and MOMoT files locally before starting resource-heavy container executions.

---

## Where to Start?

Explore the documentation sections using the sidebar to learn more:

### 🚀 Getting Started & Architecture
* [Architecture Overview](00-architecture-overview.md) — Detailed topology and REST API structure.
* [Package & Entry Points](01-package-and-entrypoints.md) — Syntactic structure of MOMoT scripts.
* [Minimal Stack Case Study](09-minimal-test-case.md) — Step-by-step walk-through of the stack load-balancing example.
* [Validation Runbook](08-validation-and-runbook.md) — Diagnosing compilation and validation errors.

### 📐 Structural Metamodeling
* [Ecore Overview](ecore/00-overview.md) — Creating valid metamodels.
* [Class & Attribute Patterns](ecore/01-class-patterns.md) — Metamodeling classes, references, and types.
* [XMI Instance Generation](xmi/00-overview.md) — Creating initial test instances and worst-case configurations.

### 🔄 Transformation Rules & Objectives
* [Henshin Overview](henshin/00-overview.md) — Writing graph transformation rules as search operators.
* [OCL Objectives](momot/10-ocl-expressions.md) — Crafting multi-objective metrics using Object Constraint Language.
* [Java Helpers](java-helpers/00-overview.md) — Implementing custom fitness functions in Java for advanced metrics.

---

## Contributing and Support

MOMoT-MCP is open-source under the MIT/EPL license. For bugs, feature requests, or contributions, please open an issue or pull request in the [MOMoT-MCP Repository](https://github.com/jku-win-se/MOMoT-MCP).
