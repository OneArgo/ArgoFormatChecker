package fr.coriolis.checker.exceptions;


/**
 * Exception thrown when a file to process is not a valid Argo file.
 */
public class NotAnArgoFileException extends Exception {
	 /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NotAnArgoFileException() {
	        super();
	    }

	    public NotAnArgoFileException(String message) {
	        super(message);
	    }

	    public NotAnArgoFileException(String message, Throwable cause) {
	        super(message, cause);
	    }

	    public NotAnArgoFileException(Throwable cause) {
	        super(cause);
	    }
}
