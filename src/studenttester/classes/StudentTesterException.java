package studenttester.classes;

/**
 * Dummy exception for StudentTester.
 * @author Andres
 *
 */
public class StudentTesterException extends Exception {

	private static final long serialVersionUID = 5759593770601805150L;

	public StudentTesterException(String message) {
		super(message);
	}

	public StudentTesterException(String message, Throwable throwable) {
		super(message, throwable);
	}

}
