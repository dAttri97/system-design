package com.attri.systemdesign.lld.venue_booking.service;

import com.attri.systemdesign.lld.venue_booking.model.AvailableSlot;
import com.attri.systemdesign.lld.venue_booking.model.Court;
import com.attri.systemdesign.lld.venue_booking.model.TimeRange;
import com.attri.systemdesign.lld.venue_booking.model.Venue;
import com.attri.systemdesign.lld.venue_booking.repository.CourtCalendarRegistry;
import com.attri.systemdesign.lld.venue_booking.repository.CourtRepository;
import com.attri.systemdesign.lld.venue_booking.repository.VenueRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Discovery of free slots (PRD requirement 3).
 * <p>
 * Read-only: it never mutates a calendar. A slot it returns can still be taken by someone else
 * before the user acts on it, which is exactly why {@code BookingService} re-checks availability
 * atomically rather than trusting a search result.
 */
public final class SearchService {

    private final VenueRepository venueRepository;
    private final CourtRepository courtRepository;
    private final CourtCalendarRegistry calendars;
    private final Clock clock;

    public SearchService(VenueRepository venueRepository, CourtRepository courtRepository,
                         CourtCalendarRegistry calendars, Clock clock) {
        this.venueRepository = venueRepository;
        this.courtRepository = courtRepository;
        this.calendars = calendars;
        this.clock = clock;
    }

    /** Free slots matching the criteria, earliest first, then by venue and court name. */
    public List<AvailableSlot> search(SearchCriteria criteria) {
        LocalDateTime now = LocalDateTime.now(clock);
        TimeRange preferredWindow = new TimeRange(
                criteria.date().atTime(criteria.preferredFrom()),
                criteria.date().atTime(criteria.preferredTo()));

        List<AvailableSlot> results = new ArrayList<>();
        for (Venue venue : candidateVenues(criteria)) {
            for (Court court : courtRepository.findByVenueId(venue.id())) {
                if (!court.isActive() || !matchesSport(criteria, court)) {
                    continue;
                }
                collectFreeSlots(venue, court, criteria, preferredWindow, now, results);
            }
        }

        results.sort(Comparator.comparing((AvailableSlot result) -> result.slot().start())
                .thenComparing(AvailableSlot::venueName)
                .thenComparing(AvailableSlot::courtName));
        return results;
    }

    private void collectFreeSlots(Venue venue, Court court, SearchCriteria criteria,
                                  TimeRange preferredWindow, LocalDateTime now,
                                  List<AvailableSlot> results) {
        // One snapshot per court instead of a lock acquisition per candidate slot.
        List<TimeRange> held = calendars.forCourt(court.id()).heldSlotsOn(criteria.date());

        for (TimeRange slot : court.schedule().slotsOn(criteria.date())) {
            if (!slot.startsAfter(now) || !slot.isWithin(preferredWindow)) {
                continue;
            }
            if (held.stream().anyMatch(slot::overlaps)) {
                continue;
            }
            results.add(new AvailableSlot(venue.id(), venue.name(), venue.city(),
                    court.id(), court.name(), court.sport(), slot));
        }
    }

    private List<Venue> candidateVenues(SearchCriteria criteria) {
        return criteria.city()
                .map(venueRepository::findByCity)
                .orElseGet(venueRepository::findAll);
    }

    private static boolean matchesSport(SearchCriteria criteria, Court court) {
        return criteria.sport().map(sport -> sport == court.sport()).orElse(true);
    }
}
