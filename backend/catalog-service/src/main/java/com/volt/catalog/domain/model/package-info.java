/**
 * Aggregates, value objects and enums.
 *
 * <p><b>Entities are classes; value objects are records.</b> The split is not
 * stylistic, and it is worth being able to defend it:
 *
 * <p>An <i>entity</i> has an identity that outlives its field values. {@link Product}, {@link Category}, {@link Brand},
 * {@link Reservation} and {@link ReservationLine}
 * are entities: they have an id, a lifecycle, invariants to protect across
 * mutations, and they remain the same thing after a change. Records get all
 * three of those wrong. Their generated {@code equals} compares every
 * component, so two snapshots of one row taken either side of an edit compare
 * as different objects and a stale copy cannot be found in a {@code HashSet}
 * that contains the fresh one. Their immutability turns every state change into
 * a new instance the caller must remember to reassign, leaving the old one
 * reachable and still claiming the old state. And every component is publicly
 * readable, so an aggregate cannot protect its own collection.
 *
 * <p>Entities here therefore define {@code equals} and {@code hashCode} by id
 * alone, with a constant hash — the id is null before persistence and assigned
 * afterwards, so a hash derived from it would change while the object sits in a
 * set and make the object unfindable in the very collection holding it.
 *
 * <p>A <i>value object</i> has no identity beyond its contents: two instances
 * with equal fields are interchangeable. {@link PagedResult}, {@link ProductSearchCriteria} and
 * {@link StockShortage} are value objects, and for
 * those a record is exactly right — component-wise equality is the correct
 * semantic, immutability is a feature, and writing them as classes would be
 * boilerplate with no benefit. The same applies to the command and result
 * records nested inside the port interfaces: they are messages, not things.
 *
 * <p>Rule of thumb: if it has an {@code id} column, it is a class.
 *
 * <p>This convention is documented rather than enforced by ArchUnit — see
 * ADR-0001 for why, and for the trade-off it costs.
 *
 * <p><b>Lombok policy.</b> {@code @Getter} and {@code @ToString} are used here —
 * accessors are boilerplate and nothing is lost by generating them. {@code @Data},
 * {@code @Value}, {@code @Setter}, {@code @EqualsAndHashCode},
 * {@code @AllArgsConstructor} and {@code @Builder} are rejected at compile time
 * by {@code ../lombok.config}: each of them would undo something on this page.
 * {@code @Data} alone reintroduces both value equality and public setters, which
 * is the record problem plus an encapsulation hole. See ADR-0008.
 *
 * <p>Note {@code @Getter(AccessLevel.NONE)} on the aggregate collections: a
 * generated accessor would hand out the live {@code ArrayList} and let any
 * caller edit the aggregate behind its back.
 */
package com.volt.catalog.domain.model;
