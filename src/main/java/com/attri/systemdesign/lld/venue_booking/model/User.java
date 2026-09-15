package com.attri.systemdesign.lld.venue_booking.model;

import java.util.Objects;

/**
 * A platform user. The same type covers players and venue owners, since the PRD puts
 * authentication and roles out of scope.
 */
public final class User {

    private final String id;
    private final String name;
    private final String email;

    public User(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String email() {
        return email;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof User user && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return name + " <" + email + ">";
    }
}
