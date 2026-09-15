package com.attri.systemdesign.venue_booking.repository;

import pop.machine.coding.model.User;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(String id);
}
