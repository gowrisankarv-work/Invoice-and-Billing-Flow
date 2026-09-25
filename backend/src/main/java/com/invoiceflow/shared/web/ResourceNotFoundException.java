package com.invoiceflow.shared.web;

/** A requested resource does not exist for the current tenant. Rendered as 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super("%s %s was not found".formatted(resource, id));
    }
}
