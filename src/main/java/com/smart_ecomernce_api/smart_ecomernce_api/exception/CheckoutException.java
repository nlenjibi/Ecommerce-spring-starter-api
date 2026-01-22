package com.smart_ecomernce_api.smart_ecomernce_api.exception;

public class CheckoutException extends RuntimeException {

    public CheckoutException() {
        super("Checkout processing error");
    }

    public CheckoutException(String message) {
        super(message);
    }

    public CheckoutException(String message, Throwable cause) {
        super(message, cause);
    }
}