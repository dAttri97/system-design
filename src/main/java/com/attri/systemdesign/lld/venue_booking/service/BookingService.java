package com.attri.systemdesign.lld.venue_booking.service;

import com.attri.systemdesign.lld.venue_booking.exception.CancellationNotAllowedException;
import com.attri.systemdesign.lld.venue_booking.exception.InvalidRequestException;
import com.attri.systemdesign.lld.venue_booking.exception.ResourceNotFoundException;
import com.attri.systemdesign.lld.venue_booking.exception.SlotUnavailableException;
import com.attri.systemdesign.lld.venue_booking.model.Booking;
import com.attri.systemdesign.lld.venue_booking.model.Court;
import com.attri.systemdesign.lld.venue_booking.model.TimeRange;
import com.attri.systemdesign.lld.venue_booking.model.Venue;
import com.attri.systemdesign.lld.venue_booking.policy.CancellationDecision;
import com.attri.systemdesign.lld.venue_booking.policy.CancellationPolicy;
import com.attri.systemdesign.lld.venue_booking.repository.BookingRepository;
import com.attri.systemdesign.lld.venue_booking.repository.CourtCalendar;
import com.attri.systemdesign.lld.venue_booking.repository.CourtCalendarRegistry;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Reserving and cancelling slots (PRD requirements 4 and 5).
 * <p>
 * The {@link Clock} is injected rather than read from {@code LocalDateTime.now()} so that
 * "is this slot in the past?" and "has the cancellation window closed?" are testable without
 * sleeping or waiting for real time to pass.
 */
public final class BookingService {

    private final BookingRepository bookingRepository;
    private final CourtCalendarRegistry calendars;
    private final VenueService venueService;
    private final UserService userService;
    private final Clock clock;

    public BookingService(BookingRepository bookingRepository, CourtCalendarRegistry calendars,
                          VenueService venueService, UserService userService, Clock clock) {
        this.bookingRepository = bookingRepository;
        this.calendars = calendars;
        this.venueService = venueService;
        this.userService = userService;
        this.clock = clock;
    }

    /** Books the court's slot beginning at {@code slotStart}; the length comes from the court schedule. */
    public Booking book(String userId, String courtId, LocalDateTime slotStart) {
        Court court = venueService.requireCourt(courtId);
        return book(userId, courtId, court.schedule().slotStartingAt(slotStart));
    }

    /**
     * Reserves {@code slot} for {@code userId}.
     * <p>
     * Everything cheap is validated first; the last step is the only one that has to be atomic,
     * so the per-court lock is held for as short a time as possible.
     *
     * @throws SlotUnavailableException if another user holds an overlapping slot
     */
    public Booking book(String userId, String courtId, TimeRange slot) {
        userService.requireUser(userId);
        Court court = venueService.requireCourt(courtId);

        if (!court.isActive()) {
            throw new InvalidRequestException("Court " + court.name() + " is not currently open for booking");
        }
        if (!court.schedule().isValidSlot(slot)) {
            throw new InvalidRequestException(
                    slot + " is not a bookable slot on " + court.name() + " (" + court.schedule() + ")");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (!slot.startsAfter(now)) {
            throw new InvalidRequestException("Cannot book " + slot + " because it starts in the past (now " + now + ")");
        }

        Booking booking = new Booking(UUID.randomUUID().toString(), userId, court.venueId(), courtId, slot, now);
        calendars.forCourt(courtId).reserve(booking);   // atomic check-and-claim; throws if taken
        return bookingRepository.save(booking);
    }

    /**
     * Cancels a confirmed booking and returns the slot to the pool.
     * <p>
     * Order matters: the venue policy is checked first, then the status transition claims the
     * cancellation, and only the thread that won the transition releases the slot. A losing
     * concurrent caller sees "already cancelled" instead of freeing a slot twice.
     */
    public Booking cancel(String userId, String bookingId) {
        Booking booking = requireBooking(bookingId);
        if (!booking.userId().equals(userId)) {
            throw new InvalidRequestException("Booking " + bookingId + " does not belong to user " + userId);
        }
        if (!booking.isConfirmed()) {
            throw new InvalidRequestException("Booking " + bookingId + " is already " + booking.status());
        }

        LocalDateTime now = LocalDateTime.now(clock);
        CancellationDecision decision = cancellationPolicyFor(booking).evaluate(booking, now);
        if (!decision.allowed()) {
            throw new CancellationNotAllowedException(
                    "Booking " + bookingId + " cannot be cancelled: " + decision.reason());
        }

        if (!booking.markCancelled(now)) {
            throw new InvalidRequestException("Booking " + bookingId + " was cancelled by a concurrent request");
        }
        calendars.forCourt(booking.courtId()).release(booking);
        return booking;
    }

    /** Lets a caller show "free cancellation until ..." without attempting the cancellation. */
    public CancellationDecision cancellationOutlook(String bookingId) {
        Booking booking = requireBooking(bookingId);
        return cancellationPolicyFor(booking).evaluate(booking, LocalDateTime.now(clock));
    }

    public boolean isAvailable(String courtId, TimeRange slot) {
        Court court = venueService.requireCourt(courtId);
        return court.isActive()
                && court.schedule().isValidSlot(slot)
                && slot.startsAfter(LocalDateTime.now(clock))
                && calendars.forCourt(courtId).isAvailable(slot);
    }

    /** A user's bookings, most recent first, cancelled ones included. */
    public List<Booking> bookingsOf(String userId) {
        userService.requireUser(userId);
        return bookingRepository.findByUserId(userId);
    }

    public Booking requireBooking(String bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
    }

    private CancellationPolicy cancellationPolicyFor(Booking booking) {
        Venue venue = venueService.requireVenue(booking.venueId());
        return venue.cancellationPolicy();
    }

    /** Exposed for diagnostics; the calendar is the source of truth on what is held. */
    CourtCalendar calendarFor(String courtId) {
        return calendars.forCourt(courtId);
    }
}
