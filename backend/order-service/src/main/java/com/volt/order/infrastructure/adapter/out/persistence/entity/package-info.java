/**
 * JPA entities ({@code *JpaEntity}) — the table shape, not the domain model.
 *
 * <p>These remain distinct from {@code domain.model}: an entity answers "how is
 * this stored", while a domain model answers "what does this mean". Collapsing
 * them makes every JPA constraint a business rule by accident and every schema
 * change a domain change.
 *
 * <p>Lombok is unrestricted here because this is boilerplate, not behaviour.
 * Two rules that are not optional: never {@code @Data} and never a bare
 * {@code @ToString} on an entity, because both generate code that touches lazy
 * associations and turns a log statement into a database round trip, or a
 * {@code LazyInitializationException} once the session has closed. And
 * {@code @ManyToOne} is always {@code FetchType.LAZY}; the JPA default is EAGER,
 * which is how a product listing becomes 3N queries.
 */
package com.volt.order.infrastructure.adapter.out.persistence.entity;
