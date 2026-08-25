/**
 * The persistence output adapter, split by role.
 *
 * <ul>
 *   <li>{@code entity} — JPA entities ({@code *JpaEntity}), the table shape</li>
 *   <li>{@code repository} — Spring Data repositories ({@code *JpaRepository})</li>
 *   <li>{@code mapper} — explicit entity/domain translation</li>
 *   <li>this package — the {@code *PersistenceAdapter} classes implementing the
 *       out-ports, which are the only part the application layer can see</li>
 * </ul>
 *
 * <p><b>This package tree is the only place permitted to import
 * {@code jakarta.persistence}</b>, and {@code HexagonalArchitectureTest}
 * enforces that. The confinement is what keeps specification §4 rule 2 real: the
 * moment an entity can be annotated anywhere, the domain model and the table
 * schema quietly become one object and every JPA constraint becomes a business
 * rule by accident.
 *
 * <p>The adapters stay at this level deliberately. They are the public face of
 * the package — everything in the sub-packages exists to serve them — and
 * burying them one level deeper alongside their own collaborators would make the
 * entry point the hardest thing to find.
 *
 * <p>Lombok is used freely throughout, because this is boilerplate rather than
 * behaviour; the {@code domain/lombok.config} restrictions do not apply here
 * (ADR-0008). The entity-specific hazards are documented in
 * {@code entity/package-info.java}.
 */
package com.volt.catalog.infrastructure.adapter.out.persistence;
