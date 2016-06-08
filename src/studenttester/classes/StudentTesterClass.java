package studenttester.classes;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import org.testng.IReporter;
import org.testng.TestNG;

import studenttester.dataclasses.SingleTest;
import studenttester.dataclasses.TestResults;
import studenttester.interfaces.IBaseStudentReporter;
import studenttester.listeners.MuteListener;
import studenttester.listeners.StudentReporter;

/**
 *
 * @author Andres
 *
 */
public class StudentTesterClass {

    /**
     * Default checkstyle rules for fallback.
     */
    private static final String DEFAULT_CHECKSTYLE_RULES = "/sun_checks.xml";

    private boolean checkstyleEnabled = true,// is checkstyle used
            testNGEnabled = true,            // is TestNG used
            customCheckstyleSet = false,     // is custom checkstyle xml set
            jsonOutput = false,              // print output to JSON instead
            muteCodeOutput = true,           // mute code output
            quiet = false;                   // print nothing to stdout if json enabled

    private String testRootName,             // test root folder pathname
    contentRootName,                 // content root folder pathname
    tempDirectoryName,               // temp folder pathname
    checkstyleXmlPathName,           // checkstyle xml pathname
    testNGXmlPathName;               // TestNG xml pathname

    private File    testRoot,        // test root folder object
    contentRoot,                     // test root folder object
    tempDirectory;                   // temp folder object

    private JsonObjectBuilder json;                  // object holding json data
    private JsonArrayBuilder singleResults;          // object holding json data for separate tests

    /**
     * Runs the tester with current configuration.
     */
    public final void run() {

        // check if any necessary variables are missing
        if (StudentHelperClass.checkAnyNull(testRoot, testRootName, tempDirectory,
                tempDirectoryName, contentRoot, contentRootName)) {
            StudentHelperClass.log("One or more necessary directories are missing");
            if (jsonOutput) {
                System.out.print("{\"output\": \"Internal error, testing cannot continue.\"}");
            }
            return;
        }

        // prepare json object if enabled, copy file contents to json
        if (jsonOutput) {

            json = Json.createObjectBuilder();
            JsonArrayBuilder sourceList = Json.createArrayBuilder();
            singleResults = Json.createArrayBuilder();
            List<File> javaFiles = new ArrayList<File>();
            StudentHelperClass.populateFiles(contentRoot, javaFiles);

            try {
                for (File f: javaFiles) {
                    String content = new String(Files.readAllBytes(Paths.get(f.getAbsolutePath())), StandardCharsets.UTF_8);
                    StudentHelperClass.log("Adding file " + f.getName() + " to output");
                    sourceList.add(Json.createObjectBuilder()
                            .add("path", f.getAbsolutePath())
                            .add("content", content));
                }
            } catch (FileNotFoundException e) {
                StudentHelperClass.log(e.getMessage());
            } catch (IOException e) {
                StudentHelperClass.log(e.getMessage());
            }

            json.add("source", sourceList);
            json.add("extra", getCheckstyleXmlPath());

            // begin redirecting stdout to a variable so it can be included in json later
            StudentHelperClass.redirectStdOut();
        }

        System.out.println("TEST RESULTS\n");

        // run checkstyle
        if (checkstyleEnabled) {
            Checkstyle checkstyle = new Checkstyle(getCheckstyleXmlPath(), contentRoot, jsonOutput, singleResults);
            checkstyle.run();
        }

        System.out.print("\n\n");

        // run TestNG
        if (testNGEnabled) {
            try {
                StudentHelperClass.deleteFolder(tempDirectory);
                StudentHelperClass.copyFolder(contentRoot, tempDirectory);
                StudentHelperClass.copyFolder(testRoot, tempDirectory);
                List<File> toBeCompiled = new ArrayList<File>();
                StudentHelperClass.populateFiles(tempDirectory, toBeCompiled);
                // compile everything
                Compiler compiler = new Compiler(toBeCompiled, contentRoot, tempDirectory);
                if (compiler.run()) {
                    runTestNG();
                }
            } catch (Exception e) {
                StudentHelperClass.log(e.toString());
                System.out.println("Internal error, cannot continue.");
            }

        }

        if (!testNGEnabled && !checkstyleEnabled) {
            System.out.println("Nothing to run.");
        }

        StudentHelperClass.restoreStdOut();
        // print out json results
        if (jsonOutput) {
            json.add("output", StudentHelperClass.getStdout().toString());
            json.add("results", singleResults);
            if (!quiet) {
                System.out.println(json.build().toString());
            }
        }

        StudentHelperClass.deleteFolder(tempDirectory);
    }


