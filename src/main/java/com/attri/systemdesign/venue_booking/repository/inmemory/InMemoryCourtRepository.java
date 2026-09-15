package com.attri.systemdesign.venue_booking.repository.inmemory;

import pop.machine.coding.model.Court;
import pop.machine.coding.repository.CourtRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Courts plus the venue-to-courts index that {@code SearchService} walks. */
public final class InMemoryCourtRepository implements CourtRepository {

    private final Map<String, Court> courtsById = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> courtIdsByVenueId = new ConcurrentHashMap<>();

    @Override
    public Court save(Court court) {
        courtsById.put(court.id(), court);
        courtIdsByVenueId.computeIfAbsent(court.venueId(), key -> ConcurrentHashMap.newKeySet())
                .add(court.id());
        return court;
    }

    @Override
    public Optional<Court> findById(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(courtsById.get(id));
    }

    @Override
    public List<Court> findByVenueId(String venueId) {
        if (venueId == null) {
            return List.of();
        }
        List<Court> courts = new ArrayList<>();
        for (String id : courtIdsByVenueId.getOrDefault(venueId, Set.of())) {
            Court court = courtsById.get(id);
            if (court != null) {
                courts.add(court);
            }
        }
        return courts;
    }
}
