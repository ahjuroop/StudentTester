package ee.ttu.java.studenttester.classes;
import static ee.ttu.java.studenttester.classes.StudentLogger.log;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ee.ttu.java.studenttester.enums.StudentPolicy;
import ee.ttu.java.studenttester.exceptions.StudentTesterException;
import org.json.JSONArray;
import org.json.JSONObject;
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

/**
 * Class for running TestNG instances.
 * @author Andres
 *
 */
public class TestNGRunner {

	//TODO: mostly copies from StudentTesterMain, maybe inheritance would be better?
	private String testNGXmlPathName = null;
	private File tempRoot, testRoot, contentRoot;
	private boolean isJsonOutput;
	private boolean muteCodeOutput = true;
	private JSONObject json;
	private JSONArray singleResults;

	private StudentSecurity secInst = StudentSecurity.getInstance();

	/**
	 * Creates a new TestNG wrapper class.
	 * @param tempRoot folder containing tests and code copied together
	 * @param testRoot folder containing unit tests
	 * @param isJsonOutput whether the tester is outputting JSON
	 */
	public TestNGRunner(final File tempRoot, final File testRoot, final File contentRoot, final boolean isJsonOutput) {
		this.tempRoot = tempRoot;
		this.testRoot = testRoot;
		this.contentRoot = contentRoot;
		this.isJsonOutput = isJsonOutput;
	}

	/**
	 * Sets whether System.out is redirected to null during testing.
	 * @param muted if true, System.out will be suppressed
	 */
	public void setMuteCodeOutput(final boolean muted) {
		this.muteCodeOutput = muted;
	}

	/**
	 * Sets the pathname to TestNG xml conf file.
	 * @param name pathname to xml
	 */
	public void setTestNGXmlPathName(final String name) {
		this.testNGXmlPathName = name;
	}

	/**
	 * If the results are to be stored in JSON, include necessary objects.
	 * @param json JSON root object
	 * @param singleResults array for single tests to be included in root
	 */
	public void setJsonVars(final JSONObject json, final JSONArray singleResults) {
		this.json = json;
		this.singleResults = singleResults;
	}

