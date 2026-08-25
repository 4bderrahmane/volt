/**
 * Exception-to-HTTP translation: the {@code @RestControllerAdvice} that turns
 * domain exceptions into RFC 7807 {@code application/problem+json} bodies.
 *
 * <p>Separate from the controllers because it applies to all of them, and
 * because error mapping is the part of the HTTP contract most likely to be
 * changed by someone who is not touching a controller at the time. Every
 * response carries a stable {@code type} URI, which is what lets a client branch
 * on the failure instead of parsing prose.
 */
package com.volt.catalog.infrastructure.adapter.in.web.advice;
