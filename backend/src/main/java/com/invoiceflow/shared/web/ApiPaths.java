package com.invoiceflow.shared.web;

/** URL conventions for the public HTTP API. */
public final class ApiPaths {

    /**
     * Prefix added to every {@code @RestController} mapping. Controllers declare paths relative to it,
     * e.g. {@code @GetMapping("/customers")} is served at {@code /api/v1/customers}.
     */
    public static final String V1 = "/api/v1";

    private ApiPaths() {
    }
}
