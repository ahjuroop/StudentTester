package studenttester.listeners;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.IReporter;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;

import studenttester.annotations.TestContextConfiguration;
import studenttester.annotations.Gradable;
import studenttester.classes.StudentHelperClass;
import studenttester.dataclasses.TestResults;
import studenttester.enums.ReportMode;
import studenttester.interfaces.IBaseStudentReporter;

/**
 * Custom reporter class.
 * @author Andres
 *
 */
public final class StudentReporter implements IReporter, IBaseStudentReporter {

	/**
	 * Temporary data class for test results.
	 */
	private TestResults results;
	/**
	 * Stores assertion errors etc for including in diagnostic test results.
	 */
	private List<String> failureReasons;
	/**
	 * Verbosity settings for test class.
	 */
	private ReportMode reportMode;

	/**
	 * Returns results to tester class.
	 * @return test results
	 */
	public TestResults getResults() {
		return results;
	}

	/**
	 * Defines unit test outcomes.
	 */
	private static final int 	SUCCESS = 0,
								FAILURE = 1,
								SKIP = 2;

	@Override
	public void generateReport(final List<XmlSuite> xmlSuites, final List<ISuite> suites, final String outputDirectory) {
		results = new TestResults(); // prepare new test results object
		int index = 1; // test counter for json
		double overallTotal = 0; // overall total score
		double overallPassed = 0; // overall passed score
		String output = ""; // test output string

		for (ISuite suite : suites) {

			output += String.format("Test suite \"%s\"\n", suite.getName());
			Map<String, ISuiteResult> suiteResults = suite.getResults();

			for (ISuiteResult sr : suiteResults.values()) {

				ITestContext tc = sr.getTestContext();
				TestContextConfiguration conf = getClassMetadata(tc);

				// get info from class annotation
				if (conf != null && conf.mode() != null) {
					reportMode = conf.mode();
				} else {
					reportMode = ReportMode.NORMAL;
				}

				output += "\n ---";
				output += String.format("\n%s\n", tc.getName());
				if (conf != null && !conf.welcomeMessage().isEmpty()) {
					output += String.format("%s\n", conf.welcomeMessage());
				}
				output += " ---\n";

				double total = 0;
				double passed = 0;
				failureReasons = new ArrayList<String>(); // clear or initialize diagnostic array
				Set<ITestResult> testsFromContext;

				// iterate over three result types
				for (int type = 0; type < 3; type++) {
					switch (type) {
					// reduce code duplication
					case SUCCESS:
						testsFromContext = tc.getPassedTests().getAllResults();
						break;
					case FAILURE:
						testsFromContext = tc.getFailedTests().getAllResults();
						break;
					case SKIP:
						testsFromContext = tc.getSkippedTests().getAllResults();
						break;
					default:
						StudentHelperClass.log("This should never happen.");
						return;
					}
					for (ITestResult unitTestResult : testsFromContext) {
						Gradable testMetadata = getTestMetadata(unitTestResult);
						if (testMetadata != null) {
							output += (getTestReportString(unitTestResult, testMetadata));
							if (type == SUCCESS) {
								passed += testMetadata.weight();
							}
							total += testMetadata.weight();
						} else {
							output += (getTestReportString(unitTestResult, getMockAnnotation()));
							if (type == SUCCESS) {
								passed += getMockAnnotation().weight();
							}
							total += getMockAnnotation().weight();
						}
						if (type == FAILURE || type == SKIP) {
							failureReasons.add(String.format("FAILURE: %s (%s)",
									unitTestResult.getName(), unitTestResult.getThrowable().toString()));
						}
					}
				}

				overallTotal += total;
				overallPassed += passed;

				// if no total, avoid dividing by 0
				if (total == 0) {
					total = 1;
				}

				if (reportMode != ReportMode.MUTED) {
					output += String.format("\nPassed unit tests: %d/%d\n"
							+ "Failed unit tests: %d\n"
							+ "Skipped unit tests: %d\n"
							+ "Grade: %.1f%%\n",
							tc.getPassedTests().getAllResults().size(),
							tc.getAllTestMethods().length,
							tc.getFailedTests().getAllResults().size(),
							tc.getSkippedTests().getAllResults().size(),
							(passed / total) * 100);
				} else {
					output += "Unit tests were run, but no output will be shown.\n";
				}

				// add results to temp class
				results.addTest(index, tc.getName(), (passed / total) * 100, String.join("\n", failureReasons));
				index++;
			}
		}

		if (overallTotal == 0) {
			overallTotal = 1;
		}

		if (reportMode != ReportMode.MUTED) {
			output += String.format("\nOverall grade: %.1f%%\n", (overallPassed / overallTotal) * 100);
		}
		// global results to object
		results.setStudentOutput(output);
		results.setPercent((overallPassed / overallTotal) * 100);
	}

