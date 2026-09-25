package com.invoiceflow.shared.tenancy;

/** Thrown when code attempts to write data that belongs to a different tenant. */
public class TenantAccessViolationException extends RuntimeException {

    public TenantAccessViolationException(String message) {
        super(message);
    }
}
