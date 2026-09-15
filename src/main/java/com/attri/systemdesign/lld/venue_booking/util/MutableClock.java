package com.attri.systemdesign.lld.venue_booking.util;

import java.time.*;

/**
 * A {@link Clock} that can be moved forwards on demand.
 * <p>
 * The cancellation rules are all of the form "N hours before the slot starts", so exercising them
 * otherwise would mean either waiting hours or leaking the current time into assertions. This lets
 * the demo and the tests state "it is now six hours before the game" outright.
 */
public final class MutableClock extends Clock {

    private final ZoneId zone;
    private volatile Instant instant;

    private MutableClock(Instant instant, ZoneId zone) {
        this.instant = instant;
        this.zone = zone;
    }

    public static MutableClock at(LocalDateTime dateTime) {
        ZoneId zone = ZoneId.systemDefault();
        return new MutableClock(dateTime.atZone(zone).toInstant(), zone);
    }

    public void advanceBy(Duration amount) {
        instant = instant.plus(amount);
    }

    public void setTo(LocalDateTime dateTime) {
        instant = dateTime.atZone(zone).toInstant();
    }

    public LocalDateTime currentDateTime() {
        return LocalDateTime.ofInstant(instant, zone);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId newZone) {
        return new MutableClock(instant, newZone);
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
