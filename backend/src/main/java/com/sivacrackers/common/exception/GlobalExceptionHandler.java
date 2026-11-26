package com.sivacrackers.common.exception;

import com.sivacrackers.common.dto.ApiError;
import com.sivacrackers.common.dto.FieldErrorDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Turns every exception thrown by a controller into the standard {@link ApiError} JSON.
 *
 * <p>SECURITY: client messages are generic for unexpected errors (no stack traces,
 * SQL or class names), rejected input values are never echoed back, and request
 * bodies are never logged (they may contain passwords).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> handleApi(ApiException ex, HttpServletRequest request) {
        List<FieldErrorDto> fieldErrors = ex.getFieldErrors();
        ResponseEntity<ApiError> response = respond(ex.getStatus(), ex.getMessage(), request, fieldErrors);
        if (ex instanceof TooManyRequestsException tooMany) {
            return ResponseEntity.status(response.getStatusCode())
                    .header("Retry-After", String.valueOf(tooMany.getRetryAfterSeconds()))
                    .body(response.getBody());
        }
        return response;
    }

    /** @Valid request bodies. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleBodyValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<FieldErrorDto> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorDto(fe.getField(), message(fe)))
                .sorted(Comparator.comparing(FieldErrorDto::field))
                .toList();
        return respond(HttpStatus.BAD_REQUEST, "Validation failed", request, errors);
    }

    /** Constraints on @RequestParam / @PathVariable (Spring method validation). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiError> handleParamValidation(HandlerMethodValidationException ex, HttpServletRequest request) {
        List<FieldErrorDto> errors = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(err -> new FieldErrorDto(result.getMethodParameter().getParameterName(), message(err))))
                .sorted(Comparator.comparing(FieldErrorDto::field))
                .toList();
        return respond(HttpStatus.BAD_REQUEST, "Validation failed", request, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex, HttpServletRequest request) {
        List<FieldErrorDto> errors = ex.getConstraintViolations().stream()
                .map(v -> new FieldErrorDto(lastNode(v.getPropertyPath().toString()), v.getMessage()))
                .sorted(Comparator.comparing(FieldErrorDto::field))
                .toList();
        return respond(HttpStatus.BAD_REQUEST, "Validation failed", request, errors);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "Validation failed", request,
                List.of(new FieldErrorDto(ex.getParameterName(), "is required")));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "Validation failed", request,
                List.of(new FieldErrorDto(ex.getName(), "has an invalid value")));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "Malformed request body", request, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "Resource not found", request, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> handleMethod(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return respond(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed", request, List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiError> handleMediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return respond(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported content type", request, List.of());
    }

    /** Uploads over the size limit (spring.servlet.multipart.max-file-size). */
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> handleTooLarge(Exception ex, HttpServletRequest request) {
        return respond(HttpStatus.CONTENT_TOO_LARGE, "The file is too large. Images can be at most 2 MB.", request, List.of());
    }

    /** Broken or missing multipart upload. */
    @ExceptionHandler({org.springframework.web.multipart.MultipartException.class,
            org.springframework.web.multipart.support.MissingServletRequestPartException.class})
    ResponseEntity<ApiError> handleMultipart(Exception ex, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "Please choose an image file to upload.", request, List.of());
    }

    /**
     * Method-level role checks ({@code @PreAuthorize}) fail inside the controller call, so they arrive here
     * rather than at the security filter chain. Same 403 and message as the URL-level role checks.
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    ResponseEntity<ApiError> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex,
                                                HttpServletRequest request) {
        return respond(HttpStatus.FORBIDDEN, "You do not have permission to do this.", request, List.of());
    }

    /** Anything unexpected: log the stack trace on the server, return a generic message. */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again later.", request, List.of());
    }

    private static ResponseEntity<ApiError> respond(HttpStatus status, String message, HttpServletRequest request,
                                                    List<FieldErrorDto> fieldErrors) {
        ApiError body = ApiError.of(status.value(), status.getReasonPhrase(), message, request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(status).body(body);
    }

    private static String message(MessageSourceResolvable error) {
        return error.getDefaultMessage() != null ? error.getDefaultMessage() : "is invalid";
    }

    private static String message(FieldError error) {
        return error.getDefaultMessage() != null ? error.getDefaultMessage() : "is invalid";
    }

    private static String lastNode(String path) {
        int dot = path.lastIndexOf('.');
        return dot >= 0 ? path.substring(dot + 1) : path;
    }
}
