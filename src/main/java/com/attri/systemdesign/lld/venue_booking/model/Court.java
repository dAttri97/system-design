package com.attri.systemdesign.lld.venue_booking.model;

import com.attri.systemdesign.lld.venue_booking.exception.InvalidRequestException;

import java.util.Objects;

/**
 * A bookable playing surface. Belongs to exactly one venue and supports exactly one sport,
 * as specified by the PRD.
 * <p>
 * The court holds only a {@code venueId} rather than a {@link Venue} reference; the court
 * repository owns the venue-to-courts index. One direction of navigation keeps the two from
 * drifting out of sync.
 */
public final class Court {

    private final String id;
    private final String venueId;
    private final Sport sport;
    private String name;
    private CourtSchedule schedule;
    private boolean active = true;

    public Court(String id, String venueId, String name, Sport sport, CourtSchedule schedule) {
        this.id = id;
        this.venueId = venueId;
        this.name = requireText(name);
        this.sport = Objects.requireNonNull(sport, "sport");
        this.schedule = Objects.requireNonNull(schedule, "schedule");
    }

    public String id() {
        return id;
    }

    public String venueId() {
        return venueId;
    }

    public String name() {
        return name;
    }

    public Sport sport() {
        return sport;
    }

    public CourtSchedule schedule() {
        return schedule;
    }

    /** An inactive court (maintenance, seasonal closure) disappears from search and rejects new bookings. */
    public boolean isActive() {
        return active;
    }

    public void rename(String newName) {
        this.name = requireText(newName);
    }

    public void reschedule(CourtSchedule newSchedule) {
        this.schedule = Objects.requireNonNull(newSchedule, "schedule");
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    private static String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidRequestException("Court name must not be blank");
        }
        return value.strip();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Court court && id.equals(court.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return name + " [" + sport + "]";
    }
}
