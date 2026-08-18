package com.volt.order.domain.exception;

/** Raised when a cart mutation targets a line outside the current cart. */
public class CartLineNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public CartLineNotFoundException(long cartLineId) {
        super("No cart line with id " + cartLineId);
    }
}
