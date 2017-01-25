package studenttester.interfaces;
import studenttester.dataclasses.TestResults;

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
	 */
	TestResults getResults();

}
