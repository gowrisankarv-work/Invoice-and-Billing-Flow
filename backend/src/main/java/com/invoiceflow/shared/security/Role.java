package com.invoiceflow.shared.security;

import java.util.Arrays;
import java.util.Optional;

/**
 * Organization roles. Carried in the JWT {@value JwtClaims#ROLES} claim and mapped to
 * {@code ROLE_<name>} authorities. See {@link SecurityConfiguration#roleHierarchy()} for which roles
 * imply others.
 */
public enum Role {
    /** Full organization control. */
    OWNER,
    /** Administrative management. */
    ADMIN,
    /** Billing, payments and financial operations. */
    ACCOUNTANT,
    /** Customers, quotations and orders. */
    SALES,
    /** Permitted operational tasks. */
    STAFF,
    /** Read-only access. */
    VIEWER;

    public String authority() {
        return "ROLE_" + name();
    }

    static Optional<Role> fromClaim(String value) {
        return Arrays.stream(values()).filter(role -> role.name().equalsIgnoreCase(value)).findFirst();
    }
}
