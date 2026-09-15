package com.attri.systemdesign.lld.venue_booking.policy;

/**
 * The outcome of applying a {@link CancellationPolicy}.
 * <p>
 * A value rather than an exception, so the UI can ask "can I still cancel this?" without
 * catching anything; only the service layer turns a denial into an exception.
 */
public record CancellationDecision(boolean allowed, String reason) {

    public static CancellationDecision allow(String reason) {
        return new CancellationDecision(true, reason);
    }

    public static CancellationDecision deny(String reason) {
        return new CancellationDecision(false, reason);
    }
}
