package com.invoiceflow.shared.web;

import com.invoiceflow.shared.money.CurrencyMismatchException;
import com.invoiceflow.shared.tenancy.MissingTenantException;
import com.invoiceflow.shared.tenancy.TenantAccessViolationException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Renders every API error as an RFC 9457 problem detail ({@code application/problem+json}). Messages
 * for security and unexpected failures are generic so nothing internal leaks to clients.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail unauthenticated(AuthenticationException exception, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, ProblemTypes.UNAUTHENTICATED,
                "A valid bearer token is required", request);
    }

    @ExceptionHandler({AccessDeniedException.class, MissingTenantException.class, TenantAccessViolationException.class})
    ProblemDetail forbidden(RuntimeException exception, HttpServletRequest request) {
        if (exception instanceof TenantAccessViolationException) {
            log.warn("Blocked cross-tenant write on {}", request.getRequestURI());
        }
        return problem(HttpStatus.FORBIDDEN, ProblemTypes.FORBIDDEN,
                "You are not allowed to perform this action", request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail notFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, ProblemTypes.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    ProblemDetail businessRule(BusinessRuleViolationException exception, HttpServletRequest request) {
        ProblemDetail problem = problem(HttpStatus.UNPROCESSABLE_CONTENT, ProblemTypes.BUSINESS_RULE,
                exception.getMessage(), request);
        problem.setProperty("code", exception.code());
        return problem;
    }

    @ExceptionHandler(CurrencyMismatchException.class)
    ProblemDetail currencyMismatch(CurrencyMismatchException exception, HttpServletRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, ProblemTypes.BUSINESS_RULE, exception.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled error on {}", request.getRequestURI(), exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, ProblemTypes.INTERNAL, "An unexpected error occurred", request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = exception.getBody();
        problem.setType(ProblemTypes.VALIDATION);
        problem.setDetail("Request validation failed");
        problem.setProperty("errors", exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .toList());
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    private static ProblemDetail problem(HttpStatus status, URI type, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(type);
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    record FieldError(String field, String message) {
    }
}
