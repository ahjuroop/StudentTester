package ee.ttu.java.studenttester.classes;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static ee.ttu.java.studenttester.classes.Logger.log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
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
			isTestNGEnabled = true,            // is TestNG used
			isCustomCheckstyleSet = false,     // is custom checkstyle xml set
			isJsonOutput = false,              // print output to JSON instead
			muteCodeOutput = true,             // mute code output
			isQuiet = false;                   // print nothing to stdout if json enabled

	private String testRootName,     // test root folder pathname
	contentRootName,                 // content root folder pathname
	tempRootName,                    // temp folder pathname
	checkstyleXmlPathName,           // checkstyle xml pathname
	testNGXmlPathName,               // TestNG xml pathname
	outputFilename,                  // if not null, output will be written here
	compilerOptions;                 // string that is passed to the compiler

	private File testRoot,           // test root folder object
	contentRoot,                     // test root folder object
	tempRoot;                        // temp folder object

	private JsonObjectBuilder json;                  // object holding json data
	private JsonArrayBuilder singleResults;          // object holding json data for separate tests

	/**
	 * Runs the tester with current configuration.
	 */
	@SuppressWarnings("deprecation")
	public final void run() {

		// start measuring time
		long startTime = System.nanoTime();

		// check if any necessary variables are missing
		if (StudentHelperClass.checkAnyNull(testRoot, testRootName, tempRoot,
				tempRootName, contentRoot, contentRootName)) {
			log("One or more necessary directories are missing");
			if (isJsonOutput) {
				System.out.print("{\"output\": \"Internal error, testing cannot continue.\"}");
			}
			return;
		}

		// prepare json object if enabled, copy file contents to json
		if (isJsonOutput) {
			json = Json.createObjectBuilder();
			JsonArrayBuilder sourceList = Json.createArrayBuilder();
			singleResults = Json.createArrayBuilder();
			List<File> javaFiles = new ArrayList<File>();
			StudentHelperClass.populateFiles(contentRoot, javaFiles);
			try {
				for (File f: javaFiles) {
					String content = new String(Files.readAllBytes(Paths.get(f.getAbsolutePath())), StandardCharsets.UTF_8);
					log("Adding file " + f.getName() + " to output");
					sourceList.add(Json.createObjectBuilder()
							.add("path", f.getAbsolutePath())
							.add("content", content));
				}
			} catch (FileNotFoundException e) {
				log(e.getMessage());
			} catch (IOException e) {
				log(e.getMessage());
			}

			json.add("source", sourceList);
			json.add("extra", getCheckstyleXmlPath());

			// begin redirecting stdout to a variable so it can be included in json later
			StudentHelperClass.redirectStdOut();
		}

		System.out.format("TEST RESULTS\n\n");

		// run checkstyle
		if (checkstyleEnabled) {
			CheckstyleRunner checkstyle = new CheckstyleRunner(getCheckstyleXmlPath(), contentRoot, isJsonOutput, singleResults);
			checkstyle.run();
		}

		System.out.print("\n\n");

		// run TestNG
		if (isTestNGEnabled) {
			try {
				StudentHelperClass.deleteFolder(tempRoot);
				StudentHelperClass.copyFolder(contentRoot, tempRoot);
				StudentHelperClass.copyFolder(testRoot, tempRoot);

				List<String> testFilenames = new ArrayList<String>();
				StudentHelperClass.populateFilenames(testRoot, testFilenames, true);

				// compile tests
				CompilerRunner compiler = new CompilerRunner(testFilenames, tempRoot, testRoot);
				compiler.addOptions(compilerOptions);
				compiler.compileSeparately(true);
				if (compiler.run()) {
					TestNGRunner testng = new TestNGRunner(tempRoot, testRoot, isJsonOutput);
					if (isJsonOutput) {
						testng.setJsonVars(json, singleResults);
					}
					testng.setMuteCodeOutput(muteCodeOutput);
					testng.setTestNGXmlPathName(testNGXmlPathName);
					testng.run();
				}
			} catch (SecurityException e) {
				System.out.println("Testing was aborted via System.exit(). Remove the statement to continue.");
			} catch (NoClassDefFoundError e) {
				log(e.toString());
				System.out.println("Could not run one or more classes. "
						+ "Please check if the folder structure matches package definitions.");
			} catch (Exception e) {
				log(e.toString());
				e.printStackTrace();
				System.out.println("Internal error, cannot continue.");
			} finally {
				StudentHelperClass.enableSystemExit();
			}
		}

		if (!isTestNGEnabled && !checkstyleEnabled) {
			System.out.println("Nothing to run.");
		}

		StudentHelperClass.restoreStdOut();
		// print out json results
		if (isJsonOutput) {
			try {
				json.add("output", StudentHelperClass.getStdout().toString("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				log("UTF-8 decoding failed: " + e.getMessage());
				json.add("output", StudentHelperClass.getStdout().toString());
			}
			json.add("results", singleResults);
			if (!isQuiet) {
				if (outputFilename != null) {
					try (PrintWriter out = new PrintWriter(
							new OutputStreamWriter(new FileOutputStream(outputFilename), StandardCharsets.UTF_8))) {
						out.println(json.build().toString());
					} catch (FileNotFoundException e) {
						log(e.getMessage());
					}
				} else {
					System.out.println(json.build().toString());
				}
			}
		}
		StudentHelperClass.deleteFolder(tempRoot);

		// if any unit tests are still alive, kill them ungracefully to enable the program to exit
		Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
		for (Thread t : threadSet) {
			if (t.getName().startsWith("TestNGInvoker")) {
				log(String.format("Warning: attempting to kill stuck thread %s, consider "
						+ "making the method exit on InterruptedException", t.getName()));
				t.stop();
			}
		}

		log("Finished. Run time in ms: " + (System.nanoTime() - startTime) / 1000000);
	}



	/**
	 * Constructor.
	 */
	public StudentTesterClass() {
		StudentHelperClass.clearRedirectedStdOut(); // delete data from previous session
		// try to automatically get temp directory
		this.tempRootName = System.getProperty("java.io.tmpdir");
		if (tempRootName != null) {
			this.tempRoot = new File(tempRootName + "/testerTemp/");
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
		this.tempRootName = System.getProperty("java.io.tmpdir");
		if (tempRootName != null) {
			this.tempRoot = new File(tempRootName + "/testerTemp/");
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
		this.isTestNGEnabled = value;
	}

	/**
	 * Returns a path to checkstyle rules whether one is set or not.
	 * @return current valid checkstyle path
	 */
	private String getCheckstyleXmlPath() {
		if (isCustomCheckstyleSet) {
			return checkstyleXmlPathName;
		} else {
			File xml = new File(testRoot.getPath() + "/checkstyle.xml");
			if (xml.exists() && xml.isFile()) {
				return xml.getPath();
			}
			log("Checkstyle XML not found in root, falling back to default rules");
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
			isCustomCheckstyleSet = true;
			log("Checkstyle XML set successfully");
		} else {
			log("Checkstyle XML not found");
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
		this.tempRootName = tempDirectoryName;
		this.tempRoot = new File(tempDirectoryName);
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
			log("TestNG XML not found");
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
		Logger.setVerbosity(verbosity);
	}

	/**
	 * Output JSON instead of normal strings.
	 * @param value
	 */
	public final void outputJSON(final boolean value) {
		this.isJsonOutput = value;
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
		if (isJsonOutput) {
			this.isQuiet = quiet;
		} else {
			log("Quiet setting not set since json is not enabled.");
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

	/**
	 * Sets the output file path.
	 * @param filename where the file will be written to
	 */
	public final void setOutputFile(final String filename) {
		this.outputFilename = filename;
	}

	/**
	 * Sets the compiler options.
	 * @param options - information to be passed to the compiler
	 */
	public final void setCompilerOptions(final String options) {
		this.compilerOptions = options;
	}

	/**
	 * Gets the compiler options.
	 * @return information to be passed to the compiler
	 */
	public final String getCompilerOptions() {
		return this.compilerOptions;
	}
}
