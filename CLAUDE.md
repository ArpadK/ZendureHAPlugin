# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

A Home Assistant (HA) plugin that interfaces with Zendure home battery systems. The project is in early development — no source files or build system exist yet.

**Read `ARCHITECTURE.md`** for the durable architecture plan (layers, package layout, integration boundaries, config/genericity rules). Keep it in sync as the design evolves.

## Tech Stack

- **Language:** Java (JDK 25 / Adoptium Temurin)
- **IDE:** IntelliJ IDEA (project config in `.idea/`)
- **Build system:** TBD (not yet initialized)

## Development Workflow

This project uses **OpenSpec** for spec-driven development. Before implementing new features:

- `/openspec-explore` — think through requirements and design
- `/openspec-propose` — create a proposal with design, specs, and tasks
- `/openspec-apply-change` — implement tasks from a proposal
- `/openspec-sync-specs` — sync delta specs back to main specs
- `/openspec-archive-change` — finalize and archive a completed change

OpenSpec config lives in `openspec/config.yaml`.

## Build & Test

No build system has been set up yet. Update this section once Maven/Gradle is initialized with the relevant commands (build, test, single test, lint).
