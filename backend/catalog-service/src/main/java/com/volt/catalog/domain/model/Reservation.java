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

@Getter
@ToString(of = {"id", "orderRef", "status"})
public final class Reservation {

    private final Long id;

    private final String orderRef;

    private ReservationStatus status;
    private final Instant expiresAt;
    private final Instant createdAt;
    private Instant updatedAt;

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

    public static Reservation open(
            String orderRef, List<ReservationLine> lines, Instant now, Instant expiresAt) {
        return new Reservation(null, orderRef, ReservationStatus.ACTIVE, expiresAt, now, now, lines);
    }

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

    public boolean release(Instant now) {
        requireMutationTime(now);
        if (status != ReservationStatus.ACTIVE) {
            return false;
        }
        status = ReservationStatus.RELEASED;
        updatedAt = now;
        return true;
    }

    public boolean expire(Instant now) {
        requireMutationTime(now);
        if (status != ReservationStatus.ACTIVE) {
            return false;
        }
        status = ReservationStatus.EXPIRED;
        updatedAt = now;
        return true;
    }

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

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Reservation that)) {
            return false;
        }

        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
