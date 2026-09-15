package com.attri.systemdesign.venue_booking.service;

import pop.machine.coding.exception.InvalidRequestException;
import pop.machine.coding.model.Sport;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

/**
 * The three filters the PRD asks for - city, sport and preferred time - plus the date they apply to.
 * <p>
 * Every filter except {@code date} is optional; an absent filter matches everything. Built through
 * {@link Builder} so callers name what they are filtering on instead of passing four positional
 * arguments, three of which are usually null.
 */
public final class SearchCriteria {

    private final String city;
    private final Sport sport;
    private final LocalDate date;
    private final LocalTime preferredFrom;
    private final LocalTime preferredTo;

    private SearchCriteria(Builder builder) {
        this.city = builder.city;
        this.sport = builder.sport;
        this.date = builder.date;
        this.preferredFrom = builder.preferredFrom;
        this.preferredTo = builder.preferredTo;
    }

    public static Builder onDate(LocalDate date) {
        return new Builder(date);
    }

    public Optional<String> city() {
        return Optional.ofNullable(city);
    }

    public Optional<Sport> sport() {
        return Optional.ofNullable(sport);
    }

    public LocalDate date() {
        return date;
    }

    /** Start of the preferred window; defaults to the beginning of the day. */
    public LocalTime preferredFrom() {
        return preferredFrom == null ? LocalTime.MIN : preferredFrom;
    }

    /** End of the preferred window; defaults to the end of the day. */
    public LocalTime preferredTo() {
        return preferredTo == null ? LocalTime.MAX : preferredTo;
    }

    public static final class Builder {

        private final LocalDate date;
        private String city;
        private Sport sport;
        private LocalTime preferredFrom;
        private LocalTime preferredTo;

        private Builder(LocalDate date) {
            if (date == null) {
                throw new InvalidRequestException("A search date is required");
            }
            this.date = date;
        }

        public Builder inCity(String city) {
            this.city = (city == null || city.isBlank()) ? null : city.strip();
            return this;
        }

        public Builder forSport(Sport sport) {
            this.sport = sport;
            return this;
        }

        /** Only return slots that fit entirely between {@code from} and {@code to}. */
        public Builder between(LocalTime from, LocalTime to) {
            if (from != null && to != null && !to.isAfter(from)) {
                throw new InvalidRequestException("Preferred window end must be after start, got " + from + " -> " + to);
            }
            this.preferredFrom = from;
            this.preferredTo = to;
            return this;
        }

        public SearchCriteria build() {
            return new SearchCriteria(this);
        }
    }
}
