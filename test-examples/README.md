# Teaching examples: unit tests, coverage and test doubles

These runnable examples test the real [Pet](../src/main/java/com/walkmates/catalog/Pet.java)
used by the provider catalog. Read this guide, then the comments in
[PetTest.java](PetTest.java) and [PetCoverageTest.java](PetCoverageTest.java).
No slides or external companion files are needed. The examples are separate from the frozen lab tasks.

For test doubles, continue with [Mockito examples](DOUBLES.md) and
[SeekerDoublesTest.java](SeekerDoublesTest.java). The success and decline cases are partial
worked examples for Activity 4.2; timeout and booking-notification tests remain student work.

| File | Focus |
|---|---|
| `PetTest.java` | Unit testing and parameterised tests |
| `PetCoverageTest.java` | Structural coverage and boundaries |
| `SeekerDoublesTest.java` | Stubbing, interaction assertions, a working repository and a spy |

Run all three classes with `mvn -Pexamples clean test`, or select one by name as below.
There are no separate TDD or mutation-demo classes in this folder.

## Start here

From the WalkMates root, with Java 21:

```bash
mvn -v
mvn -Pexamples clean test
mvn -Pexamples -Dtest=PetTest test
mvn -Pexamples -Dtest=PetTest#renameStripsSurroundingWhitespace test
```

The `examples` profile compiles the actual application but selects **only** `test-examples/`.
The Java files declare `com.walkmates.examples`; their flat source layout makes them easy to find.
Results are in `target/examples/surefire-reports/`; coverage is in
`target/examples/site/jacoco/index.html`.

Normal `mvn clean test` still selects `src/test/java` and writes to `target/`.
Use normal commands for lab evidence; deactivate the examples profile in your IDE afterward.
A normal `mvn clean` removes all of `target/`, including example reports, so save evidence first.

## The behavior under test

The catalog's existing rules are the basis for these examples:

- Names are required, stripped of surrounding whitespace, and limited to 40 characters.
- Age is an integer from 0 through 40 inclusive.
- Absent optional breed and notes become empty strings.
- Editing keeps identity and ownership.
- A failed multi-field edit preserves all previous details. The `updateDetails` Javadoc
  explicitly promises validation before any changes are applied.

These are catalog behaviors, not new FR-1/FR-4 lab requirements. Inspection of production
code tells us what it currently does; that alone is not an independent specification.
Agree the intended rule before writing a literal expected result.

## Unit testing and Arrange–Act–Assert

A unit test controls a small scope and checks observable behavior. Here that scope is a Pet:
there is no Spring context, database, network, or UI. Passing these tests does not establish
that those broader integrations work.

Every test identifies three responsibilities:

1. **Arrange:** establish valid starting objects and choose the distinguishing input.
2. **Act:** call the public operation whose behavior the test is about.
3. **Assert:** compare its result or state with the agreed expectation (the **oracle**).

For renaming, `setUp` supplies Rex, `setName("  Luna  ")` is the action, and the expected
stored name is the literal `"Luna"`. Merely asserting that the Pet exists would not detect
a setter that ignores the edit or keeps the spaces.

For constructor testing, the constructor call is visible in the test as the action.
For repeated editing, the first rename arranges the history and the second rename is the action.
AAA describes responsibilities; it does not require exactly three statements.

### Exceptions combine Act and Assert

`assertThrows(IllegalArgumentException.class, () -> pet.setName("   "))` both executes the
action and checks its exception. The lambda delays execution until JUnit can observe it.
The expected type or a subtype is accepted. Calling the operation before `assertThrows`
would let the exception escape.

Then check the postcondition: the name is still Rex. For an atomic multi-field edit, check
all previous details, because an exception alone would miss a partial update.

## Fixtures and independent tests

A **fixture** is the objects, data, and environment a test needs. `@BeforeEach setUp`
constructs a fresh valid Pet before each test, including every parameterized invocation.
No test depends on a previous test's edit. Keep scenario-specific input in the test so the
reader can see what differs.

JUnit Jupiter's default lifecycle also creates a new test instance per test. That does not
automatically isolate static fields, files, or external services. These examples acquire no
external resources, so an empty cleanup method would add no value.

## Parameterization: one structure, several cases

A parameterized test runs the same setup/action/oracle structure for each input:

| Source | Example | Why these cases matter |
|---|---|---|
| `@ValueSource` | Ages 0, 1, 5, 39, 40 | Endpoints, adjacent values, ordinary input |
| `@CsvSource` | Raw name and literal expected name | Pair input with an independent oracle |
| `@NullAndEmptySource` plus `@ValueSource` | null, empty, space, tab | Distinct inputs with the same rejection contract |

The CSV example explicitly preserves whitespace during argument parsing. Otherwise JUnit
could normalize the test input before Pet receives it, hiding a faulty implementation.
The expected result is a literal rather than a copy of the production normalization algorithm.
Invocation names expose which input failed. Keep accepted and rejected cases separate when
their assertion structures differ.

## Navigate by concept