    /**
     * Runs TestNG.
     */
    private void runTestNG() {
        TestNG testng = new TestNG();

        // search for TestNG xml file
        if (testNGXmlPathName == null) {
            File f = new File(tempDirectory.getPath() + "/testng.xml");
            if (!f.exists() || f.isDirectory()) {
                StudentHelperClass.log("No testng.xml found");
                System.out.println("Internal error, testing cannot continue.");
                return;
            }
            testNGXmlPathName = tempDirectory.getPath() + "/testng.xml";
        }
        testng.setTestSuites(Arrays.asList(new String[] {testNGXmlPathName}));

        // set TestNG verbosity. TestNG is supposed to have 10 levels.
        testng.setVerbose(StudentHelperClass.getVerbosity());

        // mute output while testing, this should be kept on
        if (muteCodeOutput) {
            testng.addListener(new MuteListener());
        } else {
            StudentHelperClass.stdoutToErr();
        }

        // TestNG does not appear to have an interface to examine xml contents.
        // Attempt to parse xml manually to find custom listeners
        // If the listener is not a reporter, you have to add it as well
        String customListener = null;
        try {
            String xmlData = new String(Files.readAllBytes(Paths.get(testNGXmlPathName)), StandardCharsets.UTF_8);
            Pattern LISTENER_PATTERN = Pattern.compile("listener\\s?class-name\\s?=\\s?\"(\\w+)\"");
            Matcher m = LISTENER_PATTERN.matcher(xmlData);
            while (m.find()) {
                customListener = m.group(1);
            }
        } catch (FileNotFoundException e1) {
            StudentHelperClass.log("testNGXml file has unexpectedly disappeared");
        } catch (Exception e) {
            StudentHelperClass.log(e.getMessage());
        }

        if (customListener != null) {
            StudentHelperClass.log("Using listener " + customListener);
        } else {
            StudentHelperClass.log("Using default listener StudentReporter");
            testng.addListener(new StudentReporter());
        }

        testng.run();

        // pull results from the first IBaseStudentReporter
        TestResults results = null;
        for (IReporter reporter : testng.getReporters()) {
            if (reporter instanceof IBaseStudentReporter) {
                results = ((IBaseStudentReporter) reporter).getResults();
                if (jsonOutput && results != null) {
                    json.add("percent", results.getPercent());
                    for (SingleTest t : results.getResultList()) {
                        singleResults.add(Json.createObjectBuilder()
                                .add("name", t.getName())
                                .add("code", t.getCode())
                                .add("percent", t.getPercent()));
                    }
                }
                break;
            }
        }

        if (!jsonOutput) {
            // restore output if no json
            StudentHelperClass.restoreStdOut();
        } else {
            // redirect output to variable again if json
            StudentHelperClass.redirectStdOut();
        }
        try {
            System.out.print(results.getOutput());
        } catch (Exception e) {
            System.out.println("Error getting test results.");
            StudentHelperClass.log("Result object was null, are reporters ok?");
        }
    }

    /**
     * Constructor.
     */
    public StudentTesterClass() {
        StudentHelperClass.clearRedirectedStdOut(); // delete data from previous session
        // try to automatically get temp directory
        this.tempDirectoryName = System.getProperty("java.io.tmpdir");
        if (tempDirectoryName != null) {
            this.tempDirectory = new File(tempDirectoryName + "/testerTemp/");
        }
    }

