package com.attri.systemdesign.venue_booking.repository;

import pop.machine.coding.model.Court;

import java.util.List;
import java.util.Optional;

public interface CourtRepository {

    Court save(Court court);

    Optional<Court> findById(String id);

    List<Court> findByVenueId(String venueId);
}
