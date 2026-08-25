/**
 * Spring Data repositories ({@code *JpaRepository}).
 *
 * <p>These are an implementation detail of the persistence adapter, not a port.
 * Nothing outside {@code adapter.out.persistence} may reference them: the
 * application layer depends on the {@code *RepositoryPort} interface it owns, so
 * that no {@code Page}, {@code Example} or {@code Specification} ever reaches a
 * use case. {@code HexagonalArchitectureTest} keeps the name and the location
 * pinned together.
 *
 * <p>Pessimistic locking lives here too, as {@code @Lock(PESSIMISTIC_WRITE)}
 * query methods. Their callers must already hold a transaction, or the lock is
 * released the instant the method returns — which is why the adapters that use
 * them declare {@code Propagation.MANDATORY}.
 */
package com.volt.catalog.infrastructure.adapter.out.persistence.repository;