	/**
	 * Prints the results of a single unit test.
	 * @param test - the unit test object
	 * @param testMetadata - annotations
	 * @return friendly string
	 */
	private String getTestReportString(final ITestResult test, Gradable testMetadata) {
		if (reportMode == ReportMode.MUTED || reportMode == ReportMode.ANONYMOUS) {
			return "";
		}
		if (testMetadata == null) {
			testMetadata = getMockAnnotation();
		}
		String str = "";
		switch (test.getStatus()) {
		case ITestResult.SUCCESS:
			if (reportMode == ReportMode.VERBOSE) {
				str += String.format("SUCCESS: %s\n\t%d msecs, unit test weight: %d units\n", test.getName(),
						test.getEndMillis() - test.getStartMillis(), testMetadata.weight());
				str += (testMetadata.description() == null ? "" : String.format("\tDescription: %s\n", testMetadata.description()));
			}
			return str;
		case ITestResult.FAILURE:
			str += String.format("FAILURE: %s\n\t%d msecs, unit test weight: %d units\n", test.getName(),
					test.getEndMillis() - test.getStartMillis(), testMetadata.weight());
			str += (testMetadata.description() == null ? "" : String.format("\tDescription: %s\n", testMetadata.description()));
			str += String.format("\tException type: %s\n", test.getThrowable().getClass());
			if (testMetadata.printExceptionMessage() || reportMode == ReportMode.VERBOSE  || reportMode == ReportMode.MAXVERBOSE) {
				str += String.format("\tDetailed information:  %s\n", test.getThrowable().getMessage());
			}
			if (test.getThrowable() instanceof SecurityException
					&& test.getThrowable().getMessage().equals(StudentHelperClass.EXITVM_MSG)) {
				str += "\tWarning: It seems that System.exit() is used in the code. "
						+ "Please remove it to prevent the tester from working abnormally.\n";
			}
			if (testMetadata.printStackTrace() || reportMode == ReportMode.MAXVERBOSE) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				test.getThrowable().printStackTrace(pw);
				str += String.format("\tStack trace:  %s\n", sw.toString());
			}
			return str;
		case ITestResult.SKIP:
			str += String.format("SKIPPED: %s\n\tUnit test weight: %d units\n", test.getName(), testMetadata.weight());
			str += (testMetadata.description() == null ? "" : String.format("\tDescription: %s\n", testMetadata.description()));
			str += String.format("\tTest skipped because:  %s\n", test.getThrowable().toString());

			if (test.getMethod().getGroupsDependedUpon().length > 0) {
				str += String.format("\tThis unit test depends on groups: %s\n", String.join(", ", test.getMethod().getGroupsDependedUpon()));
			}
			if (test.getMethod().getMethodsDependedUpon().length > 0) {
				str += String.format("\tThis unit test depends on tests: %s\n", String.join(", ", test.getMethod().getMethodsDependedUpon()));
			}
			return str;
		default:
			StudentHelperClass.log("No such test result code: " + test.getStatus());
			return null;

		}
	}

	/**
	 * Gets the custom annotations from a unit test.
	 * @param test - unit test to get the metadata from
	 * @return annotation data if found
	 */
	private Gradable getTestMetadata(final ITestResult test) {
		try {
			Method m = test.getMethod().getConstructorOrMethod().getMethod();
			return (Gradable) m.getAnnotation(Gradable.class);
		} catch (SecurityException e) {
			StudentHelperClass.log(e.getMessage());
		}
		return null;
	}

	/**
	 * Gets the custom annotation from the first annotated
	 * class referenced in a test.
	 * @param context - context to get the metadata from
	 * @return annotation data if found
	 */
	private TestContextConfiguration getClassMetadata(final ITestContext context) {
		try {
			//Class<?> c = Class.forName(context.getName());
			for (XmlClass c : context.getCurrentXmlTest().getClasses()) {
				TestContextConfiguration a = ((Class<?>) c.getSupportClass()).getAnnotation(TestContextConfiguration.class);
				if (a != null) {
					return a;
				}
			}
			return null;
		} catch (SecurityException e) {
			StudentHelperClass.log(e.getMessage());
		}
		return null;
	}

	/**
	 * Mock annotation for unit tests that don't have one.
	 * @return default annotation for tests
	 */
	private Gradable getMockAnnotation() {
		Gradable annotation = new Gradable() {

			@Override
			public Class<? extends Annotation> annotationType() {
				return Gradable.class;
			}

			@Override
			public int weight() {
				return 1;
			}

			@Override
			public String description() {
				return null;
			}

			@Override
			public boolean printExceptionMessage() {
				return false;
			}

			@Override
			public boolean printStackTrace() {
				return false;
			}
		};

		return annotation;
	}

}