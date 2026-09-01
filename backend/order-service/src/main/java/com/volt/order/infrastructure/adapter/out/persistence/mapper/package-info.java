/**
 * Explicit entity/domain mappers.
 *
 * <p>Hand-written rather than generated. These conversions keep persistence
 * entities separate from domain models and enforce aggregate rules on the way
 * in and out — replacing a collection in place so Hibernate sees an update
 * rather than an orphan-delete, and rehydrating a domain object through its own
 * factory so its invariants run. A generated mapper would copy fields and skip
 * exactly that.
 */
package com.volt.order.infrastructure.adapter.out.persistence.mapper;
