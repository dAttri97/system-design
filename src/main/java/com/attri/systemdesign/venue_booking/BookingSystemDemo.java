package com.attri.systemdesign.venue_booking;

import pop.machine.coding.exception.BookingSystemException;
import pop.machine.coding.model.*;
import pop.machine.coding.policy.NoticePeriodCancellationPolicy;
import pop.machine.coding.service.SearchCriteria;
import pop.machine.coding.util.MutableClock;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A scripted walk through every requirement in the PRD, printed to stdout.
 * <p>
 * Runs on a {@link MutableClock} anchored to a fixed date so the output is identical on every run.
 */
public final class BookingSystemDemo {

    private static final LocalDate MATCH_DAY = LocalDate.of(2026, 8, 20);
    private static final LocalDateTime START_OF_STORY = MATCH_DAY.atTime(9, 0);

    private final MutableClock clock = MutableClock.at(START_OF_STORY);
    private final BookingSystem system = new BookingSystem(clock);

    public static void main(String[] args) {
        BookingSystemDemo demo = new BookingSystemDemo();
        demo.run();
    }

    private void run() {
        User owner = system.users().register("Priya Owner", "priya@venues.in");
        User alice = system.users().register("Alice", "alice@example.com");
        User bob = system.users().register("Bob", "bob@example.com");

        Venue smashArena = system.venues().registerVenue(owner.id(), "Smash Arena", "Bengaluru",
                "12 Residency Road", NoticePeriodCancellationPolicy.ofHours(2));
        Venue baselineClub = system.venues().registerVenue(owner.id(), "Baseline Club", "Bengaluru",
                "8 Indiranagar 100ft Road", NoticePeriodCancellationPolicy.ofHours(12));
        Venue turfNation = system.venues().registerVenue(owner.id(), "Turf Nation", "Pune",
                "45 Baner Road");

        CourtSchedule hourlySlots = new CourtSchedule(LocalTime.of(6, 0), LocalTime.of(22, 0), Duration.ofHours(1));
        CourtSchedule halfHourSlots = new CourtSchedule(LocalTime.of(6, 0), LocalTime.of(23, 0), Duration.ofMinutes(30));

        Court smashOne = system.venues().addCourt(owner.id(), smashArena.id(), "Badminton Court 1", Sport.BADMINTON, hourlySlots);
        system.venues().addCourt(owner.id(), smashArena.id(), "Badminton Court 2", Sport.BADMINTON, halfHourSlots);
        system.venues().addCourt(owner.id(), smashArena.id(), "Tennis Court", Sport.TENNIS, hourlySlots);
        Court baselineOne = system.venues().addCourt(owner.id(), baselineClub.id(), "Clay Court 1", Sport.TENNIS, hourlySlots);
        system.venues().addCourt(owner.id(), turfNation.id(), "5-a-side Turf", Sport.FOOTBALL, hourlySlots);

        heading("1-2. Venues and courts registered");
        for (Venue venue : List.of(smashArena, baselineClub, turfNation)) {
            System.out.println("   " + venue + " - " + venue.cancellationPolicy().description());
            system.venues().courtsOf(venue.id())
                    .forEach(court -> System.out.println("      " + court + " " + court.schedule()));
        }

        heading("3. Search: badminton in Bengaluru, 18:00-21:00");
        SearchCriteria eveningBadminton = SearchCriteria.onDate(MATCH_DAY)
                .inCity("bengaluru")
                .forSport(Sport.BADMINTON)
                .between(LocalTime.of(18, 0), LocalTime.of(21, 0))
                .build();
        List<AvailableSlot> available = system.search().search(eveningBadminton);
        printSlots(available);

        heading("4. Alice books the 18:00 slot on " + smashOne.name());
        TimeRange sixPm = smashOne.schedule().slotStartingAt(MATCH_DAY.atTime(18, 0));
        Booking aliceBooking = system.bookings().book(alice.id(), smashOne.id(), sixPm);
        System.out.println("   confirmed: " + aliceBooking);

        heading("4a. Bob tries the same slot - must be rejected");
        expectRejection(() -> system.bookings().book(bob.id(), smashOne.id(), sixPm));

        heading("4b. Same search again - the 18:00 slot on Court 1 is gone");
        printSlots(system.search().search(eveningBadminton));

        heading("5. Alice cancels; the slot returns to the pool");
        system.bookings().cancel(alice.id(), aliceBooking.id());
        System.out.println("   " + aliceBooking);
        System.out.println("   18:00 on Court 1 available again: "
                + system.bookings().isAvailable(smashOne.id(), sixPm));

        heading("Bonus 1. 50 threads race for one slot");
        demonstrateConcurrentBooking(smashOne, MATCH_DAY.atTime(20, 0));

        heading("Bonus 2. Per-venue cancellation policies");
        demonstrateCancellationPolicies(alice, bob, smashOne, baselineOne);

        heading("Edge cases - every one of these is rejected with a clear message");
        demonstrateEdgeCases(alice, owner, smashOne, baselineClub);

        heading("Alice's booking history");
        system.bookings().bookingsOf(alice.id()).forEach(booking -> System.out.println("   " + booking));
    }

