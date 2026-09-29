# Test doubles with Mockito

These six examples use the real WalkMates `SeekerService` and `Seeker` with controlled
collaborators. They provide a **partial worked example for Lab 2 Activity 4.2**, not its
complete solution. The earlier Pet unit-testing and coverage examples remain available
in this folder.

## Run the examples

From the WalkMates root, using Java 21:

```bash
mvn -v
mvn -Pexamples clean test -Dtest=SeekerDoublesTest
mvn -Pexamples test -Dtest=SeekerDoublesTest#declinePreservesExistingBalance
```

Open [SeekerDoublesTest.java](SeekerDoublesTest.java) together with
[SeekerService.java](../src/main/java/com/walkmates/service/SeekerService.java) and
[PaymentService.java](../src/main/java/com/walkmates/service/PaymentService.java).
Test reports are in `target/examples/surefire-reports/`.

The `examples` profile selects only this folder's teaching tests. Use normal `mvn test`
with that profile disabled for your lab tests in `src/test/java`. Copy evidence you need
before a normal `mvn clean`, which removes all of `target/`, including example reports.

## Navigate by concept

| Method | What it demonstrates |
|---|---|
| `successfulChargeCreditsExistingBalance` | Stub responses, execute the real service, assert balance and payment arguments |
| `declinePreservesExistingBalance` | Throw a controlled exception, check unchanged balance and absence of a save |
| `unknownSeekerDoesNotReachGateway` | Verify that a rejected request never reaches the gateway |
| `registrationUsesMatchersForGeneratedIdentity` | Use `any`, `thenAnswer` and `argThat` when the generated ID is unknown |
| `registrationWithInMemoryRepository` | Use a working repository instead of lookup/save stubs |
| `spyKeepsRepositoryBehaviour` | Record interactions while real repository methods execute |

## Read the test in Arrange–Act–Assert order

The shared fixture uses `@ExtendWith(MockitoExtension.class)` to initialise `@Mock` fields.
It constructs a real service with those dependencies and creates a fresh valid Seeker.
Do not also call `openMocks`. Keep scenario-specific stubbing inside each test.

In the success example, Arrange funds the wallet with 50 SEK, configures repository lookup
and save, and makes the gateway return a confirmation for a 25 SEK charge. Act calls the
real `topUp`. Assert checks the literal expected balance of 75 SEK and the payment arguments.
Starting above zero distinguishes adding funds from overwriting the balance.

`when(...).thenReturn(...)` configures an answer. It does not by itself assert that the call
happened. `verify(payments).charge(...)` asserts that the matching call happened exactly once;
it does not perform a payment. Current `topUp` does not use the confirmation string, which
makes the separate interaction assertion particularly useful.

In the decline example, `thenThrow` controls the gateway outcome. `assertThrows` invokes the
operation and observes the exception. The balance assertion then checks the failure
postcondition, and `never().save(...)` checks that there was no save after the failed charge.

## Choosing a double

The same Mockito object can serve as a stub when supplying responses and as a mock when
verifying interactions. Do not mock the service under test or replace the real wallet logic
with a configured answer that your assertion merely repeats.

The in-memory repository is the real lab implementation. Relative to database persistence,
it can play the role of a fake. It stores object references, so observing a modified stored
object does not prove that a save call occurred.

A Mockito spy calls real methods unless stubbed and records interactions. If stubbing a spy
would otherwise run an unwanted real method, use the `doReturn(...).when(spy)` form.
Use a spy only when the recorded interaction adds evidence you need.

Matchers describe accepted arguments. Constrain payment amounts and identities when they
are part of the obligation. If any argument uses a matcher, all arguments must use matchers.
The registration example uses an answer that returns the object passed to save because
the new Seeker's generated ID is not known in advance.

## What remains for Activity 4.2

The supplied success and decline cases are intentional partial examples. Students must
still implement and justify the **timeout case** and the **successful booking confirmation
notification check**, using the real target service in each case. Follow the full lab brief
for required files, outcomes and reporting; these examples do not replace it.

A locally unchanged wallet after a timeout does not establish whether a remote provider
processed a charge. These tests check service behaviour under chosen responses. Actual
gateway integration, retry safety, database transactions and concurrency require other
evidence and an agreed contract.

## References

- [Mockito documentation](https://site.mockito.org/): stubbing, verification, matchers and spies.
- Gerard Meszaros, *xUnit Test Patterns* (2007): roles of test doubles.
- [Lab 2 Activity 4.2](../lab-instructions/02-LAB2-STRUCTURAL.md): complete task requirements.
