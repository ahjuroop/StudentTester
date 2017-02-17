package test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.TestNG;


public class MiniStudentTester {
	
	private Class<?> classToTest;
	private String name;
	private List<String> passedTests, failedTests, skippedTests;
	
	/**
	 * Creates a new Instance of MiniStudentTester.
	 * @param name - test name
	 * @param classToTest - TestNG/JUnit class to be tested
	 */
	public MiniStudentTester(String name, Class<?> classToTest) {
		this.name = name;
		this.classToTest = classToTest;
	}

	/**
	 * Runs TestNG with the current configuration.
	 * @return whether the unit tests were successful
	 */
	@SuppressWarnings("deprecation")
	public boolean run() {
		TestNG metaTestNG = new TestNG();
		metaTestNG.setUseDefaultListeners(false);						// disable irrelevant listeners
		metaTestNG.setDefaultSuiteName(name);
		metaTestNG.addListener(new MetaListener());						// attach custom listener (see below)
		metaTestNG.setTestClasses(new Class[] {classToTest});
		for (Method unitTest : classToTest.getMethods()) {				// if the class contains at least one
			if (unitTest.isAnnotationPresent(org.junit.Test.class)) {	// JUnit method, assume it's a JUnit test
				metaTestNG.setJUnit(true);
				break;
			}
		};
		metaTestNG.run();
		return !metaTestNG.hasFailure();
	}

	/**
	 * Returns the list of passed unit test names.
	 * @return passed unit test names.
	 */
	public List<String> getPassedTests() {
		return passedTests;
	}

	/**
	 * Returns the list of failed unit test names.
	 * @return failed unit test names.
	 */
	public List<String> getFailedTests() {
		return failedTests;
	}

	/**
	 * Returns the list of skipped unit test names.
	 * @return skipped unit test names.
	 */
	public List<String> getSkippedTests() {
		return skippedTests;
	}

	/**
	 * TestNG listener that collects information about finished unit tests.
	 * @author Andres
	 */
	public class MetaListener implements ISuiteListener {

		/**
		 * Method that is run before all tests.
		 */
		@Override
		public void onStart(ISuite suite) {
			System.err.println("Running " + suite.getName());
		}

		/**
		 * Method that is run after all tests.
		 */
		@Override
		public void onFinish(ISuite suite) {
			System.err.println("Finished " + suite.getName());
			passedTests = new ArrayList<String>();
			failedTests = new ArrayList<String>();
			skippedTests = new ArrayList<String>();
			for (String name : suite.getResults().keySet()) {
				ITestContext context = suite.getResults().get(name).getTestContext();
				context.getPassedTests().getAllMethods().forEach((x) -> passedTests.add(x.getMethodName()));
				context.getFailedTests().getAllMethods().forEach((x) -> failedTests.add(x.getMethodName()));
				context.getSkippedTests().getAllMethods().forEach((x) -> skippedTests.add(x.getMethodName()));
			}
			System.err.format("Passed: %d, failed: %d, skipped: %d\n",
					passedTests.size(), failedTests.size(), skippedTests.size());
		}
	}
}
