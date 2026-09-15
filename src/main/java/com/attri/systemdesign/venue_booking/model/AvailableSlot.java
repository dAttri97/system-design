package com.attri.systemdesign.venue_booking.model;

/**
 * A search result: one free slot on one court, denormalised with the venue details a caller
 * needs to render it without a second lookup.
 */
public record AvailableSlot(String venueId,
                            String venueName,
                            String city,
                            String courtId,
                            String courtName,
                            Sport sport,
                            TimeRange slot) {

    @Override
    public String toString() {
        return "%s / %s (%s) @ %s".formatted(venueName, courtName, sport, slot);
    }
}
