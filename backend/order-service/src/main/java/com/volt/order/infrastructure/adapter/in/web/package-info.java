/**
 * The HTTP input adapter, split by role rather than piled into one directory.
 *
 * <ul>
 *   <li>{@code controller} — Spring MVC controllers, and only controllers</li>
 *   <li>{@code dto.request} — inbound payloads, where bean validation lives</li>
 *   <li>{@code dto.response} — the published output contract</li>
 *   <li>{@code advice} — exception-to-RFC 7807 translation</li>
 * </ul>
 *
 * <p>Classes here may know about Spring MVC and JSON, but they call the
 * application only through incoming port interfaces, and domain models stay
 * separate from DTOs so an API change does not rewrite business code.
 *
 * <p>The split is asserted by {@code HexagonalArchitectureTest}, not merely
 * agreed. A flat web package is perfectly readable at six files and stops being
 * readable somewhere around sixteen, and the drift back is invisible one file at
 * a time — which is exactly the kind of decay a test can prevent and a
 * convention cannot.
 */
package com.volt.order.infrastructure.adapter.in.web;
