package com.invoiceflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.modulith.Modulithic;

@SpringBootApplication
@ConfigurationPropertiesScan
@Modulithic(systemName = "InvoiceFlow", sharedModules = "shared")
public class InvoiceFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(InvoiceFlowApplication.class, args);
    }
}