| Concept | PetTest method |
|---|---|
| AAA and a meaningful oracle | `renameStripsSurroundingWhitespace` |
| Constructor state and grouped assertions | `constructorEstablishesCatalogState` |
| Exception and preserved state | `blankNameIsRejectedWithoutChangingName` |
| Parameterized accepted values | `acceptedAgesAreStored` |
| Paired input and expected result | `namesAreNormalized` |
| Null/empty arguments | `missingNamesAreRejected` |
| Atomicity of a failed edit | `failedEditPreservesAllPreviousDetails` |
| Successful edit and stable identity | `successfulEditKeepsIdentityAndOwnership` |
| Sequence and AssertJ | `repeatedRenameUsesLatestName` |
| Fresh fixture per invocation | `setUp` |

`assertAll` groups related expectations and attempts every supplied check.
AssertJ's `assertThat(...).isEqualTo(...)` is another way to express a value oracle;
JUnit still runs the test. A generated ID is checked for preservation after an edit,
not against a fixed UUID.

## Structural coverage: predict, run, compare

Open `Pet.validatedAgeYears`. Its predicate is `age < 0 || age > 40`.

| Input | First comparison | Second comparison | Result |
|---|---|---|---|
| −1 | True | Not evaluated | Reject |
| 7 | False | False | Accept |
| 41 | False | True | Reject |

Java short-circuits OR: when the first comparison is true, it does not evaluate the second.
Both comparisons have true/false branch outcomes, so JaCoCo counts four outcomes for this method.
The source-level compound decision has only two overall outcomes; these are different denominators.
Coverage of a throw does not tell us whether the previous state was preserved.

Run each command separately and inspect/save its report before the next run.
`clean` prevents execution data from accumulating across selected suites.

```bash
mvn -Pexamples clean test -Dtest=PetCoverageTest#ordinaryAge
mvn -Pexamples clean test '-Dtest=PetCoverageTest#ordinaryAge+negativeAge+excessiveAge'
mvn -Pexamples clean test '-Dtest=PetCoverageTest#ordinaryAge+negativeAge+excessiveAge+inclusiveEndpoints'
```

Open `target/examples/site/jacoco/index.html`, then
**com.walkmates.catalog → Pet → validatedAgeYears**.

| Selected suite | Invocations | Method branch outcomes |
|---|---:|---:|
| Ordinary age | 1 | 2/4 |
| Ordinary plus both rejections | 3 | 4/4 |
| Above plus endpoints 0 and 40 | 5 | 4/4 |

The constructor in every fixture already takes the accepted route with age 5.
Coverage includes setup, not just the action. Compare the same method, source version, and
tool configuration; do not interpret these counts as whole-class or whole-project coverage.
JaCoCo's report goal is already bound to `test` in this project.

### Full coverage can miss a boundary fault

Ages −1, 7, and 41 exercise all four branch outcomes. Yet changing `> 40` to `>= 40`
would leave all three results unchanged. Age 40 distinguishes the intended inclusive rule
from that hypothetical fault. Similarly, age 0 distinguishes `< 0` from `<= 0`.

The endpoint tests improve fault detection without increasing coverage. Assertions and
well-chosen values supply evidence that an execution percentage cannot provide alone.

### Name paths and unchanged state

Run `mvn -Pexamples clean test -Dtest=PetCoverageTest` and inspect `validatedName`.

| Path | Method | Expected observation |
|---|---|---|
| Null, so skip `isBlank` | `missingName` | Reject and preserve Rex |
| Non-null but blank | `missingName` | Reject and preserve Rex |
| Nonblank, length 41 | `longName` | Reject and preserve Rex |
| Nonblank, length 40 | `maximumLengthName` | Store all 40 characters |

The fixture Rex already covers an accepted name. Length 40 adds an endpoint oracle.
These four feasible routes cover six branch outcomes across three conditional checks;
they do not prove every catalog rule or all combinations of edits.

## Validation and extension

On 2026-09-28 the recent additions were reduced to Doubles only. The current folder contains
18 PetTest, 10 PetCoverageTest and 6 SeekerDoublesTest invocations: all 34 pass on Java 21.0.11,
as do the nine normal lab invocations. The new standalone mutation/TDD demos were removed;
the earlier Pet coverage examples below remain unchanged.

The suite has 18 PetTest and 10 PetCoverageTest invocations. The initial validation on
2026-09-20 used Java 21.0.11: all 28 passed, as did all nine original lab invocations.
Default method-level coverage matched the c2e381b baseline. The selected age experiments
produced 2/4, 4/4, 4/4 branch outcomes. In a disposable copy, an upper-bound `>=` change
survived the three branch cases but was detected by endpoint 40. The application source
contains no such change. The renamed `examples` profile and expanded comments were rechecked
on the same date: all 28 examples and nine original tests pass; default coverage is unchanged.

Extend these examples by choosing a behavior, explaining the input and oracle, and keeping
AAA visible. Preserve the lab sources, requirements, CI and mutation targets.
Keep deliberately wrong expectations in disposable experiments, not the default suite.
