/**
 * Business failures, extending RuntimeException so ports stay free of checked-
 * exception noise. Each maps to exactly one HTTP status in the web adapter's
 * RestControllerAdvice. If a failure has no natural HTTP meaning it is
 * probably a technical error, not a domain one.
 */
package com.volt.catalog.domain.exception;
