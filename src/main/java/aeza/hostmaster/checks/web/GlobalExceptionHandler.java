package aeza.hostmaster.checks.web;

import aeza.hostmaster.checks.dto.ApiError;
import aeza.hostmaster.checks.service.InvalidCheckDetailsException;
import aeza.hostmaster.checks.service.SiteCheckNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    // -------------------- 404 NOT FOUND -----------------------------

    @ExceptionHandler(SiteCheckNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(SiteCheckNotFoundException ex, HttpServletRequest req) {
        log.warn("Not found: {}", ex.getMessage());
        return new ApiError(
                Instant.now(),
                404,
                "Not Found",
                ex.getMessage(),
                req.getRequestURI(),
                null
        );
    }


    // -------------------- 400 BAD REQUEST ---------------------------

    @ExceptionHandler({InvalidCheckDetailsException.class, IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleInvalidRequest(RuntimeException ex, HttpServletRequest req) {
        log.warn("Bad request: {}", ex.getMessage());
        return new ApiError(
                Instant.now(),
                400,
                "Bad Request",
                ex.getMessage(),
                req.getRequestURI(),
                null
        );
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {

        Map<String, Object> details = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return new ApiError(
                Instant.now(),
                400,
                "Validation Failed",
                "One or more fields are invalid",
                req.getRequestURI(),
                details
        );
    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        Throwable cause = ex.getMostSpecificCause();
        String msg = cause == null ? ex.getMessage() : cause.getMessage();
        log.warn("JSON parse error: {}", msg);

        return new ApiError(
                Instant.now(),
                400,
                "Invalid JSON",
                msg,
                req.getRequestURI(),
                null
        );
    }


    // -------------------- 500 INTERNAL ERROR -------------------------

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception at {}: {}", req.getRequestURI(), ex.getMessage(), ex);

        return new ApiError(
                Instant.now(),
                500,
                "Internal Server Error",
                ex.getMessage(),
                req.getRequestURI(),
                null
        );
    }
}
