package com.invoiceflow.shared.web;

import java.net.URI;

/** Stable {@code type} URIs for problem-detail responses, so clients can branch on them. */
public final class ProblemTypes {

    public static final URI UNAUTHENTICATED = of("unauthenticated");
    public static final URI FORBIDDEN = of("forbidden");
    public static final URI NOT_FOUND = of("not-found");
    public static final URI VALIDATION = of("validation");
    public static final URI BUSINESS_RULE = of("business-rule");
    public static final URI INTERNAL = of("internal");

    private ProblemTypes() {
    }

    private static URI of(String slug) {
        return URI.create("urn:invoiceflow:problem:" + slug);
    }
}
