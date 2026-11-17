package dev.logplatform;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@RestControllerAdvice
public class Problems {
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalid(Exception e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid bounded request");
    }

    @ExceptionHandler({TimeoutException.class, ExecutionException.class})
    ProblemDetail unavailable(Exception e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Kafka acceptance uncertain; retry with the same event IDs");
    }
}
