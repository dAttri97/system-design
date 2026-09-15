package com.attri.systemdesign.venue_booking.policy;

import pop.machine.coding.model.Booking;

import java.time.LocalDateTime;

/** Cancel any time, right up to the start of the slot. The default for a new venue. */
public final class FreeCancellationPolicy implements CancellationPolicy {

    public static final FreeCancellationPolicy INSTANCE = new FreeCancellationPolicy();

    private FreeCancellationPolicy() {
    }

    @Override
    public CancellationDecision evaluate(Booking booking, LocalDateTime now) {
        return CancellationDecision.allow("Free cancellation any time before the slot starts");
    }

    @Override
    public String description() {
        return "Free cancellation";
    }
}
