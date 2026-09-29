package com.walkmates.examples;

import com.walkmates.catalog.Pet;
import com.walkmates.catalog.PetSpecies;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit testing the real provider catalog through Pet's public API.
 *
 * <p>Read each test as Arrange (establish preconditions), Act (exercise one behavior),
 * Assert (compare the observable result with an independent expectation). A useful oracle
 * is more than "the call did not crash": it states what the catalog should store or preserve.
 * Expected strings and ages below come from the catalog contract in this folder's README,
 * rather than being calculated by calling the production validator.</p>
 *
 * <p>These tests need no Spring context, database, or network. They exercise the same Pet
 * class used by the application, but do not establish that the UI or persistence works.
 * Run this class from the repository root with {@code mvn -Pexamples -Dtest=PetTest test}.</p>
 */
class PetTest {
    private Pet pet;

    @BeforeEach
    void setUp() {
        // Shared Arrange: every test, including every parameterized invocation, gets a
        // fresh mutable Pet. A rename in one test cannot affect another test's starting state.
        // Keep unrelated fields valid so each test can isolate its intended behavior.
        pet = new Pet("provider-1", "Rex", PetSpecies.DOG, "Collie", 5, "Calm");
    }

    @Test
    void constructorEstablishesCatalogState() {
        // Arrange: use valid catalog details; the expected age and name are literal inputs.
        // Act: construct explicitly here because construction is the behavior under test.
        // The shared fixture is replaced so the action is visible in this test.
        pet = new Pet("provider-1", "Rex", PetSpecies.DOG, "Collie", 5, "Calm");

        // Assert: these related checks describe one initial-state contract. assertAll
        // attempts every supplied check and reports failures together. A non-null ID only
        // establishes its presence, not uniqueness across all pets.
        assertAll(
                () -> assertEquals("provider-1", pet.getProviderId()),
                () -> assertEquals("Rex", pet.getName()),
                () -> assertEquals(PetSpecies.DOG, pet.getSpecies()),
                () -> assertEquals(5, pet.getAgeYears()),
                () -> assertNotNull(pet.getId()));
    }

    @Test
    void renameStripsSurroundingWhitespace() {
        // Arrange: setUp supplies a pet named Rex; surrounding spaces distinguish this
        // input from an already-normalized name. Internal spaces are tested separately.
        // Act: rename through the public API, not by calling the private validator.
        pet.setName("  Luna  ");

        // Assert: the contract requires a normalized value, not just a non-null Pet.
        // A setter that stores the raw input or ignores the edit must fail this check.
        assertEquals("Luna", pet.getName());
    }

    @Test
    void blankNameIsRejectedWithoutChangingName() {
        // Arrange: the fixture has the valid name Rex; spaces alone are not a name.
        // Act + Assert: assertThrows executes the lambda and checks the exception type.
        // It accepts IllegalArgumentException or a subtype. Calling setName before
        // assertThrows would let the exception escape instead of checking it.
        assertThrows(IllegalArgumentException.class, () -> pet.setName("   "));

        // Assert postcondition: rejection must not corrupt the previous valid value.
        assertEquals("Rex", pet.getName());
    }

    @ParameterizedTest(name = "age {0} is stored")
    @ValueSource(ints = {0, 1, 5, 39, 40})
    void acceptedAgesAreStored(int age) {
        // Arrange: JUnit supplies one age per invocation and runs setUp each time.
        // 0 and 40 are inclusive boundaries; 1 and 39 are adjacent valid values;
        // 5 represents an ordinary value. The invocation name identifies a failing input.
        // Act
        pet.setAgeYears(age);

        // Assert: an accepted integer age is stored unchanged. This oracle tests storage,
        // not merely acceptance. A setter that always stores 5 fails the other invocations.
        assertEquals(age, pet.getAgeYears());
    }

    @ParameterizedTest(name = "name [{0}] becomes [{1}]")
    @CsvSource(value = {"  Luna  |Luna", "Rex|Rex", "  Sir Rex |Sir Rex"},
            delimiter = '|', ignoreLeadingAndTrailingWhitespace = false)
    void namesAreNormalized(String input, String expected) {
        // Arrange: each CSV row pairs an input with its literal expected result.
        // Preserve whitespace in JUnit's argument parsing: otherwise the test framework
        // could strip it before Pet receives it, hiding a broken production normalizer.
        // "Sir Rex" also checks that meaningful internal spaces survive.
        // Act
        pet.setName(input);

        // Assert: do not derive expected by applying the same normalization algorithm.
        // An independent literal can disagree with a faulty implementation.
        assertEquals(expected, pet.getName());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void missingNamesAreRejected(String name) {
        // Arrange: four separate cases: null, empty string, space, and tab. They share
        // a rejection contract, but are not identical inputs (null cannot call isBlank).
        // Act + Assert: the same assertion structure applies to every invalid case.
        assertThrows(IllegalArgumentException.class, () -> pet.setName(name));

        // Assert postcondition: each invocation starts at Rex and must leave it intact.
        assertEquals("Rex", pet.getName());
    }

    @Test
    void failedEditPreservesAllPreviousDetails() {
        // Arrange: start with Rex/DOG/Collie/5/Calm from setUp. Propose different, valid
        // name/species/breed before an invalid age. This makes a partial update observable.
        // Act + Assert: updateDetails promises to validate all fields before applying any.
        assertThrows(IllegalArgumentException.class,
                () -> pet.updateDetails("Luna", PetSpecies.CAT, "Siamese", -1, "Quiet"));

        // Assert postconditions: an exception alone would miss earlier fields being changed
        // before age validation fails. Check the whole record's previous details.
        assertAll(
                () -> assertEquals("Rex", pet.getName()),
                () -> assertEquals(PetSpecies.DOG, pet.getSpecies()),
                () -> assertEquals("Collie", pet.getBreed()),
                () -> assertEquals(5, pet.getAgeYears()),
                () -> assertEquals("Calm", pet.getNotes()));
    }

    @Test
    void successfulEditKeepsIdentityAndOwnership() {
        // Arrange: capture this pet's generated ID. A hard-coded UUID would be unrelated
        // to the contract; the meaningful expectation is that editing preserves identity.
        String originalId = pet.getId();

        // Act: edit mutable details. Optional breed and notes accept null and become empty.
        pet.updateDetails("Luna", PetSpecies.CAT, null, 2, null);

        // Assert: check both the requested changes and what must remain unchanged.
        assertAll(
                () -> assertEquals(originalId, pet.getId()),
                () -> assertEquals("provider-1", pet.getProviderId()),
                () -> assertEquals("Luna", pet.getName()),
                () -> assertEquals(PetSpecies.CAT, pet.getSpecies()),
                () -> assertEquals(2, pet.getAgeYears()),
                () -> assertEquals("", pet.getBreed()),
                () -> assertEquals("", pet.getNotes()));
    }

    @Test
    void repeatedRenameUsesLatestName() {
        // Arrange: establish a previous successful edit as this scenario's history.
        pet.setName("Luna");

        // Act: the second rename is the operation under test.
        pet.setName("  Milo  ");

        // Assert: retain neither Rex nor Luna, and do not concatenate names. AssertJ's
        // fluent assertion expresses the same value comparison as JUnit's assertEquals;
        // JUnit still discovers and runs the @Test method.
        assertThat(pet.getName()).isEqualTo("Milo");
    }
}
