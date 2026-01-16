---
name: coding-standard
description: Code refactoring and inspection standards based on Martin Fowler. Use when modifying or refactoring code, reviewing code quality, identifying smells, or planning/executing tests to prevent regressions and keep changes deployable.
---
# Refactor Inspection Standard

## Overview

Apply refactoring, inspection, and testing rules to keep code readable, maintainable, and regression-safe during edits.

## Workflow

1. Inspect code structure and smells before changing behavior.
2. Verify existing tests; add characterization tests when coverage is missing.
3. Refactor in micro-steps; run tests after each step.
4. Document exceptions (performance-critical paths or legacy constraints).

## Guidelines

- Enforce naming, size limits, and parameter simplification rules.
- Detect and address common smells (duplication, long methods, large classes, feature envy, data clumps, primitive obsession, switch abuse).
- Prefer guard clauses, move methods to the right owner, and replace magic numbers.
- Keep test suites independent; include boundary and negative cases.

## References

- Use `references/coding-rules.md` for refactoring rules and smell 대응 전략.
- Use `references/code-inspection.md` for inspection checklists and test protocol.
