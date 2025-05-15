package transport.core;

public class ReductionImpossibleException extends Exception {

    public ReductionImpossibleException() {
        super();
    }

    public ReductionImpossibleException(String message) {
        super(message);
    }

    public ReductionImpossibleException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReductionImpossibleException(Throwable cause) {
        super(cause);
    }
}
