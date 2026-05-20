package nl.openmrs.comm_module.testgui;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Duidelijke 4xx bij test-GUI i.p.v. generieke 500. */
@RestControllerAdvice(basePackageClasses = SchedulingTestController.class)
@ConditionalOnProperty(name = "comm.test-gui.enabled", havingValue = "true")
public class TestGuiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail badRequest(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler({IllegalStateException.class, DataAccessException.class})
    public ProblemDetail conflict(Exception ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, cause.getMessage());
    }
}
