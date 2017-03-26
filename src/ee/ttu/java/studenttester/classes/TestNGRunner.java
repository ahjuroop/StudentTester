package ee.ttu.java.studenttester.classes;
import static ee.ttu.java.studenttester.classes.Logger.log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import org.testng.IReporter;
import org.testng.TestNG;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import ee.ttu.java.studenttester.dataclasses.SingleTest;
import ee.ttu.java.studenttester.dataclasses.TestResults;
import ee.ttu.java.studenttester.interfaces.IBaseStudentReporter;
import ee.ttu.java.studenttester.listeners.MuteListener;
import ee.ttu.java.studenttester.listeners.StudentReporter;

public class TestNGRunner {

	private String testNGXmlPathName = null;
	private File tempRoot, testRoot;
	private boolean isJsonOutput;
	private boolean muteCodeOutput = true;
	private JsonObjectBuilder json;
	private JsonArrayBuilder singleResults;

	public TestNGRunner(File tempRoot, File testRoot, boolean isJsonOutput) {
		this.tempRoot = tempRoot;
		this.testRoot = testRoot;
		this.isJsonOutput = isJsonOutput;
	}

	public void setMuteCodeOutput(boolean muted) {
		this.muteCodeOutput = muted;
	}

	public void setTestNGXmlPathName(String name) {
		this.testNGXmlPathName = name;
	}

	public void setJsonVars(JsonObjectBuilder json, JsonArrayBuilder singleResults) {
		this.json = json;
		this.singleResults = singleResults;
	}

