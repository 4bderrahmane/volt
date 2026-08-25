/**
 * Explicit entity/domain mappers.
 *
 * <p>Hand-written rather than generated. The conversions here are the seam that
 * §4 rule 2 depends on, and they are where aggregate rules get enforced on the
 * way in and out — replacing a collection in place so Hibernate sees an update
 * rather than an orphan-delete, rehydrating a domain object through its own
 * factory so its invariants run. A generated mapper would copy fields and skip
 * exactly that.
 */
package com.volt.catalog.infrastructure.adapter.out.persistence.mapper;
