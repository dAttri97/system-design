package com.attri.systemdesign.lld.venue_booking.model;

import com.attri.systemdesign.lld.venue_booking.exception.InvalidRequestException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The rule that defines a court's bookable slots: opening hours plus a fixed slot length.
 * <p>
 * Slots are derived on demand rather than materialised up front, so the system does not have to
 * pre-create rows for every court for every future day. A trailing gap shorter than
 * {@code slotDuration} is simply not offered.
 */
public record CourtSchedule(LocalTime openTime, LocalTime closeTime, Duration slotDuration) {

    public CourtSchedule {
        if (openTime == null || closeTime == null || slotDuration == null) {
            throw new InvalidRequestException("Court schedule requires openTime, closeTime and slotDuration");
        }
        if (!closeTime.isAfter(openTime)) {
            throw new InvalidRequestException(
                    "Court closeTime must be after openTime, got " + openTime + " -> " + closeTime);
        }
        if (slotDuration.isZero() || slotDuration.isNegative()) {
            throw new InvalidRequestException("Slot duration must be positive, got " + slotDuration);
        }
        if (slotDuration.toSecondsPart() != 0 || slotDuration.toNanosPart() != 0) {
            throw new InvalidRequestException("Slot duration must be a whole number of minutes, got " + slotDuration);
        }
        if (slotDuration.compareTo(Duration.between(openTime, closeTime)) > 0) {
            throw new InvalidRequestException("Slot duration " + slotDuration + " exceeds the court's opening hours");
        }
    }

    /** Every slot the court offers on {@code date}, in chronological order. */
    public List<TimeRange> slotsOn(LocalDate date) {
        LocalDateTime dayClose = date.atTime(closeTime);
        List<TimeRange> slots = new ArrayList<>();
        for (LocalDateTime cursor = date.atTime(openTime);
             !cursor.plus(slotDuration).isAfter(dayClose);
             cursor = cursor.plus(slotDuration)) {
            slots.add(new TimeRange(cursor, cursor.plus(slotDuration)));
        }
        return slots;
    }

    /**
     * True when {@code slot} is one of the slots this schedule would generate.
     * Guards against clients inventing arbitrary ranges (wrong length, off-grid start, after hours).
     */
    public boolean isValidSlot(TimeRange slot) {
        LocalDate date = slot.start().toLocalDate();
        // openTime < closeTime are both same-day wall clock times, so a valid slot never crosses midnight.
        if (!slot.end().toLocalDate().equals(date)) {
            return false;
        }
        if (!slot.duration().equals(slotDuration)) {
            return false;
        }
        LocalDateTime dayOpen = date.atTime(openTime);
        if (slot.start().isBefore(dayOpen) || slot.end().isAfter(date.atTime(closeTime))) {
            return false;
        }
        return Duration.between(dayOpen, slot.start()).toMinutes() % slotDuration.toMinutes() == 0;
    }

    /** Convenience for callers that know a start time and want the matching slot. */
    public TimeRange slotStartingAt(LocalDateTime start) {
        TimeRange slot = new TimeRange(start, start.plus(slotDuration));
        if (!isValidSlot(slot)) {
            throw new InvalidRequestException(slot + " is not a bookable slot for a court open "
                    + openTime + "-" + closeTime + " in " + slotDuration.toMinutes() + " minute slots");
        }
        return slot;
    }

    @Override
    public String toString() {
        return openTime + "-" + closeTime + " in " + slotDuration.toMinutes() + "min slots";
    }
}
