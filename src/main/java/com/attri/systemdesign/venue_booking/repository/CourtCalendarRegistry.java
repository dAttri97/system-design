package com.attri.systemdesign.venue_booking.repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hands out the one {@link CourtCalendar} per court.
 * <p>
 * {@code computeIfAbsent} on a {@link ConcurrentHashMap} guarantees a single instance even when
 * two threads book a brand-new court simultaneously - if each got its own calendar, each would
 * hold its own lock and the double-booking guard would silently do nothing.
 */
public final class CourtCalendarRegistry {

    private final Map<String, CourtCalendar> calendarsByCourtId = new ConcurrentHashMap<>();

    public CourtCalendar forCourt(String courtId) {
        return calendarsByCourtId.computeIfAbsent(courtId, CourtCalendar::new);
    }
}
