package com.attri.systemdesign.lld.venue_booking.repository.inmemory;

import com.attri.systemdesign.lld.venue_booking.model.Venue;
import com.attri.systemdesign.lld.venue_booking.repository.VenueRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Venues plus a secondary index on city.
 * <p>
 * Search always starts from a city, so scanning every venue in the country to answer
 * "courts in Bengaluru" would be the wrong access path. The index turns that into a hash lookup.
 * It is safe to keep because {@code Venue.city} is immutable.
 */
public final class InMemoryVenueRepository implements VenueRepository {

    private final Map<String, Venue> venuesById = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> venueIdsByCity = new ConcurrentHashMap<>();

    @Override
    public Venue save(Venue venue) {
        venuesById.put(venue.id(), venue);
        venueIdsByCity.computeIfAbsent(cityKey(venue.city()), key -> ConcurrentHashMap.newKeySet())
                .add(venue.id());
        return venue;
    }

    @Override
    public Optional<Venue> findById(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(venuesById.get(id));
    }

    @Override
    public List<Venue> findByCity(String city) {
        if (city == null || city.isBlank()) {
            return List.of();
        }
        List<Venue> venues = new ArrayList<>();
        for (String id : venueIdsByCity.getOrDefault(cityKey(city), Set.of())) {
            Venue venue = venuesById.get(id);
            if (venue != null) {
                venues.add(venue);
            }
        }
        return venues;
    }

    @Override
    public List<Venue> findAll() {
        return List.copyOf(venuesById.values());
    }

    private static String cityKey(String city) {
        return city.strip().toLowerCase(Locale.ROOT);
    }
}
