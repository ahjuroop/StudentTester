package ee.ttu.java.studenttester.listeners;
import static ee.ttu.java.studenttester.classes.StudentLogger.log;

import org.testng.ISuite;
import org.testng.ISuiteListener;

import ee.ttu.java.studenttester.classes.StudentHelperClass;

/**
 * Listener that suppresses the output of student's code.
 * @author Andres
 *
 */
public class MuteListener implements ISuiteListener {

	/**
	 * Mute before running suite.
	 */
	@Override
	public final void onStart(final ISuite suite) {
		log(("Starting suite " + suite.getName() + ", muting output"));
		StudentHelperClass.muteStdOut();
	}

	/**
	 * Unmute after running suite.
	 */
	@Override
	public final void onFinish(final ISuite suite) {
		log(("Finished suite " + suite.getName() + ", unmuting output"));
		StudentHelperClass.stdoutToErr();
	}
}