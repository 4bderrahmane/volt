package com.volt.catalog.infrastructure.adapter.in.web.advice;

import com.volt.catalog.domain.exception.DuplicateProductReferenceException;
import com.volt.catalog.domain.exception.InsufficientStockException;
import com.volt.catalog.domain.exception.ProductNotFoundException;
import com.volt.catalog.domain.exception.ReservationExpiredException;
import com.volt.catalog.domain.exception.ReservationNotFoundException;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    ProblemDetail productNotFound(ProductNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Product not found", "product-not-found", exception.getMessage());
    }

    @ExceptionHandler(DuplicateProductReferenceException.class)
    ProblemDetail duplicateReference(DuplicateProductReferenceException exception) {
        return problem(
                HttpStatus.CONFLICT,
                "Duplicate product reference",
                "duplicate-product-reference",
                exception.getMessage());
    }

    @ExceptionHandler(InsufficientStockException.class)
    ProblemDetail insufficientStock(InsufficientStockException exception) {
        ProblemDetail problem = problem(
                HttpStatus.CONFLICT,
                "Insufficient stock",
                "insufficient-stock",
                exception.getMessage());
        problem.setProperty("shortages", exception.shortages());
        return problem;
    }

    @ExceptionHandler(ReservationNotFoundException.class)
    ProblemDetail reservationNotFound(ReservationNotFoundException exception) {
        return problem(
                HttpStatus.NOT_FOUND,
                "Reservation not found",
                "reservation-not-found",
                exception.getMessage());
    }

    @ExceptionHandler(ReservationExpiredException.class)
    ProblemDetail reservationExpired(ReservationExpiredException exception) {
        return problem(
                HttpStatus.GONE,
                "Reservation expired",
                "reservation-expired",
                exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalidRequest(MethodArgumentNotValidException exception) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                "validation-error",
                "One or more request fields are invalid");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalidDomainValue(IllegalArgumentException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid value", "invalid-value", exception.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail invalidConstraint(ConstraintViolationException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid value", "invalid-value", exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail dataConflict(DataIntegrityViolationException exception) {
        return problem(
                HttpStatus.CONFLICT,
                "Data conflict",
                "data-conflict",
                "The request conflicts with existing catalog data");
    }

    private static ProblemDetail problem(HttpStatus status, String title, String type, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://volt.local/problems/" + type));
        return problem;
    }
}
