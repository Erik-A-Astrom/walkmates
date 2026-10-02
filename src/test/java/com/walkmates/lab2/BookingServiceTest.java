package com.walkmates.lab2;

import com.walkmates.model.Booking;
import com.walkmates.model.Listing;
import com.walkmates.model.ListingType;
import com.walkmates.model.Seeker;
import com.walkmates.model.Provider;
import com.walkmates.repository.BookingRepository;
import com.walkmates.repository.ListingRepository;
import com.walkmates.repository.ProviderRepository;
import com.walkmates.repository.SeekerRepository;
import com.walkmates.service.BookingService;
import com.walkmates.service.NotificationService;
import com.walkmates.service.PricingCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingServiceTest {

        @Test
        @DisplayName("NEW seeker with one active booking cannot create another booking")
        void newSeekerAtBookingLimitIsRejected() {

                SeekerRepository seekers = mock(SeekerRepository.class);
                ListingRepository listings = mock(ListingRepository.class);
                ProviderRepository providers = mock(ProviderRepository.class);
                BookingRepository bookings = mock(BookingRepository.class);
                PricingCalculator pricing = mock(PricingCalculator.class);
                NotificationService notifications = mock(NotificationService.class);

                BookingService service = new BookingService(
                                seekers,
                                listings,
                                providers,
                                bookings,
                                pricing,
                                notifications);

                Seeker seeker = new Seeker(
                                "you@example.com",
                                "You",
                                "0701234567");

                Listing listing = new Listing(
                                "provider-1",
                                "Dog walk",
                                "Description",
                                ListingType.DOG_WALK);

                Booking existingBooking = new Booking(
                                seeker.getId(),
                                listing.getId(),
                                60);

                when(seekers.findById(seeker.getId()))
                                .thenReturn(Optional.of(seeker));

                when(listings.findById(listing.getId()))
                                .thenReturn(Optional.of(listing));

                when(bookings.findBySeekerId(seeker.getId()))
                                .thenReturn(List.of(existingBooking));

                assertThatThrownBy(() -> service.createBooking(
                                seeker.getId(),
                                listing.getId(),
                                60))
                                .isInstanceOf(BookingService.BookingRejectedException.class)
                                .hasMessageContaining("Seeker booking limit reached");
        }

        @Test
        @DisplayName("Successful booking sends notification")
        void successfulBookingSendsNotification() {

                SeekerRepository seekers = mock(SeekerRepository.class);
                ListingRepository listings = mock(ListingRepository.class);
                ProviderRepository providers = mock(ProviderRepository.class);
                BookingRepository bookings = mock(BookingRepository.class);
                PricingCalculator pricing = mock(PricingCalculator.class);
                NotificationService notifications = mock(NotificationService.class);

                BookingService service = new BookingService(
                                seekers,
                                listings,
                                providers,
                                bookings,
                                pricing,
                                notifications);

                Seeker seeker = new Seeker(
                                "you@example.com",
                                "You",
                                "0701234567");

                seeker.addFunds(500);

                Listing listing = new Listing(
                                "provider-1",
                                "Dog walk",
                                "Description",
                                ListingType.DOG_WALK);

                Provider provider = mock(Provider.class);

                when(provider.getCapacity()).thenReturn(3);

                when(seekers.findById(seeker.getId())).thenReturn(Optional.of(seeker));

                when(listings.findById(listing.getId())).thenReturn(Optional.of(listing));

                when(bookings.findBySeekerId(seeker.getId())).thenReturn(List.of());

                when(providers.findById("provider-1")).thenReturn(Optional.of(provider));

                when(listings.findByProviderId("provider-1")).thenReturn(List.of(listing));

                when(bookings.findByListingId(listing.getId())).thenReturn(List.of());

                when(pricing.priceFor(any(Booking.class), eq(listing), eq(seeker))).thenReturn(100.00);

                Booking result = service.createBooking(
                                seeker.getId(),
                                listing.getId(),
                                60);

                verify(notifications).sendBookingConfirmed(
                                seeker,
                                result);
        }
}
