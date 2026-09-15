package com.attri.systemdesign.venue_booking.service;

import pop.machine.coding.exception.InvalidRequestException;
import pop.machine.coding.exception.ResourceNotFoundException;
import pop.machine.coding.model.User;
import pop.machine.coding.repository.UserRepository;

import java.util.UUID;

public final class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(String name, String email) {
        if (name == null || name.isBlank()) {
            throw new InvalidRequestException("User name must not be blank");
        }
        if (email == null || !email.contains("@")) {
            throw new InvalidRequestException("A valid email is required, got: " + email);
        }
        return userRepository.save(new User(UUID.randomUUID().toString(), name.strip(), email.strip()));
    }

    public User requireUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
