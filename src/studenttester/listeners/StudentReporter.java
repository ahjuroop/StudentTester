package studenttester.listeners;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.testng.IReporter;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;

import studenttester.annotations.GlobalConfiguration;
import studenttester.annotations.Gradeable;
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

    @Override
    public void generateReport(final List<XmlSuite> xmlSuites, final List<ISuite> suites, final String outputDirectory) {
        results = new TestResults(); // prepare new test results object
        int index = 1; // test counter for json
        double overallTotal = 0; // overall total score
        double overallPassed = 0; // overall passed score
        String output = ""; // test output string

        for (ISuite suite : suites) {

            output += String.format("Test suite %s\n", suite.getName());
            Map<String, ISuiteResult> suiteResults = suite.getResults();

            for (ISuiteResult sr : suiteResults.values()) {

                ITestContext tc = sr.getTestContext();
                GlobalConfiguration conf = getClassMetadata(tc);

                // get info from class annotation
                if (conf != null && conf.mode() != null) {
                    reportMode = conf.mode();
                } else {
                    reportMode = ReportMode.NORMAL;
                }

                output += String.format("\nTest context %s\n", tc.getName());
                if (conf != null && !conf.welcomeMessage().isEmpty()) {
                    output += String.format("%s\n", conf.welcomeMessage());
                }

                double total = 0;
                double passed = 0;

                for (ITestResult passedTest : tc.getPassedTests().getAllResults()) {
                    Gradeable testMetadata = getTestMetadata(passedTest);
                    if (testMetadata != null) {
                        output += (getTestReport(passedTest, testMetadata));
                        passed += testMetadata.weight();
                        total += testMetadata.weight();
                    } else {
                        output += (getTestReport(passedTest, getMockAnnotation()));
                        passed += getMockAnnotation().weight();
                        total += getMockAnnotation().weight();
                    }
                }
                for (ITestResult failedTest : tc.getFailedTests().getAllResults()) {
                    Gradeable testMetadata = getTestMetadata(failedTest);
                    if (testMetadata != null) {
                        output += (getTestReport(failedTest, testMetadata));
                        total += testMetadata.weight();
                    } else {
                        output += (getTestReport(failedTest, getMockAnnotation()));
                        total += getMockAnnotation().weight();
                    }
                }
                for (ITestResult skippedTest : tc.getSkippedTests().getAllResults()) {
                    Gradeable testMetadata = getTestMetadata(skippedTest);
                    if (testMetadata != null) {
                        output += (getTestReport(skippedTest, testMetadata));
                        total += testMetadata.weight();
                    } else {
                        output += (getTestReport(skippedTest, getMockAnnotation()));
                        total += getMockAnnotation().weight();
                    }
                }

                overallTotal += total;
                overallPassed += passed;

                // if no total
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
                    output += "Unit tests were run.\n";
                }


                // add results to temp class
                results.addTest(index, tc.getName(), (passed / total) * 100);
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
        results.setOutput(output);
        results.setPercent((overallPassed / overallTotal) * 100);
    }

    /**
     * Prints the results of a single unit test.
     * @param test - the unit test object
     * @param testMetadata - annotations
     * @return friendly string
     */
    private String getTestReport(final ITestResult test, Gradeable testMetadata) {
        if (reportMode == ReportMode.MUTED || reportMode == ReportMode.ANONYMOUS) {
            return "";
        }
        if (testMetadata == null) {
            testMetadata = getMockAnnotation();
        }
        String str = "";
        switch (test.getStatus()) {
        case ITestResult.SUCCESS:
            str += String.format("SUCCESS: %s\n\t%d msecs, unit test weight: %d units\n", test.getName(),
                    test.getEndMillis() - test.getStartMillis(), testMetadata.weight());
            if (reportMode == ReportMode.VERBOSE) {
                // if no description, omit this line
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
    private Gradeable getTestMetadata(final ITestResult test) {
        try {
            Method m = test.getMethod().getConstructorOrMethod().getMethod();
            return (Gradeable) m.getAnnotation(Gradeable.class);
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
    private GlobalConfiguration getClassMetadata(final ITestContext context) {
        try {
            //Class<?> c = Class.forName(context.getName());
            for (XmlClass c : context.getCurrentXmlTest().getClasses()) {
                GlobalConfiguration a = ((Class<?>) c.getSupportClass()).getAnnotation(GlobalConfiguration.class);
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
    private Gradeable getMockAnnotation() {
        Gradeable annotation = new Gradeable() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return Gradeable.class;
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