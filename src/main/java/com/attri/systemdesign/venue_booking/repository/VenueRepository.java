package com.attri.systemdesign.venue_booking.repository;

import pop.machine.coding.model.Venue;

import java.util.List;
import java.util.Optional;

public interface VenueRepository {

    Venue save(Venue venue);

    Optional<Venue> findById(String id);

    /** Case- and whitespace-insensitive lookup; city is the primary search axis, so it is indexed. */
    List<Venue> findByCity(String city);

    List<Venue> findAll();
}
