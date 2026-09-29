package com.walkmates.examples;

import com.walkmates.catalog.Pet;
import com.walkmates.catalog.PetSpecies;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Structural coverage and boundary testing of the real catalog Pet.
 *
 * <p>The age guard is {@code age < 0 || age > 40}. Java evaluates the second comparison
 * only when the first is false. JaCoCo therefore counts four branch outcomes: true/false
 * for each of the two comparisons. They are method-level counters, not coverage of all Pet.</p>
 *
 * <p>Select ordinaryAge alone for 2/4 outcomes; add negativeAge and excessiveAge for 4/4;
 * then add inclusiveEndpoints for stronger boundary evidence with the same 4/4. All tests
 * assert behavior: reaching a route without checking its effect would give weaker evidence.
 * See this folder's README for commands that clean between measurements.</p>
 *
 * <p>Tests use the public setters. Private validators are implementation details, inspected
 * to reason about control flow rather than invoked through reflection.</p>
 */
class PetCoverageTest {
    private Pet pet;

    @BeforeEach
    void setUp() {
        // Shared Arrange: a valid name and age isolate later rejection cases.
        // This constructor ALREADY executes the accepted age and name routes. Coverage
        // includes setup as well as the test action; it does not identify AAA phases.
        pet = new Pet("provider-1", "Rex", PetSpecies.DOG, "Collie", 5, "Calm");
    }

    @Test
    void ordinaryAge() {
        // Arrange: the fixture's age is 5; choose a different, ordinary accepted age.
        // Act: both comparisons evaluate false, so validation returns the supplied age.
        pet.setAgeYears(7);

        // Assert: prove the new value was stored, not just that the accepted route ran.
        assertEquals(7, pet.getAgeYears());
    }

    @Test
    void negativeAge() {
        // Arrange: -1 is just below the inclusive lower bound; the starting age is 5.
        // Act + Assert: the first comparison is true, so OR short-circuits. The second
        // comparison is NOT evaluated. Check the domain rejection, not any exception.
        assertThrows(IllegalArgumentException.class, () -> pet.setAgeYears(-1));

        // Assert postcondition: failed validation must leave the prior age intact.
        assertEquals(5, pet.getAgeYears());
    }

    @Test
    void excessiveAge() {
        // Arrange: 41 is just above the inclusive upper bound; the starting age is 5.
        // Act + Assert: the first comparison is false, the second is true. This reaches
        // a route that negativeAge cannot reach, even though both end in the same throw.
        assertThrows(IllegalArgumentException.class, () -> pet.setAgeYears(41));

        // Assert postcondition: rejecting the request must not store the invalid value.
        assertEquals(5, pet.getAgeYears());
    }

    @ParameterizedTest(name = "inclusive endpoint {0}")
    @ValueSource(ints = {0, 40})
    void inclusiveEndpoints(int age) {
        // Arrange: JUnit supplies each inclusive endpoint with a fresh fixture.
        // The -1/7/41 suite already covers all branches but would miss changing < to <=
        // or > to >=. These inputs distinguish those faults without adding a new route.
        // Act
        pet.setAgeYears(age);

        // Assert: acceptance AND correct storage are required at the endpoints.
        assertEquals(age, pet.getAgeYears());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void missingName(String name) {
        // Arrange: null, empty, and blank each receive the fresh name Rex.
        // Act + Assert: null short-circuits before isBlank; empty/space reach isBlank.
        // The routes differ even though they share the same exception and state oracle.
        assertThrows(IllegalArgumentException.class, () -> pet.setName(name));

        // Assert postcondition
        assertEquals("Rex", pet.getName());
    }

    @Test
    void longName() {
        // Arrange: a nonblank name of length 41 passes the missing-name checks, then
        // exceeds the maximum of 40. No surrounding spaces obscure the length boundary.
        String name = "x".repeat(41);

        // Act + Assert: reach the length-rejection route through the public setter.
        assertThrows(IllegalArgumentException.class, () -> pet.setName(name));

        // Assert postcondition
        assertEquals("Rex", pet.getName());
    }

    @Test
    void maximumLengthName() {
        // Arrange: exactly 40 characters must be accepted under the catalog contract.
        String name = "x".repeat(40);

        // Act: the same accepted route already ran when setUp constructed Rex.
        pet.setName(name);

        // Assert: this endpoint oracle adds fault-detection value, not branch coverage.
        assertEquals(name, pet.getName());
    }
}
