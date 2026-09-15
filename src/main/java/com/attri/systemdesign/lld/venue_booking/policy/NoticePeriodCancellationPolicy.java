package com.attri.systemdesign.lld.venue_booking.policy;

import com.attri.systemdesign.lld.venue_booking.exception.InvalidRequestException;
import com.attri.systemdesign.lld.venue_booking.model.Booking;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Cancellation is allowed only up to a fixed notice period before the slot starts.
 * <p>
 * Both examples in the PRD are this same rule with a different duration:
 * <ul>
 *   <li>Venue A - "free cancellation until 2 hours before" is {@code of(Duration.ofHours(2))}</li>
 *   <li>Venue B - "no cancellation within 12 hours" is {@code of(Duration.ofHours(12))}</li>
 * </ul>
 */
public final class NoticePeriodCancellationPolicy implements CancellationPolicy {

    private final Duration minimumNotice;

    private NoticePeriodCancellationPolicy(Duration minimumNotice) {
        if (minimumNotice == null || minimumNotice.isNegative()) {
            throw new InvalidRequestException("Minimum notice must be a non-negative duration");
        }
        this.minimumNotice = minimumNotice;
    }

    public static NoticePeriodCancellationPolicy of(Duration minimumNotice) {
        return new NoticePeriodCancellationPolicy(minimumNotice);
    }

    public static NoticePeriodCancellationPolicy ofHours(long hours) {
        return of(Duration.ofHours(hours));
    }

    public Duration minimumNotice() {
        return minimumNotice;
    }

    @Override
    public CancellationDecision evaluate(Booking booking, LocalDateTime now) {
        LocalDateTime deadline = booking.slot().start().minus(minimumNotice);
        if (now.isAfter(deadline)) {
            return CancellationDecision.deny(
                    "Cancellation closed at " + deadline + " (" + minimumNotice.toHours()
                            + "h before start); it is now " + now);
        }
        return CancellationDecision.allow("Within the free cancellation window, until " + deadline);
    }

    @Override
    public String description() {
        return "Free cancellation until " + minimumNotice.toHours() + "h before start";
    }
}
