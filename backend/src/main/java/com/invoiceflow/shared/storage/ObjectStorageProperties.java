package com.invoiceflow.shared.storage;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** S3-compatible object storage (MinIO) used for generated invoice and receipt documents. */
@Validated
@ConfigurationProperties("invoiceflow.storage")
public record ObjectStorageProperties(
        @NotBlank String endpoint, @NotBlank String accessKey, @NotBlank String secretKey, @NotBlank String bucket) {
}
