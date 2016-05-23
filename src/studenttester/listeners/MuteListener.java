package studenttester.listeners;
import org.testng.ISuite;
import org.testng.ISuiteListener;

import studenttester.classes.StudentHelperClass;

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
        StudentHelperClass.log(("Starting suite " + suite.getName() + ", muting output"));
        StudentHelperClass.muteStdOut();
    }

    /**
     * Unmute after running suite.
     */
    @Override
    public final void onFinish(final ISuite suite) {
        StudentHelperClass.log(("Finished suite " + suite.getName() + ", unmuting output"));
        StudentHelperClass.restoreStdOut();
    }
}