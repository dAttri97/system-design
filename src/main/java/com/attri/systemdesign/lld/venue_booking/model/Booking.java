package com.attri.systemdesign.lld.venue_booking.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A user's reservation of one court slot.
 * <p>
 * The status transition is guarded so that two threads cancelling the same booking cannot both
 * "win": exactly one call to {@link #markCancelled(LocalDateTime)} returns {@code true}, and only
 * that caller goes on to release the slot back to the court calendar.
 */
public final class Booking {

    private final String id;
    private final String userId;
    private final String venueId;
    private final String courtId;
    private final TimeRange slot;
    private final LocalDateTime bookedAt;

    private BookingStatus status = BookingStatus.CONFIRMED;
    private LocalDateTime cancelledAt;

    public Booking(String id, String userId, String venueId, String courtId, TimeRange slot,
                   LocalDateTime bookedAt) {
        this.id = id;
        this.userId = userId;
        this.venueId = venueId;
        this.courtId = courtId;
        this.slot = Objects.requireNonNull(slot, "slot");
        this.bookedAt = bookedAt;
    }

    public String id() {
        return id;
    }

    public String userId() {
        return userId;
    }

    public String venueId() {
        return venueId;
    }

    public String courtId() {
        return courtId;
    }

    public TimeRange slot() {
        return slot;
    }

    public LocalDateTime bookedAt() {
        return bookedAt;
    }

    public synchronized BookingStatus status() {
        return status;
    }

    public synchronized LocalDateTime cancelledAt() {
        return cancelledAt;
    }

    public synchronized boolean isConfirmed() {
        return status == BookingStatus.CONFIRMED;
    }

    /**
     * Atomically moves the booking to {@code CANCELLED}.
     *
     * @return {@code true} if this call performed the transition, {@code false} if it was already cancelled
     */
    public synchronized boolean markCancelled(LocalDateTime at) {
        if (status != BookingStatus.CONFIRMED) {
            return false;
        }
        status = BookingStatus.CANCELLED;
        cancelledAt = at;
        return true;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Booking booking && id.equals(booking.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Booking{" + id + ", court=" + courtId + ", " + slot + ", " + status() + "}";
    }
}
