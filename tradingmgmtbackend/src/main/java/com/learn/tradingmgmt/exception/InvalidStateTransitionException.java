package com.learn.tradingmgmt.exception;

/**
 * Thrown whenever a business command is attempted from a status that does
 * not permit it (e.g. verifying a payment that is still PAYMENT_PENDING).
 * Deliberately distinct from validation errors — this is a workflow-rule
 * violation, not a malformed request.
 */
public class InvalidStateTransitionException extends RuntimeException {
    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
