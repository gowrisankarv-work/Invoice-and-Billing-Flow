package com.invoiceflow.shared.security;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Test-only endpoints guarded by roles, used to check the role hierarchy through the real filter chain. */
@RestController
class RoleProbeController {

    @GetMapping("/test-probes/accounting")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    String accounting() {
        return "ok";
    }

    @GetMapping("/test-probes/read")
    @PreAuthorize("hasRole('VIEWER')")
    String read() {
        return "ok";
    }
}
