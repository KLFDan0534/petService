---
name: engineering-four-step
description: "Run before writing implementation code for any non-trivial software change. Enforces four pre-code operations: understand the real context, define the exact change boundary, plan the smallest safe implementation, and choose verification before editing files."
---

# Engineering Four Step

## Overview

Use this skill immediately before code changes. It keeps implementation grounded in the real repository, scoped to the user's request, and tied to verification before edits begin.

## Four Operations Before Code

Complete these four operations before editing implementation files:

1. Context: inspect the relevant files, routes, APIs, tests, styles, and existing patterns. Verify facts from the current tree instead of relying on memory.
2. Boundary: state what is in scope and what is out of scope. Keep unrelated refactors, style churn, and broad rewrites out unless required.
3. Implementation path: choose the smallest coherent change that fits local conventions. Reuse existing helpers, modules, naming, and project structure.
4. Verification: decide the proof before coding. Prefer focused tests first for behavior changes, then integration or browser checks when the change crosses layers or affects UI.

## Execution Rules

Apply these rules after the four operations:

- Make the code change only after the verification plan is clear.
- For behavior-bearing work, add, update, or identify the test that proves the behavior. When practical, observe the failing or characterization state before the production change.
- For frontend-visible work, verify with browser or screenshot checks when the project can run locally.
- For cross-module work, run the narrow checks first and then the broader checks needed to prove linked modules still work together.
- If a check cannot run, record the reason and the replacement evidence used.

This skill is an entry discipline, not a replacement for deeper planning, code review, or debugging skills. Use those when the task is large, risky, or explicitly asks for them.