    /**
     * Creates a tester with minimal arguments.
     * @param testRootName - test root folder
     * @param contentRootName - student code folder
     */
    public StudentTesterClass(final String testRootName, final String contentRootName) {
        StudentHelperClass.clearRedirectedStdOut(); // delete data from previous session
        // try to automatically get temp directory
        this.testRootName = testRootName;
        this.contentRootName = contentRootName;
        this.tempDirectoryName = System.getProperty("java.io.tmpdir");
        if (tempDirectoryName != null) {
            this.tempDirectory = new File(tempDirectoryName + "/testerTemp/");
        }
        this.testRoot = new File(testRootName);
        this.contentRoot = new File(contentRootName);
    }

    /**
     * Enables or disables checkstyle.
     * @param value - disable if false
     */
    public final void enableCheckstyle(final boolean value) {
        this.checkstyleEnabled = value;
    }

    /**
     * Enables or disables TestNG.
     * @param value - disable if false
     */
    public final void enableTestNG(final boolean value) {
        this.testNGEnabled = value;
    }

    /**
     * Returns a path to checkstyle rules whether one is set or not.
     * @return current valid checkstyle path
     */
    private String getCheckstyleXmlPath() {
        if (customCheckstyleSet) {
            return checkstyleXmlPathName;
        } else {
            File xml = new File(testRoot.getPath() + "/checkstyle.xml");
            if (xml.exists() && xml.isFile()) {
                return xml.getPath();
            }
            StudentHelperClass.log("Checkstyle XML not found in root, falling back to default rules");
        }
        return DEFAULT_CHECKSTYLE_RULES;
    }

    /**
     * Sets the checkstyle xml file, if the path is correct.
     * @param xmlPath - path to xml
     */
    public final void setCheckstyleXml(final String xmlPath) {
        File xml = new File(xmlPath);
        if (xml.exists() && !xml.isDirectory()) {
            this.checkstyleXmlPathName = xmlPath;
            customCheckstyleSet = true;
            StudentHelperClass.log("Checkstyle XML set successfully");
        } else {
            StudentHelperClass.log("Checkstyle XML not found");
        }
    }

    /**
     * Sets the content root filename.
     * @param contentRootName - path to content root
     */
    public final void setContentRootName(final String contentRootName) {
        this.contentRootName = contentRootName;
        this.contentRoot = new File(contentRootName);
    }

    /**
     * Sets the temporary folder name.
     * @param tempDirectoryName - path to temp folder
     */
    public final void setTempDirectoryName(final String tempDirectoryName) {
        this.tempDirectoryName = tempDirectoryName;
        this.tempDirectory = new File(tempDirectoryName);
    }

    /**
     * Sets the TestNG xml file, if the path is correct.
     * @param xmlPath - path to xml
     */
    public final void setTestNGXml(final String xmlPath) {
        File xml = new File(xmlPath);
        if (xml.exists() && !xml.isDirectory()) {
            this.testNGXmlPathName = xmlPath;
        } else {
            StudentHelperClass.log("TestNG XML not found");
        }
    }

    /**
     * Sets the test folder name.
     * @param testRootName - path to test folder
     */
    public final void setTestRootName(final String testRootName) {
        this.testRootName = testRootName;
        this.testRoot = new File(testRootName);
    }

    /**
     * Sets the verbosity of the project.
     * TestNG has levels 1-10, this project has less.
     * @param verbosity - verbosity level
     */
    public final void setVerbosity(final int verbosity) {
        StudentHelperClass.setVerbosity(verbosity);
    }

    /**
     * Output JSON instead of normal strings.
     * @param value
     */
    public final void outputJSON(final boolean value) {
        this.jsonOutput = value;
    }

    /**
     * Disables stdout to suppress students' debug messages.
     * @param value
     */
    public final void muteCodeOutput(final boolean value) {
        this.muteCodeOutput = value;
    }

    /**
     * Sets the quiet state, if JSON output is enabled.
     * @param quiet state
     */
    public final void setQuiet(final boolean quiet) {
        if (jsonOutput) {
            this.quiet = quiet;
        } else {
            StudentHelperClass.log("Quiet setting not set since json is not enabled.");
        }
    }

    /**
     * Returns the json of this instance.
     * @return json
     */
    public final String getJson() {
        if (json != null) {
            return json.build().toString();
        }
        return null;
    }
}
