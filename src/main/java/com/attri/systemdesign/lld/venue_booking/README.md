# Sports Venue Booking (Playo-style) — in-memory design

Implementation of [prd.txt](prd.txt). No external dependencies; JDK only.

## Layout

```
com.attri.systemdesign.lld.venue_booking
├── BookingSystem            composition root — wires repositories into services
├── BookingSystemDemo        runnable walk-through of every requirement
├── model/                   Venue, Court, CourtSchedule, Booking, TimeRange, AvailableSlot, User, Sport
├── policy/                  CancellationPolicy strategy + three implementations
├── service/                 UserService, VenueService, SearchService, BookingService
├── repository/              interfaces + CourtCalendar (the double-booking guard)
│   └── inmemory/            ConcurrentHashMap-backed implementations
├── exception/               BookingSystemException hierarchy
└── util/MutableClock        injectable clock, so time-based rules are testable
```

Services depend on repository *interfaces*, so replacing the in-memory store with a database
touches only `BookingSystem` and the `inmemory` package.

## Design decisions worth calling out

**Slots are derived, not stored.** A `Court` holds a `CourtSchedule` (open time, close time, slot
length) and generates its slots for a given date on demand. Pre-materialising slot rows for every
court for every future day would be a lot of state to keep for something a pure function computes.
`CourtSchedule.isValidSlot` then rejects anything off-grid, the wrong length, or after hours.

**`TimeRange` is half-open, `[start, end)`.** That is what makes 18:00–19:00 and 19:00–20:00
non-overlapping, and reduces the entire double-booking question to `TimeRange.overlaps`.

**One `CourtCalendar` per court, holding a `TreeMap` keyed by slot start.** Because the map is
sorted, an overlap check only inspects the two neighbours of the candidate start — `floorEntry`
for a booking that began earlier and runs in, `ceilingEntry` for one that begins inside. O(log n),
and still correct if a venue later changes its slot length so old and new bookings sit on
different grids.

**Availability lives in the calendar, history lives in `BookingRepository`.** Cancelled bookings
stay in the repository forever but leave the calendar immediately, so "what did I book?" and
"what is free?" never have to filter each other's data.

**City is an index, not a scan.** Search always starts from a city, so `InMemoryVenueRepository`
keeps a `city -> venueIds` index. This is why `Venue.city` is immutable: a mutable city would
silently stale the index.

**The clock is injected.** Every time-dependent rule ("is this slot in the past?", "has the
cancellation window closed?") reads `LocalDateTime.now(clock)`, so tests state the time outright
instead of sleeping.

### Bonus 1 — concurrent booking

*Two users book the same court at the same instant.* "Check if free" and "take it" must be one
indivisible step; if they are not, both threads see a free slot and both write. `CourtCalendar`
holds a `ReentrantLock` across exactly that pair.

The lock is **per court**, so contention is limited to users fighting over the same court while
other courts proceed fully in parallel. It is the in-memory equivalent of a unique constraint on
`(court_id, slot_start)`. Everything cheap — user lookup, schedule validation, past-slot check —
happens before the lock is taken.

Cancellation has a matching race: two threads cancelling one booking must not free the slot twice.
`Booking.markCancelled` is a synchronized state transition that exactly one caller wins, and only
the winner releases the slot. `CourtCalendar.release` additionally removes by key *and* value, so
a delayed release can never evict whoever booked the slot afterwards.

`ConcurrentBookingTest` verifies all three with 64 threads on a starting-gun latch, repeated 5×.
Removing the lock makes it fail with 32 successful bookings of the same slot.

### Bonus 2 — cancellation policy

`CancellationPolicy` is a per-venue strategy returning a `CancellationDecision` (a value, not an
exception) so a UI can render "free cancellation until 17:00" without catching anything. Both PRD
examples are the same rule with a different duration:

| Venue                            | Policy                                     |
| -------------------------------- | ------------------------------------------ |
| A — free cancellation until 2h before | `NoticePeriodCancellationPolicy.ofHours(2)` |
| B — no cancellation within 12h        | `NoticePeriodCancellationPolicy.ofHours(12)` |
| default                               | `FreeCancellationPolicy.INSTANCE`           |
| never refundable                      | `NonRefundablePolicy.INSTANCE`              |

A new rule (fee-based, tiered, weather exceptions) is a new implementation with no change to
`BookingService`.

## Scope notes

Auth is out of scope per the PRD, but venue mutations still check the caller is the recorded owner
— that is a domain rule about who may edit a venue, not authentication. Deactivating a court hides
it from search and blocks new bookings, and deliberately leaves existing bookings standing rather
than silently cancelling a user's slot.

## Running it

Demo:

```bash
mvn -q compile exec:java -Dexec.mainClass=com.attri.systemdesign.lld.venue_booking.BookingSystemDemo
```

Tests:

```bash
mvn test
```

Both currently require the unrelated `org.example` package to compile, which it does not on
JDK 26 — Lombok 1.18.46 has no JDK 26 support, so `@Data` on `org/example/Pair.java` generates no
getters. Until that is resolved, this package builds and runs standalone:

```bash
javac -d /tmp/booking $(find src/main/java/pop -name '*.java') && java -cp /tmp/booking com.attri.systemdesign.lld.venue_booking.BookingSystemDemo
```
