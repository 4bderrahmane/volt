/**
 * Outbound response DTOs, one per representation the API returns.
 *
 * <p>Records, for the reasons set out in {@code dto.request}: a response is a
 * value, and immutability means a serialised body cannot be altered between
 * construction and the wire.
 *
 * <p>Kept separate from the domain model on purpose. Serialising an entity or a
 * domain object directly makes every field rename a breaking API change and
 * quietly leaks internals — stock levels, version columns, whatever gets added
 * next — to whoever is listening. A static {@code from(...)} factory does the
 * mapping, so the translation is one obvious place rather than scattered
 * through controllers.
 */
package com.volt.order.infrastructure.adapter.in.web.dto.response;
