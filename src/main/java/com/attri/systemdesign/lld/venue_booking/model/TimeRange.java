package com.attri.systemdesign.lld.venue_booking.model;

import com.attri.systemdesign.lld.venue_booking.exception.InvalidRequestException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A half-open interval {@code [start, end)}.
 * <p>
 * Half-open is what makes back-to-back slots (18:00-19:00 and 19:00-20:00) non-overlapping,
 * so the whole double-booking guard reduces to {@link #overlaps(TimeRange)}.
 */
public record TimeRange(LocalDateTime start, LocalDateTime end) implements Comparable<TimeRange> {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter TIME_ONLY = DateTimeFormatter.ofPattern("HH:mm");

    public TimeRange {
        if (start == null || end == null) {
            throw new InvalidRequestException("Time range requires both a start and an end");
        }
        if (!end.isAfter(start)) {
            throw new InvalidRequestException("Time range end must be after start, got " + start + " -> " + end);
        }
    }

    public boolean overlaps(TimeRange other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }

    /** True when this range fits entirely inside {@code window}, used for the "preferred time" filter. */
    public boolean isWithin(TimeRange window) {
        return !start.isBefore(window.start) && !end.isAfter(window.end);
    }

    public boolean startsAfter(LocalDateTime instant) {
        return start.isAfter(instant);
    }

    public Duration duration() {
        return Duration.between(start, end);
    }

    @Override
    public int compareTo(TimeRange other) {
        int byStart = start.compareTo(other.start);
        return byStart != 0 ? byStart : end.compareTo(other.end);
    }

    @Override
    public String toString() {
        return DATE_TIME.format(start) + "-" + TIME_ONLY.format(end);
    }
}
