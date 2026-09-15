package com.attri.systemdesign.venue_booking.repository;

import pop.machine.coding.exception.SlotUnavailableException;
import pop.machine.coding.model.Booking;
import pop.machine.coding.model.TimeRange;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The held slots of a single court, and the one place double booking is prevented.
 *
 * <h2>Data structure</h2>
 * A {@link TreeMap} keyed by slot start. Because the map is sorted, an overlap check only has to
 * look at the two neighbours of the candidate start - {@code floorEntry} catches a booking that
 * began earlier and runs into the candidate, {@code ceilingEntry} catches one that begins inside
 * it. That is O(log n) instead of scanning every booking, and it stays correct even if a venue
 * later changes its slot length so that old and new bookings sit on different grids.
 *
 * <h2>Concurrency (PRD bonus 1)</h2>
 * "Check availability" and "take the slot" must be one indivisible step; if they are not, two
 * threads can both observe a free slot and both write. The lock here makes that pair atomic.
 * <p>
 * The lock is <em>per court</em>, not global, so the contention is limited to users competing
 * for the same court - bookings on different courts proceed fully in parallel. This is the
 * in-memory equivalent of a unique constraint on {@code (court_id, slot_start)} in a database.
 */
public final class CourtCalendar {

    private final String courtId;
    private final NavigableMap<LocalDateTime, Booking> heldSlots = new TreeMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    public CourtCalendar(String courtId) {
        this.courtId = courtId;
    }

    public String courtId() {
        return courtId;
    }

    /**
     * Atomically claims the booking's slot.
     *
     * @throws SlotUnavailableException if any confirmed booking already overlaps it
     */
    public void reserve(Booking booking) {
        lock.lock();
        try {
            Booking conflict = findConflict(booking.slot());
            if (conflict != null) {
                throw new SlotUnavailableException(
                        "Slot " + booking.slot() + " on court " + courtId
                                + " is already booked (booking " + conflict.id() + ")");
            }
            heldSlots.put(booking.slot().start(), booking);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Frees the slot again, but only if this exact booking still holds it - a stale release must
     * never evict whoever booked the slot afterwards.
     */
    public void release(Booking booking) {
        lock.lock();
        try {
            heldSlots.remove(booking.slot().start(), booking);
        } finally {
            lock.unlock();
        }
    }

    public boolean isAvailable(TimeRange slot) {
        lock.lock();
        try {
            return findConflict(slot) == null;
        } finally {
            lock.unlock();
        }
    }

    /** Snapshot of the slots held on {@code date}; safe to iterate after the lock is dropped. */
    public List<TimeRange> heldSlotsOn(LocalDate date) {
        lock.lock();
        try {
            List<TimeRange> held = new ArrayList<>();
            heldSlots.subMap(date.atStartOfDay(), true, date.plusDays(1).atStartOfDay(), false)
                    .values()
                    .forEach(booking -> held.add(booking.slot()));
            return held;
        } finally {
            lock.unlock();
        }
    }

    /** Must be called with the lock held. */
    private Booking findConflict(TimeRange slot) {
        Map.Entry<LocalDateTime, Booking> earlier = heldSlots.floorEntry(slot.start());
        if (earlier != null && earlier.getValue().slot().overlaps(slot)) {
            return earlier.getValue();
        }
        Map.Entry<LocalDateTime, Booking> later = heldSlots.ceilingEntry(slot.start());
        if (later != null && later.getValue().slot().overlaps(slot)) {
            return later.getValue();
        }
        return null;
    }
}
