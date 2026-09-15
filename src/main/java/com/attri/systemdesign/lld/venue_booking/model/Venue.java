package com.attri.systemdesign.lld.venue_booking.model;

import com.attri.systemdesign.lld.venue_booking.exception.InvalidRequestException;
import com.attri.systemdesign.lld.venue_booking.policy.CancellationPolicy;

import java.util.Objects;

/**
 * A physical location owned by a user and hosting one or more {@link Court}s.
 * <p>
 * The cancellation policy lives here rather than on the booking, because the PRD's bonus
 * requirement is per-venue: "Venue A allows free cancellation until 2 hours before,
 * Venue B allows none within 12 hours".
 * <p>
 * {@code city} is immutable: the venue repository maintains a city index and re-keying it on
 * update would be a silent source of stale search results. A venue that physically moves is a
 * new venue.
 */
public final class Venue {

    private final String id;
    private final String ownerId;
    private final String city;
    private String name;
    private String address;
    private CancellationPolicy cancellationPolicy;

    public Venue(String id, String ownerId, String name, String city, String address,
                 CancellationPolicy cancellationPolicy) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = requireText(name, "Venue name");
        this.city = requireText(city, "Venue city");
        this.address = requireText(address, "Venue address");
        this.cancellationPolicy = Objects.requireNonNull(cancellationPolicy, "cancellationPolicy");
    }

    public String id() {
        return id;
    }

    public String ownerId() {
        return ownerId;
    }

    public String name() {
        return name;
    }

    public String city() {
        return city;
    }

    public String address() {
        return address;
    }

    public CancellationPolicy cancellationPolicy() {
        return cancellationPolicy;
    }

    public void rename(String newName) {
        this.name = requireText(newName, "Venue name");
    }

    public void relocateWithinCity(String newAddress) {
        this.address = requireText(newAddress, "Venue address");
    }

    public void applyCancellationPolicy(CancellationPolicy policy) {
        this.cancellationPolicy = Objects.requireNonNull(policy, "cancellationPolicy");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidRequestException(field + " must not be blank");
        }
        return value.strip();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Venue venue && id.equals(venue.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return name + " (" + city + ")";
    }
}
