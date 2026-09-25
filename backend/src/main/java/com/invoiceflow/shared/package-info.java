/**
 * Cross-cutting building blocks every module may use: money, tenancy, security and API conventions.
 * This module must never depend on a business module.
 */
@ApplicationModule(displayName = "Shared Kernel", type = ApplicationModule.Type.OPEN)
package com.invoiceflow.shared;

import org.springframework.modulith.ApplicationModule;
