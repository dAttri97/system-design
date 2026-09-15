package com.attri.systemdesign.lld.venue_booking.repository.inmemory;


import com.attri.systemdesign.lld.venue_booking.model.Booking;
import com.attri.systemdesign.lld.venue_booking.repository.BookingRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class InMemoryBookingRepository implements BookingRepository {

    private final Map<String, Booking> bookingsById = new ConcurrentHashMap<>();
    private final Map<String, Queue<Booking>> bookingsByUserId = new ConcurrentHashMap<>();

    @Override
    public Booking save(Booking booking) {
        // Bookings are append-only here; status changes mutate the object already in the map.
        if (bookingsById.putIfAbsent(booking.id(), booking) == null) {
            bookingsByUserId.computeIfAbsent(booking.userId(), key -> new ConcurrentLinkedQueue<>())
                    .add(booking);
        }
        return booking;
    }

    @Override
    public Optional<Booking> findById(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(bookingsById.get(id));
    }

    @Override
    public List<Booking> findByUserId(String userId) {
        if (userId == null) {
            return List.of();
        }
        List<Booking> bookings = new ArrayList<>(bookingsByUserId.getOrDefault(userId, new ConcurrentLinkedQueue<>()));
        bookings.sort(Comparator.comparing(Booking::bookedAt).reversed());
        return bookings;
    }
}
