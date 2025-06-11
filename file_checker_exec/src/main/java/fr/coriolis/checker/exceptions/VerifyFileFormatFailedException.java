package fr.coriolis.checker.exceptions;

/**
 * Exception raised when there is an error during the verify format process
 */

public class VerifyFileFormatFailedException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public VerifyFileFormatFailedException() {
        super();
    }

    public VerifyFileFormatFailedException(String message) {
        super(message);
    }

    public VerifyFileFormatFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public VerifyFileFormatFailedException(Throwable cause) {
        super(cause);
    }

}
