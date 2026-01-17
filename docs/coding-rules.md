# Claude Code Skill: Standard Refactoring & Code Quality Rules

## 1. Purpose

Based on Martin Fowler's "Refactoring" principles, Claude Code maximizes code readability, maintainability, and extensibility. Claude Code adheres to the following rules when writing and modifying code, and immediately refactors any "bad smells" it encounters.

## 2. Standard Rules for Code Quality

### 2.1. Naming Convention

- **Principle**: Code should clearly indicate "what it does."
- **Execution**:
- Method names should indicate "purpose (intention)," not "how" they perform their function.
- Variable names should be intuitive and their purpose should be understandable even without comments.
- Don't be stingy with name changes. Apply the "method renaming" technique until names are clear.

### 2.2. Method and Class Size Constraints

- **Short Method**: Each method should perform only one clear function. Extract any parts that might require comments into separate methods (Extract Method).
- **Small Class**: If a class has too many instance variables or functions, separate responsibilities and implement an Extract Class.

### 2.3. Long Parameter List

- If there are too many parameters, simplify the interface by converting them to parameter objects or passing entire objects.

## 3. Code Smell Detection and Response

Claude Code immediately suggests refactoring plans when it detects the following patterns:

| Smell | Refactoring Strategy |
| :-------------------------------- | :------------------------------------------------------------------ |
| **Duplicated Code** | Extract methods and then move them to a higher class or extract classes |
| **Long Method** | Convert temporary variables to method calls, split conditional statements |
| **Large Class** | Extract Class or subclass |
| **Feature Envy** | Move methods to the class that contains the data |
| **Data Clumps** | Bundle together data that travels together into a single class |
| **Obsessive Primitive Types** | Replace Data Values ​​with Objects |
| **Switch Statements** | Use Polymorphism to Convert Conditional Statements to Overrides |

## 4. Refactoring Execution Protocol

When refactoring, Clod code follows Martin Fowler's "Rhythm."

1. **Secure Tests**: Before refactoring, check if self-test code exists for the relevant section. If not, create it.
2. **Micro-steps**: Make changes one at a time.
3. **Immediate Testing**: Run tests after each step to ensure there are no regressions.
4. **Add and Separate Features**: Do not add new features during refactoring (cleaning). Clearly distinguish between the two tasks.

## 5. Key Technique Guidelines

### 5.1. Simplify Conditional Statements

- Reduce nesting by converting multiple conditional statements to **Guard Clauses**.
- Extract complex conditional expressions into methods and expose their meaning through their names.

### 5.2. Move Functionality Between Objects

- If a method references more data from another object than the object it belongs to, consider **Moving Methods**. - Classes that only delegate unnecessary data should be encouraged to access the actual implementation objects directly by **eliminating excessive intermediary methods**.

### 5.3. Data Organization

- Magic numbers should always be converted to **symbolic constants**.
- When returning a collection, return a **read-only view** or a copy to protect the original.

## 6. Exceptions

- In extremely exceptional cases (critical paths) where performance optimization must take precedence over readability, defer refactoring, but clearly state the reason.
- When external library classes cannot be modified, use the "add methods to foreign classes" or "local inheritance extension" techniques.