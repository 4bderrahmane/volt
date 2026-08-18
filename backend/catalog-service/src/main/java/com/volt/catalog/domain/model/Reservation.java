package com.volt.catalog.domain.model;

import com.volt.catalog.domain.exception.ReservationExpiredException;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A temporary hold on stock (ADR-0003). Aggregate root.
 *
 * <p>The row is a single-use token, and that is what makes confirmation
 * idempotent: once it leaves {@link ReservationStatus#ACTIVE}, a repeated
 * confirm finds it already consumed and decrements nothing.
 *
 * <p>All state transitions are idempotent. Confirmation after expiry is rejected
 * so physical stock can never be decremented for a stale hold.
 */
@Getter
@ToString(of = {"id", "orderRef", "status"})
public final class Reservation {

    private final Long id;

    /** Idempotency key: the order reference that requested this hold. */
    private final String orderRef;

    private ReservationStatus status;
    private final Instant expiresAt;
    private final Instant createdAt;
    private Instant updatedAt;

    /**
     * {@code AccessLevel.NONE} because a generated getter would hand out the
     * live {@code ArrayList}, letting any caller mutate the aggregate's
     * contents behind its back. The accessor below returns an unmodifiable view
     * instead. This is the one place {@code @Getter} on a whole class needs a
     * per-field override, and forgetting it is how encapsulation quietly dies.
     */
    @Getter(AccessLevel.NONE)
    private final List<ReservationLine> lines = new ArrayList<>();

    public Reservation(
            Long id,
            String orderRef,
            ReservationStatus status,
            Instant expiresAt,
            Instant createdAt,
            Instant updatedAt,
            List<ReservationLine> lines) {
        if (id != null && id < 1) {
            throw new IllegalArgumentException("id must be positive");
        }
        this.id = id;
        this.orderRef = requireOrderRef(orderRef);
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("expiresAt must be after createdAt");
        }
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not be before createdAt");
        }
        List<ReservationLine> requiredLines = List.copyOf(lines);
        if (requiredLines.isEmpty()) {
            throw new IllegalArgumentException("reservation must contain at least one line");
        }
        Set<Long> productIds = new HashSet<>();
        for (ReservationLine line : requiredLines) {
            if (!productIds.add(line.getProductId())) {
                throw new IllegalArgumentException("reservation contains duplicate productId " + line.getProductId());
            }
        }
        this.lines.addAll(requiredLines);
    }

    /** A new ACTIVE hold expiring at {@code expiresAt}. */
    public static Reservation open(
            String orderRef, List<ReservationLine> lines, Instant now, Instant expiresAt) {
        return new Reservation(null, orderRef, ReservationStatus.ACTIVE, expiresAt, now, now, lines);
    }

    /** Unmodifiable — lines are fixed once the hold is opened. */
    public List<ReservationLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public boolean isActive() {
        return status == ReservationStatus.ACTIVE;
    }

    public boolean hasExpired(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return !expiresAt.isAfter(now);
    }

    // ----------------------------------------------------------------- behaviour

    /**
     * Moves to CONFIRMED so the caller may decrement physical stock.
     *
     * <p>Three cases, each of which matters:
     * <ul>
     *   <li>already CONFIRMED → return quietly. This is what makes the retry
     *       mandated by specification §3.4 safe rather than a double decrement.</li>
     *   <li>expired, RELEASED or EXPIRED → throw
     *       {@link ReservationExpiredException}. Succeeding here oversells.</li>
     *   <li>ACTIVE and unexpired → confirm.</li>
     * </ul>
     */
    public boolean confirm(Instant now) {
        requireMutationTime(now);
        if (status == ReservationStatus.CONFIRMED) {
            return false;
        }
        if (status != ReservationStatus.ACTIVE || hasExpired(now)) {
            throw new ReservationExpiredException(id);
        }
        status = ReservationStatus.CONFIRMED;
        updatedAt = now;
        return true;
    }

    /** Releases an active hold early. Idempotent. */
    public boolean release(Instant now) {
        requireMutationTime(now);
        if (status != ReservationStatus.ACTIVE) {
            return false;
        }
        status = ReservationStatus.RELEASED;
        updatedAt = now;
        return true;
    }

    /** Marks an active hold expired during the sweep. Idempotent. */
    public boolean expire(Instant now) {
        requireMutationTime(now);
        if (status != ReservationStatus.ACTIVE) {
            return false;
        }
        status = ReservationStatus.EXPIRED;
        updatedAt = now;
        return true;
    }

    /** Returns confirmed stock exactly once. */
    public boolean restock(Instant now) {
        requireMutationTime(now);
        if (status == ReservationStatus.RESTOCKED) {
            return false;
        }
        if (status != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("only a confirmed reservation can be restocked");
        }
        status = ReservationStatus.RESTOCKED;
        updatedAt = now;
        return true;
    }

    private static String requireOrderRef(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("orderRef must not be blank");
        }
        String normalized = value.trim();
        if (normalized.length() > 64) {
            throw new IllegalArgumentException("orderRef must be at most 64 characters");
        }
        return normalized;
    }

    private void requireMutationTime(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        if (now.isBefore(createdAt)) {
            throw new IllegalArgumentException("now must not be before createdAt");
        }
    }

    // ---------------------------------------------------------------- identity

    /**
     * Hand-written on purpose: {@code @EqualsAndHashCode} would generate value
     * equality over every field, which is the same defect a record has. Entity
     * equality is <b>identity</b> — two instances with the same id are the same
     * Reservation, one merely staler than the other.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Reservation that)) {
            return false;
        }
        // No id means no persistent identity yet, so two unsaved instances are
        // equal only if they are the same object.
        return id != null && id.equals(that.id);
    }

    /**
     * Constant, not {@code Objects.hash(id)}: the id is null before persistence
     * and assigned afterwards, so a derived hash would change while the object
     * sits in a HashSet and make it unfindable in the collection holding it.
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
