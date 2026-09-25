package com.invoiceflow.shared.web;

/**
 * A request was well-formed but breaks a business rule (for example, paying a voided invoice).
 * Rendered as 422 with the machine-readable {@code code} as a problem-detail property.
 */
public class BusinessRuleViolationException extends RuntimeException {

    private final String code;

    public BusinessRuleViolationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