	/**
	 * Runs TestNG.
	 * @throws Exception if TestNG fails or someone coughs
	 */
	@SuppressWarnings("deprecation")
	public final void run() throws Exception {

		TestNG testng = new TestNG();
		boolean incompleteTests = false;
		// get a fancy new loader so Java 9 does not scream in our face
		URLClassLoader loader = URLClassLoader.newInstance(new URL[] {tempRoot.toURI().toURL()});
		testng.addClassLoader(loader); // must be declared here, otherwise using testng.xml will not work
		// search for TestNG xml file
		if (testNGXmlPathName == null) {
			// attempt to use default path
			File f = new File(tempRoot.getPath() + "/testng.xml");
			if (!f.exists() || f.isDirectory()) {

				// here be dragons

				log("No testng.xml found, running all test classes");
				List<String> testFilenames = new ArrayList<String>();
				StudentHelperClass.populateFilenames(testRoot, testFilenames, true);

				// add test files to protected list. Do not add paths as there might be differences under Linux/Windows
				// path separators
				// TODO: implement paths anyway?
				List<String> testFilenamesNoPath = new ArrayList<String>();
				StudentHelperClass.populateFilenames(testRoot, testFilenamesNoPath, false);
				for (String testClassName : testFilenamesNoPath) {
						secInst.addProtectedFile(testClassName); // add .java file to protected list
						secInst.addProtectedFile(testClassName.replace(".java", ".class")); // add .class file to protected list
				}

				List<String> codeFilenames = new ArrayList<String>();
				StudentHelperClass.populateFilenames(contentRoot, codeFilenames, true);

				for (String codeClassName : codeFilenames) {
					try {
						Class testClass = loader.loadClass(StudentHelperClass.filePathToClassPath(codeClassName));
						// add to blacklist
						secInst.addClass(testClass);
					} catch (ClassNotFoundException e) {
						StudentLogger.log(e.toString());
						StudentLogger.log("Class not found: " + codeClassName);
					}
				}

				List<XmlSuite> suites = new ArrayList<XmlSuite>();
				XmlSuite suite = new XmlSuite();
				suite.setName(testRoot.getName());
				List<XmlTest> tests = new ArrayList<XmlTest>();
				for (String testClassName : testFilenames) {
					try {
						// confirm the existence of a compiled class
						// in Java 8, it was possible to dynamically load a path to classpath (in our case the temp folder),
						// but in Java 9 such hacks do not work. Instead, load the class through a custom URLClassLoader
						// and pass it as an object to TestNG. Something WILL blow up (see the comment ~20 lines above)
						Class testClass = loader.loadClass(StudentHelperClass.filePathToClassPath(testClassName));
						XmlTest test;
						List<XmlClass> classes;
						switch (StudentHelperClass.getClassType(testClass)) {
						case JUNIT:
							test = new XmlTest(suite);
							classes = new ArrayList<XmlClass>();
							//classes.add(new XmlClass(StudentHelperClass.filePathToClassPath(testClassName)));
							classes.add(new XmlClass(testClass));
							test.setXmlClasses(classes);
							test.setName(StudentHelperClass.filePathToClassPath(testClassName) + " (JUnit)");
							test.setJunit(true);
							StudentLogger.log(String.format("Found JUnit class %s", testClassName));
							tests.add(test);
							break;
						case TESTNG:
							test = new XmlTest(suite);
							classes = new ArrayList<XmlClass>();
							classes.add(new XmlClass(testClass));
							test.setXmlClasses(classes);
							test.setName(StudentHelperClass.filePathToClassPath(testClassName) + " (TestNG)");
							StudentLogger.log(String.format("Found TestNG class %s", testClassName));
							tests.add(test);
							break;
						case MIXED:
							StudentLogger.log(String.format("Class %s contains mixed test annotations!", testClassName));
							StudentLogger.log("Skipping " + testClassName);
							break;
						default:
							StudentLogger.log("Skipping " + testClassName);
						}
					} catch (ClassNotFoundException e) {
						StudentLogger.log(e.toString());
						StudentLogger.log("Class not found: " + testClassName);
						incompleteTests = true;
					}
				}

				/* older implementation of the above
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
		testng.setVerbose(StudentLogger.getVerbosity());

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
			String xmlData = new String(Files.readAllBytes(Paths.get(testNGXmlPathName)), StandardCharsets.UTF_8);
			Pattern LISTENER_PATTERN = Pattern.compile("listener\\s?class-name\\s?=\\s?\"(\\w+)\"");
			Matcher m = LISTENER_PATTERN.matcher(xmlData);
			while (m.find()) {
				customListener = m.group(1);
			}
		}

		StudentReporter reporter = null;
		if (customListener != null) {
			log("Using listener " + customListener);
		} else {
			log("Using default listener StudentReporter");
			reporter = new StudentReporter();
			reporter.setHasIncompleteResults(incompleteTests);
			// deprecated, see http://testng.org/doc/documentation-main.html#listeners-testng-xml
			// for now it's still the best way to configure programmatically
			testng.addListener(reporter);
		}

		// disable built-in listeners to reduce load
		testng.setUseDefaultListeners(false);

		// redirect some debug messages to stderr
		StudentHelperClass.stdoutToErr();

		// run TestNG. If an exception is thrown, restore streams.
		Exception tempEx = null;
		try {
			secInst.setDefaultRestrictions();
			secInst.setCustomSecurityManager();

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

		TestResults results = null;
		// if a custom listener was specified
		if (customListener != null) {
			// TODO: untested functionality
			// try to pull results from the first IBaseStudentReporter
			for (IReporter otherReporter : testng.getReporters()) {
				if (otherReporter instanceof IBaseStudentReporter) {
					results = ((IBaseStudentReporter) otherReporter).getResults();
					break;
				}
			}
		// otherwise get from our default StudentReporter
		} else {
			results = reporter.getResults();
		}

		// if JSON specified
		if (isJsonOutput && results != null) {
			if (json == null || singleResults == null) {
				throw new StudentTesterException("JSON output specified but setJsonVars() not called?");
			}
			json.put("percent", results.getPercent());
			for (SingleTest t : results.getResultList()) {
				singleResults.put(new JSONObject()
						.put("name", t.getName())
						.put("code", t.getCode())
						.put("percent", t.getPercent())
						.put("output", t.getErrorOutput()));
			}
		}

		// try to print out all results
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
