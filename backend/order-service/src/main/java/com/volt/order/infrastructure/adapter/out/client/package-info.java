/**
 * CatalogRestClientAdapter. Translates HTTP failures into domain exceptions:
 * no RestClientResponseException may escape this package, or the application
 * layer starts knowing about HTTP.
 */
package com.volt.order.infrastructure.adapter.out.client;
