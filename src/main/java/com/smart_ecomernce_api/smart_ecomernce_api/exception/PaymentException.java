package com.smart_ecomernce_api.smart_ecomernce_api.exception;

public class PaymentException extends RuntimeException {

    public PaymentException() {
        super("Payment processing error");
    }

    public PaymentException(String message) {
        super(message);
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
