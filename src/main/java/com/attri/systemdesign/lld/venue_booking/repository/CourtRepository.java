package com.attri.systemdesign.lld.venue_booking.repository;


import com.attri.systemdesign.lld.venue_booking.model.Court;

import java.util.List;
import java.util.Optional;

public interface CourtRepository {

    Court save(Court court);

    Optional<Court> findById(String id);

    List<Court> findByVenueId(String venueId);
}
