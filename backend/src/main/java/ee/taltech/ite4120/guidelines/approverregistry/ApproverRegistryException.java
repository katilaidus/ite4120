package ee.taltech.ite4120.guidelines.approverregistry;

/** The registry could not be reached, or answered with a fault. */
public class ApproverRegistryException extends RuntimeException {

    public ApproverRegistryException(String message) {
        super(message);
    }

    public ApproverRegistryException(String message, Throwable cause) {
        super(message, cause);
    }
}
