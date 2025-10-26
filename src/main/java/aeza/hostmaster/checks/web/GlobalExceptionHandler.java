package aeza.hostmaster.checks.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Map<String,String> handle(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getMostSpecificCause();
        String msg = cause == null ? ex.getMessage() : cause.getMessage();
        return Map.of("error", "Bad Request", "reason", msg);
    }
}

