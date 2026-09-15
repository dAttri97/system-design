package com.attri.systemdesign.lld.venue_booking;

import com.attri.systemdesign.lld.venue_booking.repository.*;
import com.attri.systemdesign.lld.venue_booking.repository.inmemory.InMemoryBookingRepository;
import com.attri.systemdesign.lld.venue_booking.repository.inmemory.InMemoryCourtRepository;
import com.attri.systemdesign.lld.venue_booking.repository.inmemory.InMemoryUserRepository;
import com.attri.systemdesign.lld.venue_booking.repository.inmemory.InMemoryVenueRepository;
import com.attri.systemdesign.lld.venue_booking.service.BookingService;
import com.attri.systemdesign.lld.venue_booking.service.SearchService;
import com.attri.systemdesign.lld.venue_booking.service.UserService;
import com.attri.systemdesign.lld.venue_booking.service.VenueService;

import java.time.Clock;

/**
 * Composition root: wires the in-memory repositories into the four services.
 * <p>
 * Nothing below this class knows how the object graph is built, so swapping the in-memory
 * repositories for persistent ones - or handing the whole thing to a DI container - is a change
 * confined to this file. The {@link Clock} is a constructor parameter purely so tests can drive
 * time forwards and backwards.
 */
public final class BookingSystem {

    private final UserService userService;
    private final VenueService venueService;
    private final SearchService searchService;
    private final BookingService bookingService;

    public BookingSystem() {
        this(Clock.systemDefaultZone());
    }

    public BookingSystem(Clock clock) {
        UserRepository userRepository = new InMemoryUserRepository();
        VenueRepository venueRepository = new InMemoryVenueRepository();
        CourtRepository courtRepository = new InMemoryCourtRepository();
        BookingRepository bookingRepository = new InMemoryBookingRepository();
        CourtCalendarRegistry calendars = new CourtCalendarRegistry();

        this.userService = new UserService(userRepository);
        this.venueService = new VenueService(venueRepository, courtRepository, userService);
        this.searchService = new SearchService(venueRepository, courtRepository, calendars, clock);
        this.bookingService = new BookingService(bookingRepository, calendars, venueService, userService, clock);
    }

    public UserService users() {
        return userService;
    }

    public VenueService venues() {
        return venueService;
    }

    public SearchService search() {
        return searchService;
    }

    public BookingService bookings() {
        return bookingService;
    }
}