    /**
     * The heart of the concurrency requirement: every thread is held at a latch and released at
     * once, so they all attempt the identical slot with maximum overlap. Exactly one may win.
     */
    private void demonstrateConcurrentBooking(Court court, LocalDateTime slotStart) {
        int contenders = 50;
        TimeRange contestedSlot = court.schedule().slotStartingAt(slotStart);
        AtomicInteger booked = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(contenders);

        try (ExecutorService pool = Executors.newFixedThreadPool(16)) {
            for (int i = 0; i < contenders; i++) {
                User racer = system.users().register("Racer " + i, "racer" + i + "@example.com");
                pool.submit(() -> {
                    try {
                        startGun.await();
                        system.bookings().book(racer.id(), court.id(), contestedSlot);
                        booked.incrementAndGet();
                    } catch (BookingSystemException rejection) {
                        rejected.incrementAndGet();
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                    } finally {
                        finished.countDown();
                    }
                });
            }
            startGun.countDown();
            awaitQuietly(finished);
        }

        System.out.println("   contenders : " + contenders);
        System.out.println("   booked     : " + booked.get() + "   <- must be exactly 1");
        System.out.println("   rejected   : " + rejected.get());
        System.out.println("   slot still available? " + system.bookings().isAvailable(court.id(), contestedSlot));
    }

    private void demonstrateCancellationPolicies(User alice, User bob, Court smashCourt, Court baselineCourt) {
        TimeRange smashSlot = smashCourt.schedule().slotStartingAt(MATCH_DAY.atTime(19, 0));
        TimeRange baselineSlot = baselineCourt.schedule().slotStartingAt(MATCH_DAY.atTime(19, 0));
        Booking atSmash = system.bookings().book(alice.id(), smashCourt.id(), smashSlot);
        Booking atBaseline = system.bookings().book(bob.id(), baselineCourt.id(), baselineSlot);

        System.out.println("   both booked for 19:00; clock is " + clock.currentDateTime());
        System.out.println("   Smash Arena  (2h notice)  -> " + system.bookings().cancellationOutlook(atSmash.id()));
        System.out.println("   Baseline Club (12h notice) -> " + system.bookings().cancellationOutlook(atBaseline.id()));

        clock.setTo(MATCH_DAY.atTime(18, 30));
        System.out.println("\n   fast-forward to " + clock.currentDateTime() + " (30 minutes before tip-off)");

        System.out.print("   Alice cancels at Smash Arena : ");
        expectRejection(() -> system.bookings().cancel(alice.id(), atSmash.id()));

        System.out.print("   Bob cancels at Baseline Club : ");
        expectRejection(() -> system.bookings().cancel(bob.id(), atBaseline.id()));

        clock.setTo(START_OF_STORY);
        System.out.println("\n   rewind to " + clock.currentDateTime() + " - Smash Arena's window is open again");
        system.bookings().cancel(alice.id(), atSmash.id());
        System.out.println("   Alice cancelled: " + atSmash);
    }

    private void demonstrateEdgeCases(User alice, User owner, Court court, Venue otherVenue) {
        System.out.print("   unknown court            : ");
        expectRejection(() -> system.bookings().book(alice.id(), "no-such-court", MATCH_DAY.atTime(18, 0)));

        System.out.print("   off-grid start (18:17)   : ");
        expectRejection(() -> system.bookings().book(alice.id(), court.id(), MATCH_DAY.atTime(18, 17)));

        System.out.print("   outside opening hours    : ");
        expectRejection(() -> system.bookings().book(alice.id(), court.id(), MATCH_DAY.atTime(23, 0)));

        System.out.print("   slot in the past         : ");
        expectRejection(() -> system.bookings().book(alice.id(), court.id(), MATCH_DAY.atTime(7, 0)));

        System.out.print("   non-owner edits a venue  : ");
        expectRejection(() -> system.venues().renameVenue(alice.id(), otherVenue.id(), "Alice's Club"));

        System.out.print("   cancelling someone else's: ");
        Booking mine = system.bookings().book(alice.id(), court.id(), MATCH_DAY.atTime(21, 0));
        expectRejection(() -> system.bookings().cancel(owner.id(), mine.id()));

        System.out.print("   cancelling twice         : ");
        system.bookings().cancel(alice.id(), mine.id());
        expectRejection(() -> system.bookings().cancel(alice.id(), mine.id()));
    }

    private static void printSlots(List<AvailableSlot> slots) {
        if (slots.isEmpty()) {
            System.out.println("   (no slots match)");
            return;
        }
        slots.forEach(slot -> System.out.println("   " + slot));
    }

    private static void expectRejection(Runnable action) {
        try {
            action.run();
            System.out.println("   !! expected a rejection but the call succeeded");
        } catch (BookingSystemException rejected) {
            System.out.println(rejected.getClass().getSimpleName() + ": " + rejected.getMessage());
        }
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                System.out.println("   !! timed out waiting for the racers");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static void heading(String title) {
        System.out.println("\n" + "=".repeat(78));
        System.out.println(title);
        System.out.println("=".repeat(78));
    }
}
