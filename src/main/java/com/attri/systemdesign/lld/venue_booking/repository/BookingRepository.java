package com.attri.systemdesign.lld.venue_booking.repository;


import com.attri.systemdesign.lld.venue_booking.model.Booking;

import java.util.List;
import java.util.Optional;

/**
 * The full booking history, cancelled ones included.
 * <p>
 * Which slots are actually <em>held</em> is a separate question answered by
 * {@link CourtCalendar} - this repository never decides availability.
 */
public interface BookingRepository {

    Booking save(Booking booking);

    Optional<Booking> findById(String id);

    /** Most recently booked first. */
    List<Booking> findByUserId(String userId);
}
