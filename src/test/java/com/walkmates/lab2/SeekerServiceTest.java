package com.walkmates.lab2;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.walkmates.model.Seeker;
import com.walkmates.repository.SeekerRepository;
import com.walkmates.service.NotificationService;
import com.walkmates.service.PaymentService;
import com.walkmates.service.SeekerService;

import java.util.Optional;

public class SeekerServiceTest {

    private SeekerRepository seekers;
    private PaymentService payments;
    private NotificationService notifications;
    private SeekerService service;
    private Seeker seeker;

    @BeforeEach
    void setup() {
        seekers = mock(SeekerRepository.class);
        payments = mock(PaymentService.class);
        notifications = mock(NotificationService.class);

        service = new SeekerService(seekers, payments, notifications);

        seeker = new Seeker("example@example.com", "Example", "0701234567");

        when(seekers.findById(seeker.getId()))
                .thenReturn(Optional.of(seeker));
    }

    @Test
    @DisplayName("Successful payment credits wallet")
    void successfulTopUpCreditsWallet() throws Exception {

        when(payments.charge(
                seeker.getId(),
                "payment-1",
                100.00)).thenReturn("charge-123");

        when(seekers.save(seeker)).thenReturn(seeker);

        Seeker updatedSeeker = service.topUp(seeker.getId(), "payment-1", 100.00);

        assertThat(updatedSeeker.getBalance()).isEqualTo(100.00);

        verify(payments).charge(seeker.getId(), "payment-1", 100.00);

        verify(seekers).save(seeker);
    }

    @Test
    @DisplayName("Declined payment does not credit wallet")
    void declinedPaymentDoesNotCreditWallet() throws Exception {

        when(payments.charge(
                seeker.getId(),
                "payment-1",
                100.00)).thenThrow(new PaymentService.PaymentException("Payment declined"));

        assertThatThrownBy(() -> service.topUp(seeker.getId(), "payment-1", 100.00))
                .isInstanceOf(PaymentService.PaymentException.class);

        assertThat(seeker.getBalance()).isEqualTo(0.00);

        verify(payments).charge(seeker.getId(), "payment-1", 100.00);

        verify(seekers, never()).save(any(Seeker.class));
    }

    @Test
    @DisplayName("Payment timeout does not credit wallet")
    void timeOutPaymentDoesNotCreditWallet() throws Exception {

        when(payments.charge(
                seeker.getId(),
                "payment-1",
                100.00)).thenThrow(
                        new PaymentService.PaymentTimeoutException("Payment timeout"));
        assertThatThrownBy(() -> service.topUp(
                seeker.getId(),
                "payment-1",
                100.00))
                .isInstanceOf(PaymentService.PaymentTimeoutException.class);

        assertThat(seeker.getBalance()).isEqualTo(0.00);

        verify(payments).charge(seeker.getId(), "payment-1", 100.00);

        verify(seekers, never()).save(any(Seeker.class));
    }

}
