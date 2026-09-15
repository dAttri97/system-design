package com.attri.systemdesign.lld.venue_booking.repository;


import com.attri.systemdesign.lld.venue_booking.model.User;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(String id);
}
