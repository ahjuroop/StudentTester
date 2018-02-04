package ee.ttu.java.studenttester.interfaces;
import ee.ttu.java.studenttester.exceptions.StudentTesterException;
import ee.ttu.java.studenttester.dataclasses.TestResults;

/**
 * Interface for TestNG reporters communicating with the tester.
 * The reporter implementing this interface should also implement
 * IReporter interface.
 * @author Andres
 *
 */
public interface IBaseStudentReporter {
	/**
	 * Public method for getting data from the reporter.
	 * @return test results in TestResults format.
	 * @throws StudentTesterException if an error has occurred or the reported has not been run.
	 */
	TestResults getResults() throws StudentTesterException;

}
