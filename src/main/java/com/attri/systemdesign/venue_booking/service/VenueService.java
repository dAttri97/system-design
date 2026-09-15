package com.attri.systemdesign.venue_booking.service;

import pop.machine.coding.exception.InvalidRequestException;
import pop.machine.coding.exception.ResourceNotFoundException;
import pop.machine.coding.model.Court;
import pop.machine.coding.model.CourtSchedule;
import pop.machine.coding.model.Sport;
import pop.machine.coding.model.Venue;
import pop.machine.coding.policy.CancellationPolicy;
import pop.machine.coding.policy.FreeCancellationPolicy;
import pop.machine.coding.repository.CourtRepository;
import pop.machine.coding.repository.VenueRepository;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Registration and upkeep of venues and their courts (PRD requirements 1 and 2).
 * <p>
 * Authentication is out of scope, but every mutation still checks that the caller is the
 * recorded owner - that is a domain rule about who may change a venue, not an auth concern,
 * and dropping it would let one owner silently edit another's courts.
 */
public final class VenueService {

    private final VenueRepository venueRepository;
    private final CourtRepository courtRepository;
    private final UserService userService;

    public VenueService(VenueRepository venueRepository, CourtRepository courtRepository, UserService userService) {
        this.venueRepository = venueRepository;
        this.courtRepository = courtRepository;
        this.userService = userService;
    }

    public Venue registerVenue(String ownerId, String name, String city, String address) {
        return registerVenue(ownerId, name, city, address, FreeCancellationPolicy.INSTANCE);
    }

    public Venue registerVenue(String ownerId, String name, String city, String address,
                               CancellationPolicy cancellationPolicy) {
        userService.requireUser(ownerId);
        Venue venue = new Venue(UUID.randomUUID().toString(), ownerId, name, city, address, cancellationPolicy);
        return venueRepository.save(venue);
    }

    public Court addCourt(String ownerId, String venueId, String courtName, Sport sport, CourtSchedule schedule) {
        Venue venue = requireOwnedVenue(ownerId, venueId);
        Court court = new Court(UUID.randomUUID().toString(), venue.id(), courtName, sport, schedule);
        return courtRepository.save(court);
    }

    public Venue renameVenue(String ownerId, String venueId, String newName) {
        Venue venue = requireOwnedVenue(ownerId, venueId);
        venue.rename(newName);
        return venue;
    }

    public Venue updateAddress(String ownerId, String venueId, String newAddress) {
        Venue venue = requireOwnedVenue(ownerId, venueId);
        venue.relocateWithinCity(newAddress);
        return venue;
    }

    public Venue updateCancellationPolicy(String ownerId, String venueId, CancellationPolicy policy) {
        Venue venue = requireOwnedVenue(ownerId, venueId);
        venue.applyCancellationPolicy(policy);
        return venue;
    }

    /**
     * Takes a court in or out of service. Existing bookings are intentionally left alone -
     * honouring them is the venue's problem, and silently cancelling a user's slot would be worse
     * than leaving it standing.
     */
    public Court setCourtActive(String ownerId, String courtId, boolean active) {
        Court court = requireCourt(courtId);
        requireOwnedVenue(ownerId, court.venueId());
        court.setActive(active);
        return court;
    }

    public Venue requireVenue(String venueId) {
        return venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", venueId));
    }

    public Court requireCourt(String courtId) {
        return courtRepository.findById(courtId)
                .orElseThrow(() -> new ResourceNotFoundException("Court", courtId));
    }

    /** Courts of a venue, in a stable order so output and tests are deterministic. */
    public List<Court> courtsOf(String venueId) {
        requireVenue(venueId);
        return courtRepository.findByVenueId(venueId).stream()
                .sorted(Comparator.comparing(Court::name))
                .toList();
    }

    private Venue requireOwnedVenue(String ownerId, String venueId) {
        Venue venue = requireVenue(venueId);
        if (!venue.ownerId().equals(ownerId)) {
            throw new InvalidRequestException(
                    "User " + ownerId + " does not own venue " + venueId);
        }
        return venue;
    }
}
