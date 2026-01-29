package encube.assignment.modules.files.domain.exception;

public class VersioningException extends RuntimeException {

    public VersioningException(String message) {
        super(message);
    }

    public VersioningException(String message, Throwable cause) {
        super(message, cause);
    }
}
