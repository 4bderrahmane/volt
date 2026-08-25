/**
 * Driven ports: what the application needs from the world, expressed as a need
 * rather than as a technology. CatalogClientPort, not CatalogRestClient. If a
 * port name mentions HTTP, JPA or SQL, the abstraction has leaked and the
 * infrastructure has reached inward.
 */
package com.volt.catalog.application.port.out;
