package com.invoiceflow.shared.tenancy;

import java.util.Objects;
import java.util.regex.Pattern;

/** Identifier of a tenant (an organization). Only ever derived from the authenticated principal. */
public record TenantId(String value) {

    private static final Pattern FORMAT = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    public TenantId {
        Objects.requireNonNull(value, "value");
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid tenant id");
        }
    }

    public static TenantId of(String value) {
        return new TenantId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
