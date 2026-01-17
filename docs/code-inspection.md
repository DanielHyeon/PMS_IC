# Claude Code Skill: Automated Code Inspection & Test Protocol (Refactoring Standard)

## 1. Overview

This skill defines systematic rules to proactively detect code defects (Inspection) and ensure that changes do not break existing functionality (Testing). By implementing Martin Fowler's "self-testing code" philosophy, it ensures that code written or modified by AI is always "redeployable."

---
## 2. Code Inspection Guidelines (Inspection Rules)

Claude Code always checks the following items (Checklist) before and after modifying code:

### 2.1. Cognitive Complexity and Structure Check

- **Control Flow Simplification**: Check whether nested `if` statements or complex `switch` statements increase cognitive load. Nesting levels greater than three are a target for refactoring.
- **Dependency Mapping**: Check whether a specific class or function depends on too many external modules (Coupling). Investigate the possibility of a "one modification causing a cascade of destruction."
- **Location of Responsibility**: Checks whether a method uses more data from other classes than its own (Feature Envy).

### 2.2. Deep Detection of Code Smells

- **Data Clumps**: If a group of variables are always grouped together, warns that they should be grouped into a class.
- **Primitive Obsession**: Checks whether phone numbers, amounts, addresses, etc. are simply treated as strings or numbers and suggests dedicated objects.
- **Temporary Variable Verbosity**: If there are too many variables within a method that temporarily store values, check whether these can be converted into method calls (Replace Temp with Query).

### 2.3. Interface and Naming Convention Review

- **Intent Revealing**: Checks whether function names clearly indicate "what" rather than "how" they do something.
- **Parameter Objectification**: If a function has three or more arguments, check whether they can be combined into a single structure or object.

--

## 3. Testing Rules

A prerequisite for refactoring is "perfect testing." Claude Code designs and executes tests according to the following rules.

### 3.1. Test-First Principle

- **Before Refactoring**: Check for existing tests for the target code to be modified. If no tests exist, first write a "Characterization Test" that records the current behavior.
- **During Refactoring**: Run tests at each "micro-step." A single test failure means an immediate rollback to the previous step.

### 3.2. Test Case Design Standards

- **Boundary Value Analysis**: Ensure that tests for exceptional boundary conditions, such as the beginning and end of loops, when a collection is empty, or when data is null, are included.
- **Positive/Negative Testing**: Ensure that appropriate exceptions are raised in expected error situations, as well as the normal flow (happy path).
- **Ensuring Independence**: Each test must not depend on others, and the results must be consistent regardless of the order in which they are run.

### 3.3. Test Automation Protocol

- Clod Code automatically finds and executes relevant test suites when files are modified.
- Changes that reduce test coverage are classified as "risk factors" and reported to the user.

--

## 4. Refactoring-Test Integration Workflow

1. **Inspect**: Identify code smells and complexity leverage in the target code.
2. **Verify**: Run unit tests that verify the target functionality. 3. **Refactor**: Apply mechanical transformations (e.g., extracting functions, renaming variables) one at a time.
4. **Regression**: Run all tests to ensure there are no side effects.
5. **Commit**: Only commit code if the tests pass.

--

## 5. Exceptions and Warnings

- **Conflicts with Performance Optimization**: If refactoring for readability degrades critical system performance, specify this during the inspection and provide a comment explaining the reason.
- **Legacy Code**: For code that is so tangled that it's impossible to write tests, we recommend minimal modifications to "break dependencies."