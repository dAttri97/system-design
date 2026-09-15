package com.attri.systemdesign.venue_booking.policy;

import pop.machine.coding.model.Booking;

import java.time.LocalDateTime;

/** Once booked, always booked. Useful for tournament or peak-hour courts. */
public final class NonRefundablePolicy implements CancellationPolicy {

    public static final NonRefundablePolicy INSTANCE = new NonRefundablePolicy();

    private NonRefundablePolicy() {
    }

    @Override
    public CancellationDecision evaluate(Booking booking, LocalDateTime now) {
        return CancellationDecision.deny("This venue does not allow cancellations");
    }

    @Override
    public String description() {
        return "No cancellation";
    }
}
