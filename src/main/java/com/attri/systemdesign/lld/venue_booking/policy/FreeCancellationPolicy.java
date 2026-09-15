package com.attri.systemdesign.lld.venue_booking.policy;

import com.attri.systemdesign.lld.venue_booking.model.Booking;

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
