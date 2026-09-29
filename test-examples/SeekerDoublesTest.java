package com.walkmates.examples;

import com.walkmates.model.Seeker;
import com.walkmates.repository.SeekerRepository;
import com.walkmates.repository.inmemory.InMemorySeekerRepository;
import com.walkmates.service.NotificationService;
import com.walkmates.service.PaymentService;
import com.walkmates.service.SeekerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test doubles: real service and domain object, controlled collaborators.
 * Partial worked examples for Lab 2 Activity 4.2, explained in DOUBLES.md.
 * Students still implement timeout and successful booking-notification checks.
 */
@ExtendWith(MockitoExtension.class)
class SeekerDoublesTest {
    @Mock SeekerRepository seekers;
    @Mock PaymentService payments;
    @Mock NotificationService notifications;
    SeekerService service;
    Seeker ada;

    @BeforeEach
    void setUp() {
        // Explicit injection makes the seam visible. The extension initializes @Mock.
        // Do not also call openMocks. Stub only what each scenario uses.
        service = new SeekerService(seekers, payments, notifications);
        ada = new Seeker("ada@example.se", "Ada", "0701234567");
    }

    @Test
    void successfulChargeCreditsExistingBalance() throws Exception {
        ada.addFunds(50.00);
        when(seekers.findById(ada.getId())).thenReturn(Optional.of(ada));
        when(payments.charge(ada.getId(), "card-1", 25.00)).thenReturn("tx-17");
        when(seekers.save(ada)).thenReturn(ada);
        Seeker result = service.topUp(ada.getId(), "card-1", 25.00);
        assertEquals(75.00, result.getBalance(), 0.001);
        // verify is an assertion about a call, not a command to perform the charge.
        verify(payments).charge(ada.getId(), "card-1", 25.00);
    }

    @Test
    void declinePreservesExistingBalance() throws Exception {
        ada.addFunds(50.00);
        when(seekers.findById(ada.getId())).thenReturn(Optional.of(ada));
        when(payments.charge(ada.getId(), "card-1", 25.00))
                .thenThrow(new PaymentService.PaymentException("declined"));
        assertThrows(PaymentService.PaymentException.class,
                () -> service.topUp(ada.getId(), "card-1", 25.00));
        assertEquals(50.00, ada.getBalance(), 0.001);
        verify(seekers, never()).save(any(Seeker.class));
    }

    @Test
    void unknownSeekerDoesNotReachGateway() {
        when(seekers.findById("missing")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> service.topUp("missing", "card-1", 25.00));
        verifyNoInteractions(payments);
        verify(seekers, never()).save(any(Seeker.class));
    }

    @Test
    void registrationUsesMatchersForGeneratedIdentity() {
        when(seekers.findByEmail("ada@example.se")).thenReturn(Optional.empty());
        // The new Seeker's UUID is unknown. Return the object actually passed to save.
        when(seekers.save(any(Seeker.class))).thenAnswer(call -> call.getArgument(0));
        Seeker result = service.register("ada@example.se", "Ada", "0701234567");
        assertEquals("ada@example.se", result.getEmail());
        verify(seekers).save(argThat(s -> s.getEmail().equals("ada@example.se")));
    }

    @Test
    void registrationWithInMemoryRepository() {
        // This is the real lab repository; relative to a database it plays a fake's role.
        var memory = new InMemorySeekerRepository();
        var realService = new SeekerService(memory, payments, notifications);
        Seeker result = realService.register("ada@example.se", "Ada", "0701234567");
        assertSame(result, memory.findById(result.getId()).orElseThrow());
        assertThrows(IllegalArgumentException.class,
                () -> realService.register("ada@example.se", "Ada", "0701234567"));
    }

    @Test
    void spyKeepsRepositoryBehaviour() {
        var memory = spy(new InMemorySeekerRepository());
        var realService = new SeekerService(memory, payments, notifications);
        Seeker result = realService.register("ada@example.se", "Ada", "0701234567");
        assertSame(result, memory.findById(result.getId()).orElseThrow());
        verify(memory).save(result);
        // Illustrates a spy, not a reason to spy on every collaborator.
    }
}
