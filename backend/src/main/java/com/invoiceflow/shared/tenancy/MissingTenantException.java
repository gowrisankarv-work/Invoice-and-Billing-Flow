package com.invoiceflow.shared.tenancy;

/** Thrown when tenant-scoped work is attempted without an authenticated tenant. */
public class MissingTenantException extends RuntimeException {

    public MissingTenantException() {
        super("No tenant is associated with the current request");
    }
}
