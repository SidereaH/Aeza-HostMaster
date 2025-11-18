package aeza.hostmaster.checks.web;

import aeza.hostmaster.checks.service.CheckJobNotFoundException;
import aeza.hostmaster.checks.service.SiteCheckSchedulingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Map<String, String> handle(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getMostSpecificCause();
        String msg = cause == null ? ex.getMessage() : cause.getMessage();
        return Map.of("error", "Bad Request", "reason", msg);
    }

    @ExceptionHandler(SiteCheckSchedulingException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @ResponseBody
    public Map<String, String> handleSiteCheckScheduling(SiteCheckSchedulingException ex) {
        String reason = ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage();
        return Map.of("error", "Service Unavailable", "reason", reason);
    }

    @ExceptionHandler(CheckJobNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public Map<String, String> handleJobNotFound(CheckJobNotFoundException ex) {
        return Map.of("error", "Not Found", "reason", ex.getMessage());
    }
}

