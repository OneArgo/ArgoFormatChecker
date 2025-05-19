package fr.coriolis.checker.exceptions;

public class ValidateFileDataFailedException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ValidateFileDataFailedException() {
        super();
    }

    public ValidateFileDataFailedException(String message) {
        super(message);
    }

    public ValidateFileDataFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ValidateFileDataFailedException(Throwable cause) {
        super(cause);
    }

}
