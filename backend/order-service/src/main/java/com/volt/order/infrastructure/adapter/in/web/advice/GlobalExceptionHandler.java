package com.volt.order.infrastructure.adapter.in.web.advice;

import com.volt.order.domain.exception.CartLineNotFoundException;
import com.volt.order.domain.exception.CatalogUnavailableException;
import com.volt.order.domain.exception.EmptyCartException;
import com.volt.order.domain.exception.IllegalStatusTransitionException;
import com.volt.order.domain.exception.InsufficientStockException;
import com.volt.order.domain.exception.OrderNotFoundException;
import com.volt.order.domain.exception.ProductUnavailableException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({OrderNotFoundException.class, CartLineNotFoundException.class})
    ProblemDetail notFound(RuntimeException exception) {
        return problem(HttpStatus.NOT_FOUND, "Resource not found", "not-found", exception.getMessage());
    }

    @ExceptionHandler(EmptyCartException.class)
    ProblemDetail emptyCart(EmptyCartException exception) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Empty cart", "empty-cart", exception.getMessage());
    }

    @ExceptionHandler(ProductUnavailableException.class)
    ProblemDetail productUnavailable(ProductUnavailableException exception) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Product unavailable", "product-unavailable", exception.getMessage());
    }

    @ExceptionHandler(InsufficientStockException.class)
    ProblemDetail insufficientStock(InsufficientStockException exception) {
        ProblemDetail problem = problem(HttpStatus.CONFLICT, "Insufficient stock", "insufficient-stock", exception.getMessage());
        problem.setProperty("shortages", exception.shortages());
        return problem;
    }

    @ExceptionHandler(IllegalStatusTransitionException.class)
    ProblemDetail illegalTransition(IllegalStatusTransitionException exception) {
        return problem(HttpStatus.CONFLICT, "Illegal order status transition", "illegal-status-transition", exception.getMessage());
    }

    @ExceptionHandler(CatalogUnavailableException.class)
    ProblemDetail catalogUnavailable(CatalogUnavailableException exception) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Catalog unavailable", "catalog-unavailable", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalidRequest(MethodArgumentNotValidException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Request validation failed", "validation-error",
                "One or more request fields are invalid");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalidValue(IllegalArgumentException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid value", "invalid-value", exception.getMessage());
    }

    /**
     * Constraints on path variables — the {@code @Positive} ids — are checked by
     * the {@code @Validated} proxy, which raises this rather than
     * {@link MethodArgumentNotValidException}. Without a handler it escapes as a
     * 500, so {@code GET /api/v1/orders/0} reported a server fault for what is
     * plainly a bad request.
     *
     * <p>The violation message is not echoed back: it is phrased in terms of the
     * Java method and parameter that failed, which tells a caller nothing useful
     * and describes internals.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail invalidConstraint(ConstraintViolationException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid value", "invalid-value",
                "One or more request parameters are invalid");
    }

    private static ProblemDetail problem(HttpStatus status, String title, String type, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://volt.local/problems/" + type));
        return problem;
    }
}
