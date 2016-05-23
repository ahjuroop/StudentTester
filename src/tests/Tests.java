package tests;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.Random;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import studenttester.classes.StudentTesterClass;

import javax.json.Json;
import javax.json.JsonObject;

/**
 * Tests for StudentTester.
 * @author Andres
 *
 */
public class Tests {

    PrintWriter testFileWriter, studentCodeWriter;
    String tempDirName = System.getProperty("java.io.tmpdir");
    File testDir, sourceDir;
    int testCounter = 0;
    Random rn = new Random();

    /**
     * Initialize folders (create fixture).
     */
    @BeforeClass
    public void beforeClass() {

        if (tempDirName == null) {
            throw new NullPointerException("Temp folder not found!");
        }

        testDir = new File(tempDirName + "test");
        testDir.mkdir();
        sourceDir = new File(tempDirName + "source");
        sourceDir.mkdir();
    }

    /**
     * Delete junk files after tests (destroy fixture).
     */
    @AfterClass
    public void afterClass() {
        for (File f : testDir.listFiles()) {
            f.delete();
        }
        testDir.delete();
        for (File f : sourceDir.listFiles()) {
            f.delete();
        }
        sourceDir.delete();
    }

    /**
     * Create files before testing.
     */
    @BeforeMethod
    public void beforeMethod(final Method method) {
        method.getName();
        try {
            PrintWriter writer = new PrintWriter(tempDirName + "test/testng.xml", "UTF-8");
            writer.write(String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<!DOCTYPE suite SYSTEM \"http://testng.org/testng-1.0.dtd\" >"
                    + "<suite name=\"StudentReporterTest%1$s\">"
                    +     "<test name=\"StudentReporterTest%1$s\" %2$s>"
                    +         "<classes>"
                    +             "<class name=\"StudentReporterTest%1$s\"/>"
                    +         "</classes>"
                    +     "</test>"
                    + "</suite>", testCounter,
                        method.getName().toLowerCase().contains("junit")
                        ? "junit = \"true\"" : "")); // if method name contains "junit", run junit test
            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            testFileWriter = new PrintWriter(String.format(tempDirName + "test/StudentReporterTest%1$s.java", testCounter), "UTF-8");
            studentCodeWriter = new PrintWriter(String.format(tempDirName + "source/StudentCode%1$s.java", testCounter), "UTF-8");
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Delete files after testing.
     */
    @AfterMethod
    public void afterMethod() {
        File test = new File(String.format(tempDirName + "test/StudentReporterTest%1$s.java", testCounter));
        File code = new File(String.format(tempDirName + "source/StudentCode%1$s.java", testCounter));
        test.delete();
        code.delete();
        testCounter++;
    }

    /**
     * Finalises objects, runs the test and returns results.
     * @return test results in JSON format
     */
    private JsonObject getTestResults(final boolean checkStyleEnabled, final boolean testNGEnabled) {
        testFileWriter.close();
        studentCodeWriter.close();
        StudentTesterClass c = new StudentTesterClass(tempDirName + "test/", tempDirName + "source/");
        c.enableCheckstyle(checkStyleEnabled);
        c.enableTestNG(testNGEnabled);
        c.outputJSON(true);
        c.setQuiet(true);
        c.setVerbosity(0);
        c.run();
        JsonObject results = Json.createReader(new StringReader(c.getJson())).readObject();
        return results;
    }

    @Test(description = "Check if a simple test can receive a result from a class.")
    public void testTrivial() {
        testFileWriter.write(String.format("import org.testng.annotations.Test;"
                + "public class StudentReporterTest%1$s {"
                +     "@Test\r\n"
                +     "public void testSanity() {"
                +         "StudentCode%1$s c = new StudentCode%1$s();"
                +         "assert c.onePlusOne() == 2;"
                +     "}"
                + "}", testCounter));
        studentCodeWriter.write(String.format("public class StudentCode%1$s {"
                +     "public int onePlusOne() {"
                +         "return 1 + 1;"
                +     "}"
                + "}", testCounter));
        // the first argument enables/disables Checkstyle, the second is for TestNG
        JsonObject results = getTestResults(false, true);
        Assert.assertEquals(results.getInt("percent"), 100);
    }

    @Test(description = "Check if a simple JUnit test can receive a result from a class.")
    public void testJUnitTrivial() {
        testFileWriter.write(String.format("import org.junit.Test;"
                + "import static org.junit.Assert.*;"
                + "public class StudentReporterTest%1$s {"
                +     "@Test\r\n"
                +     "public void testSanity() {"
                +         "StudentCode%1$s c = new StudentCode%1$s();"
                +         "assertEquals(2, c.onePlusOne());"
                +     "}"
                + "}", testCounter));
        studentCodeWriter.write(String.format("public class StudentCode%1$s {"
                +     "public int onePlusOne() {"
                +         "return 1 + 1;"
                +     "}"
                + "}", testCounter));
        JsonObject results = getTestResults(false, true);
        Assert.assertEquals(results.getInt("percent"), 100);
    }

    @Test(description = "Check two exceptions. One should succeed, the other should not.")
    public void testTwoExceptions() {
        testFileWriter.write(String.format("import org.testng.annotations.Test;"
                + "public class StudentReporterTest%1$s {"
                +     "@Test(expectedExceptions = NullPointerException.class)\r\n"
                +     "public void testExceptionSuccess() {"
                +         "StudentCode%1$s c = new StudentCode%1$s();"
                +         "c.getException1();"
                +     "}"
                +     "@Test(expectedExceptions = NullPointerException.class)\r\n"
                +     "public void testExceptionFail() {"
                +         "StudentCode%1$s c = new StudentCode%1$s();"
                +         "c.getException2();"
                +     "}"
                + "}", testCounter));
        studentCodeWriter.write(String.format("public class StudentCode%1$s {"
                +     "public int getException1() {"
                +         "throw new NullPointerException();"
                +     "}"
                +     "public int getException2() {"
                +         "throw new IllegalArgumentException();"
                +     "}"
                + "}", testCounter));
        JsonObject results = getTestResults(false, true);
        Assert.assertEquals(results.getInt("percent"), 50);
    }

    @Test(description = "Create a muted test. Test names and scores should not be visible.")
    public void testMutedTest() {
        testFileWriter.write(String.format("import org.testng.annotations.Test;"
                + "@GlobalConfiguration(mode = ReportMode.MUTED)\r\n"
                + "public class StudentReporterTest%1$s {"
                +     "@Test\r\n"
                +     "public void testOnePlusOne() {"
                +         "StudentCode%1$s c = new StudentCode%1$s();"
                +         "assert c.onePlusOne() == 2;"
                +     "}"
                + "}", testCounter));
        studentCodeWriter.write(String.format("public class StudentCode%1$s {"
                +     "public int onePlusOne() {"
                +         "return 1 + 1;"
                +     "}"
                + "}", testCounter));
        JsonObject results = getTestResults(false, true);
        assert !results.getString("output").contains("testOnePlusOne");
        assert !results.getString("output").contains("Final score");
    }

    @Test(description = "Test weights with random numbers. Some tests fail, some do not. The grade must be correct.")
    public void testWeights100() {
        final int TEST_AMOUNT = 100;
        boolean[] testStatus = new boolean[TEST_AMOUNT];
        int[] testWeights = new int[TEST_AMOUNT];

        double expectedTotal = 0;
        double expectedPassed = 0;
        String tests = "";

        for (int i = 0; i < TEST_AMOUNT; i++) {
            testStatus[i] = rn.nextBoolean();
            testWeights[i] = rn.nextInt(101);
            expectedTotal += testWeights[i];
            if (testStatus[i]) {
                expectedPassed += testWeights[i];
            }
            tests += (String.format("@Gradeable(weight = %1$s)\r\n"
                    +     "@Test\r\n"
                    +     "public void test%2$s() {"
                    +         "assert %3$s;"
                    +     "}", testWeights[i], i, testStatus[i]));
        }

        if (expectedTotal == 0) {
            expectedTotal = 1;
        }
        double expectedGrade = (expectedPassed / expectedTotal) * 100;

        testFileWriter.write(String.format("import org.testng.annotations.Test;"
                + "import studenttester.annotations.*;"
                + "import studenttester.enums.*;"
                + "@GlobalConfiguration(mode = ReportMode.MUTED)\r\n"
                + "public class StudentReporterTest%1$s {"
                +  "%2$s"
                + "}", testCounter, tests));
        studentCodeWriter.write(String.format("public class StudentCode%1$s {"
                + "}", testCounter));
        JsonObject results = getTestResults(false, true);
        Assert.assertEquals(results.getJsonNumber("percent").doubleValue(), expectedGrade, 0.1);
    }

    @Test(description = "Check the output contains 'Nothing to run.' when both Checkstyle and TestNG are disabled.")
    public void testDoNothing() {
        testFileWriter.write(String.format(""
                + "public class StudentReporterTest%1$s {"
                + "}", testCounter));
        studentCodeWriter.write(String.format("public class StudentCode%1$s {"
                + "}", testCounter));
        JsonObject results = getTestResults(false, false);
        assert results.getString("output").contains("Nothing to run.");
    }

    @Test(description = "Check if student's compilation error is displayed correctly.")
    public void testBrokenStudentCode() {
        testFileWriter.write(String.format("import org.testng.annotations.Test;"
                + "public class StudentReporterTest%1$s {"
                +     "@Test\r\n"
                +     "public void testSanity() {"
                +         "StudentCode%1$s c = new StudentCode%1$s();"
                +         "assert c.onePlusOne() == 2;"
                +     "}"
                + "}", testCounter));
        studentCodeWriter.write(String.format("public class StudentCode%1$s {"
                +     "public int onePlusOne() {"
                +         "return 1 + 1"
                +     "}"
                + "}", testCounter));
        JsonObject results = getTestResults(false, true);
        assert results.getString("output").contains("Error in StudentCode0.java: ';' expected");
    }
}
