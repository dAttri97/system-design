package com.attri.systemdesign.lld.venue_booking.repository.inmemory;

import com.attri.systemdesign.lld.venue_booking.model.User;
import com.attri.systemdesign.lld.venue_booking.repository.UserRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryUserRepository implements UserRepository {

    private final Map<String, User> usersById = new ConcurrentHashMap<>();

    @Override
    public User save(User user) {
        usersById.put(user.id(), user);
        return user;
    }

    @Override
    public Optional<User> findById(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(usersById.get(id));
    }
}
