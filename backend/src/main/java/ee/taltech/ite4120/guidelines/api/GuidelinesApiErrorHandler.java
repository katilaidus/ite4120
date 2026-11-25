package ee.taltech.ite4120.guidelines.api;

import ee.taltech.ite4120.guidelines.approverregistry.ApproverRegistryException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * The component's own error handling, for the one failure the shared handler
 * cannot know about: the approver registry being unreachable.
 *
 * <p>Everything else — validation, not-found, conflict — is already answered as
 * {@code application/problem+json} by the platform handler, and this class
 * deliberately does not duplicate it.
 *
 * <p><b>502, not 500.</b> A registry outage is neither the caller's mistake nor
 * a defect in this register: it is a failure of a system upstream of us, and
 * Bad Gateway is the status that says so. Answering 500 would send the
 * coordinator to the wrong team.
 *
 * <p>This is the <b>fail-open</b> policy from GUIDELINES.01 at its narrowest
 * point: the outage is visible on {@code /approver} and nowhere else. The
 * guideline, the search and the review report keep working, because the
 * register computes its main answer — the four-year review deadline — from a
 * date it owns itself. No external system can stop it answering that.
 */
@RestControllerAdvice
public class GuidelinesApiErrorHandler {

    @ExceptionHandler(ApproverRegistryException.class)
    public ProblemDetail approverRegistryUnavailable(ApproverRegistryException e) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
        pd.setTitle("Approver registry unavailable");
        pd.setDetail(e.getMessage());
        return pd;
    }
}