	/**
	 * Runs TestNG.
	 * @throws Exception if TestNG fails
	 */
	@SuppressWarnings("deprecation")
	public final void run() throws Exception {
		TestNG testng = new TestNG();
		boolean incompleteTests = false;

		// search for TestNG xml file
		if (testNGXmlPathName == null) {
			// attempt to use default path
			File f = new File(tempRoot.getPath() + "/testng.xml");
			if (!f.exists() || f.isDirectory()) {

				// here be dragons

				log("No testng.xml found, running all test classes");
				List<String> testFilenames = new ArrayList<String>();
				StudentHelperClass.populateFilenames(testRoot, testFilenames, true);

				List<XmlSuite> suites = new ArrayList<XmlSuite>();
				XmlSuite suite = new XmlSuite();
				suite.setName(testRoot.getName());
				List<XmlTest> tests = new ArrayList<XmlTest>();

				for (String testClass : testFilenames) {
					try {
						// confirm the existence of a compiled class
						Class.forName(StudentHelperClass.filePathToClassPath(testClass));
						XmlTest test;
						List<XmlClass> classes;
						switch (StudentHelperClass.getClassType(testClass)) {
						case JUNIT:
							test = new XmlTest(suite);
							classes = new ArrayList<XmlClass>();
							classes.add(new XmlClass(StudentHelperClass.filePathToClassPath(testClass)));
							test.setXmlClasses(classes);
							test.setName(StudentHelperClass.filePathToClassPath(testClass) + " (JUnit)");
							test.setJunit(true);
							Logger.log(String.format("Found JUnit class %s", testClass));
							tests.add(test);
							break;
						case TESTNG:
							test = new XmlTest(suite);
							classes = new ArrayList<XmlClass>();
							classes.add(new XmlClass(StudentHelperClass.filePathToClassPath(testClass)));
							test.setXmlClasses(classes);
							test.setName(StudentHelperClass.filePathToClassPath(testClass) + " (TestNG)");
							Logger.log(String.format("Found TestNG class %s", testClass));
							tests.add(test);
							break;
						case MIXED:
							Logger.log(String.format("Class %s contains mixed test annotations!", testClass));
							Logger.log("Skipping " + testClass);
							break;
						default:
							Logger.log("Skipping " + testClass);
						}
					} catch (ClassNotFoundException e) {
						Logger.log(e.toString());
						Logger.log("Class not found: " + testClass);
						incompleteTests = true;
					}
				}

				/*
				List<XmlClass> junitClasses = new ArrayList<XmlClass>();
				List<XmlClass> testngClasses = new ArrayList<XmlClass>();

				for (String testClass : testFilenames) {
					try {
						Class.forName(StudentHelperClass.filePathToClassPath(testClass)); // confirm the existence of a compiled class
						XmlClass c = new XmlClass(StudentHelperClass.filePathToClassPath(testClass));
						if (StudentHelperClass.isJUnitClass(testClass)) {
							log(String.format("Found JUnit class %s", testClass));
							junitClasses.add(c);
						} else {
							log(String.format("Found TestNG class %s", testClass));
							testngClasses.add(c);
						}
					} catch (ClassNotFoundException e) {
						log(e.toString());
						log("Skipping " + testClass);
						incompleteTests = true;
					}
				}
				if (junitClasses.size() > 0) {
					// create test for JUnit
					XmlTest testJunit = new XmlTest(suite);
					testJunit.setJUnit(true);
					testJunit.setName("JUnit tests");
					testJunit.setXmlClasses(junitClasses);
				}
				if (testngClasses.size() > 0) {
					// and for TestNG
					XmlTest testTestng = new XmlTest(suite);
					testTestng.setName("TestNG tests");
					testTestng.setXmlClasses(testngClasses);
				}
				if ((testngClasses.size() + junitClasses.size()) == 0) {
					log("Warning: nothing to test?");
				}
	*/
				// run in parallel, maybe more efficient?
				/*
				suite.setParallel(ParallelMode.METHODS);
				suite.setThreadCount(4);
				*/
				// nope, messes up stdout

				suites.add(suite);
				testng.setXmlSuites(suites);
			} else {
				testNGXmlPathName = tempRoot.getPath() + "/testng.xml";
				testng.setTestSuites(Arrays.asList(new String[] {testNGXmlPathName}));
			}
		} else {
			testng.setTestSuites(Arrays.asList(new String[] {testNGXmlPathName}));
		}

		// set TestNG verbosity. TestNG is supposed to have 10 levels.
		testng.setVerbose(Logger.getVerbosity());

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
		if (testNGXmlPathName != null) {
			try {
				String xmlData = new String(Files.readAllBytes(Paths.get(testNGXmlPathName)), StandardCharsets.UTF_8);
				Pattern LISTENER_PATTERN = Pattern.compile("listener\\s?class-name\\s?=\\s?\"(\\w+)\"");
				Matcher m = LISTENER_PATTERN.matcher(xmlData);
				while (m.find()) {
					customListener = m.group(1);
				}
			} catch (FileNotFoundException e1) {
				log("testNGXml file has unexpectedly disappeared");
				throw e1;
			}
		}

		if (customListener != null) {
			log("Using listener " + customListener);
		} else {
			log("Using default listener StudentReporter");
			// deprecated, see http://testng.org/doc/documentation-main.html#listeners-testng-xml
			// for now it's still the best way to configure programmatically
			testng.addListener(new StudentReporter());
		}

		// disable built-in listeners to reduce load
		testng.setUseDefaultListeners(false);

		// redirect some debug messages to stderr
		StudentHelperClass.stdoutToErr();

		// run TestNG. If an exception is thrown, restore streams.
		Exception tempEx = null;
		try {
			StudentHelperClass.disableSystemExit();
			testng.run();
		} catch (Exception e) {
			tempEx = e;
		} finally {
			if (!isJsonOutput) {
				// restore output if no json
				StudentHelperClass.restoreStdOut();
			} else {
				if (json == null || singleResults == null) {
					throw new StudentTesterException("JSON output specified but setJsonVars() not called?");
				}
				// redirect output to variable again if json
				StudentHelperClass.redirectStdOut();
			}
			if (tempEx != null) {
				throw tempEx;
			}
		}

		// pull results from the first IBaseStudentReporter
		TestResults results = null;
		for (IReporter reporter : testng.getReporters()) {
			if (reporter instanceof IBaseStudentReporter) {
				results = ((IBaseStudentReporter) reporter).getResults();
				if (isJsonOutput && results != null) {
					if (json == null || singleResults == null) {
						throw new StudentTesterException("JSON output specified but setJsonVars() not called?");
					}
					json.add("percent", results.getPercent());
					for (SingleTest t : results.getResultList()) {
						singleResults.add(Json.createObjectBuilder()
								.add("name", t.getName())
								.add("code", t.getCode())
								.add("percent", t.getPercent())
								.add("output", t.getOutput()));
					}
				}
				break;
			}
		}

		try {
			if (results.getOutput() != null) {
				System.out.print(results.getOutput());
			} else {
				throw new NullPointerException("results.getOutput() is null");
			}
		} catch (Exception e) {
			System.out.println("Error getting test results.");
			log("Result object was null, are reporters ok?");
			throw e;
		}
	}
}
