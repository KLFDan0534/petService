---
name: frontend-skill-chain
description: "Run before frontend UI, UX, React, animation, visual polish, page, component, or browser-facing implementation work when the user wants the installed frontend skills applied in a fixed order. Orchestrates this exact sequence: ui-ux-pro-max, impeccable, react-bits, karpathy, then engineering-four-step before code changes."
---

# Frontend Skill Chain

## Overview

Use this skill as the local orchestrator for the user's preferred frontend skill order. It does not replace the individual skills; it fixes the sequence in which they should be loaded and applied.

## Required Order

For frontend work, use the following skills in this exact order:

1. `ui-ux-pro-max`
2. `impeccable`
3. `react-bits`
4. `karpathy`
5. `engineering-four-step`

Do not reorder the first four frontend skills unless the user explicitly gives a new order.

## How To Apply

1. Load `ui-ux-pro-max` first to establish UX structure, visual direction, layout quality, and domain fit.
2. Load `impeccable` second to audit design quality, spacing, hierarchy, accessibility, and visual defects.
3. Load `react-bits` third when React UI, animated components, or premium interaction patterns are relevant.
4. Load `karpathy` fourth to apply disciplined engineering judgment and reduce common LLM coding mistakes.
5. Load `engineering-four-step` before editing code so implementation begins only after context, scope, plan, and verification are clear.

If a downstream skill cannot be found in the available skill list, state that clearly and continue with the remaining available skills while preserving the order of the rest.

## Frontend Completion Rule

After implementing frontend changes, verify the affected UI through the best available project checks. Prefer browser inspection or screenshots for visible changes, plus the project's existing test, lint, typecheck, or build commands.
