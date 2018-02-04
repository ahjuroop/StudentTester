package ee.ttu.java.studenttester.classes;
import ee.ttu.java.studenttester.exceptions.StudentTesterException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static ee.ttu.java.studenttester.classes.StudentLogger.log;

/**
 *
 * @author Andres Antonen
 *
 */
public final class StudentTesterMain {

	/**
	 * Default checkstyle rules for fallback.
	 */
	private static final String DEFAULT_CHECKSTYLE_RULES = "/sun_checks.xml";
	private static boolean isRunning = false;  // current running state
	private boolean checkstyleEnabled = true,  // is checkstyle used
			isTestNGEnabled = true,            // is TestNG used
			isCustomCheckstyleSet = false,     // is custom checkstyle xml set
			isJsonOutput = false,              // print output to JSON instead
			muteCodeOutput = true,             // mute code output
			isQuiet = false;                   // print nothing to stdout if json enabled
	private String testRootName,             // test root folder pathname
			contentRootName,                 // content root folder pathname
			tempRootName,                    // temp folder pathname
			checkstyleXmlPathName,           // checkstyle xml pathname
			testNGXmlPathName,               // TestNG xml pathname
			outputFilename,                  // if not null, output will be written here
			compilerOptions;                 // string that is passed to the compiler
	private File testRoot,                   // test root folder object
			contentRoot,                     // test root folder object
			tempRoot;                        // temp folder object
	private JSONObject json;                  // object holding json data
	private JSONArray singleResults;          // object holding json data for separate tests
	private StudentSecurity secInstance = StudentSecurity.getInstance();

	/**
	 * Constructor.
	 */
	public StudentTesterMain() {
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
	public StudentTesterMain(final String testRootName, final String contentRootName) {
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
	 * Main entry point.
	 * @param args - see usage javadoc
	 */
	public static void main(String[] args) throws StudentTesterException {
		if (args.length == 0) {
			System.err.println(getUsage());
			System.exit(1);
		}
		StudentTesterMain c = new StudentTesterMain();
		try {
			for (int i = 0; i < args.length; i++) {
				// parse arguments and build tester
				switch (args[i].toLowerCase()) {
				case "-testroot":
					if (quickFileCheck(args[i + 1], true)) {
						c.setTestRootName(args[i + 1]);
						i++;
					} else {
						System.err.println("Could not find test root folder: " + args[i + 1]);
						System.exit(1);
					}
					break;
				case "-contentroot":
					if (quickFileCheck(args[i + 1], true)) {
						c.setContentRootName(args[i + 1]);
						i++;
					} else {
						System.err.println("Could not find content root folder: " + args[i + 1]);
						System.exit(1);
					}
					break;
				case "-temproot":
					c.setTempDirectoryName(args[i + 1]);
					i++;
					break;
				case "-jsonfile":
					c.setOutputFile(args[i + 1]);
					i++;
					c.outputJSON(true);
					break;
				case "-verbosity":
					try {
						int v = Integer.parseInt(args[i + 1]);
						c.setVerbosity(v);
						i++;
					} catch (Exception e) {
						System.err.println("Could not set verbosity level: " + e.getMessage());
					}
					break;
				case "-nocheckstyle":
					c.enableCheckstyle(false);
					break;
				case "-notestng":
					c.enableTestNG(false);
					break;
				case "-jsonoutput":
					c.outputJSON(true);
					break;
				case "-nomute":
					c.muteCodeOutput(false);
					break;
				case "-checkstylexml":
					if (quickFileCheck(args[i + 1], false)) {
						c.setCheckstyleXml(args[i + 1]);
						i++;
					} else {
						System.err.println("Could not find checkstyle xml: " + args[i + 1]);
					}
					break;
				case "-testngxml":
					if (quickFileCheck(args[i + 1], false)) {
						c.setTestNGXml(args[i + 1]);
						i++;
					} else {
						System.err.println("Could not find TestNG xml: " + args[i + 1]);
					}
					break;
				case "-javacoptions":
					c.setCompilerOptions(args[i + 1]);
					i++;
					break;
				default:
					System.err.println("Unknown argument: " + args[i]);
					System.err.println(getUsage());
					System.exit(1);
				}
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println(getUsage());
			System.exit(1);
		}
		c.run();
	}

	/**
	 * Prints the instruction manual for this tool.
	 * @return text
	 */
	private static String getUsage() {
		return "Usage:\n"
				+ "StudentTester -testRoot [path] -contentRoot [path] [options]\n"
				+ "\nOptions:\n"
				+ "-tempRoot [path]\tSets the path to the temporary directory, default is system's temp folder\n"
				+ "-verbosity [level]\tSets the verbosity level, 10 is max, default is 0\n"
				+ "-nocheckstyle\t\tdisables checkstyle, default is false\n"
				+ "-notestng\t\tdisables TestNG, default is false\n"
				+ "-jsonoutput\t\tWrites results to JSON, default is false\n"
				+ "-jsonfile [path]\tWrites results to JSON file\n"
				+ "-nomute\t\t\tWrites code output to stderr instead of discarding, default is false\n"
				+ "-checkstylexml [path]\tSets the path to checkstyle XML file\n"
				+ "-testngxml [path]\tSets the path to TestNG test configuration\n"
				+ "-javacoptions [options]\tPasses additional flags to the compiler; multiple flags should be\n"
				+ "separated with spaces and quoted, e.g -javacoptions \"-Xlint:cast -Xlint:deprecation\""
				+ "\nNotes:\n"
				+ "By default XML files are used from testRoot directory.\n"
				+ "For now, the paths must be absolute.\n";
	}

	/**
	 * Checks if the path is a valid file.
	 * @param path - pathname to the file
	 * @param shouldBeFolder - whether the path should be folder
	 * @return true if the path is ok
	 */
	private static boolean quickFileCheck(final String path, final boolean shouldBeFolder) {
		File f = new File(path);
		if (f.exists() && f.isDirectory()) {
			if (shouldBeFolder) {
				return true;
			}
			return false;
		}
		if (f.exists() && f.isFile()) {
			if (shouldBeFolder) {
				return false;
			}
			return true;
		}
		return false;
	}

	/**
	 * Runs the tester with current configuration.
	 */
	@SuppressWarnings("deprecation")
	public final void run() throws StudentTesterException {

		// allow only one instance per JVM
		if (StudentTesterMain.isRunning) {
			throw new StudentTesterException("Only one instance of StudentTester should be running at the same time!");
		}
		StudentTesterMain.isRunning = true;

		// start measuring time
		long startTime = System.nanoTime();
		log("Version: " + StudentHelperClass.getSelfVersion());

		// check if any necessary variables are missing
		if (StudentHelperClass.checkAnyNull(testRoot, testRootName, tempRoot,
				tempRootName, contentRoot, contentRootName)) {
			System.err.println("One or more necessary directories are missing");
			if (isJsonOutput) {
				System.out.print("{\"output\": \"Internal error, testing cannot continue.\"}");
			}
			System.exit(1);
		}

		// prepare json object if enabled, copy file contents to json
		if (isJsonOutput) {
			json = new JSONObject();
			singleResults = new JSONArray();

			// copy the content of all .java files to JSON
			JSONArray sourceList = new JSONArray();
			JSONArray testSourceList = new JSONArray();
			List<File> javaCodeFiles = new ArrayList<File>();
			List<File> javaTestFiles = new ArrayList<File>();
			StudentHelperClass.populateFiles(contentRoot, javaCodeFiles);
			StudentHelperClass.populateFiles(testRoot, javaTestFiles);
			try {
				for (File f: javaCodeFiles) {
					String content = new String(Files.readAllBytes(Paths.get(f.getAbsolutePath())), StandardCharsets.UTF_8);
					log("Adding file " + f.getName() + " to output");
					sourceList.put(new JSONObject()
							.put("path", f.getAbsolutePath())
							.put("content", content)
							.put("type", "code"));
				}
				for (File f: javaTestFiles) {
					String content = new String(Files.readAllBytes(Paths.get(f.getAbsolutePath())), StandardCharsets.UTF_8);
					log("Adding file " + f.getName() + " to output");
					testSourceList.put(new JSONObject()
							.put("path", f.getAbsolutePath())
							.put("content", content)
							.put("type", "test"));
				}
			} catch (FileNotFoundException e) {
				log(e.getMessage());
			} catch (IOException e) {
				log(e.getMessage());
			}

			json.put("source", sourceList);
			json.put("testSource", testSourceList);

			// TODO: redefine obscure fields
			json.put("extra", getCheckstyleXmlPath());

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
					TestNGRunner testng = new TestNGRunner(tempRoot, testRoot, contentRoot, isJsonOutput);
					if (isJsonOutput) {
						testng.setJsonVars(json, singleResults);
					}
					testng.setMuteCodeOutput(muteCodeOutput);
					testng.setTestNGXmlPathName(testNGXmlPathName);
					testng.run();
				}
			} catch (SecurityException e) {
				System.out.println("Testing was aborted due to an illegal statement. Remove the statement to continue.");
			} catch (NoClassDefFoundError e) {
				log(e.toString());
				System.out.println("Could not run one or more classes. "
						+ "Please check if the folder structure matches package definitions.");
			} catch (Exception e) {
				log(e.toString());
				e.printStackTrace();
				System.out.println("Internal error, cannot continue.");
			} finally {
				secInstance.restoreSecurityManager();
			}
		}

		if (!isTestNGEnabled && !checkstyleEnabled) {
			System.out.println("Nothing to run.");
		}

		StudentHelperClass.restoreStdOut();
		// print out json results
		if (isJsonOutput) {
			try {
				json.put("output", StudentHelperClass.getStdout().toString("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				log("UTF-8 decoding failed: " + e.getMessage());
				json.put("output", StudentHelperClass.getStdout().toString());
			}
			json.put("results", singleResults);
			if (!isQuiet) {
				if (outputFilename != null) {
					try (PrintWriter out = new PrintWriter(
							new OutputStreamWriter(new FileOutputStream(outputFilename), StandardCharsets.UTF_8))) {
						out.println(json.toString());
					} catch (FileNotFoundException e) {
						log(e.getMessage());
					}
				} else {
					System.out.println(json.toString());
				}
			}
		}
		StudentHelperClass.deleteFolder(tempRoot);

		// if any unit tests are still alive, kill them ungracefully to enable the program to exit
		Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
		for (Thread t : threadSet) {
			// the name might change!!!!
			if (t.getName().startsWith("TestNG")) {
				log(String.format("Warning: attempting to kill stuck thread %s, consider "
						+ "making the method exit on InterruptedException", t.getName()));
				t.stop();
			}
		}

		log("Finished. Run time in ms: " + (System.nanoTime() - startTime) / 1000000);
		StudentTesterMain.isRunning = false;
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
		StudentLogger.setVerbosity(verbosity);
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
			return json.toString();
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
	 * Gets the compiler options.
	 * @return information to be passed to the compiler
	 */
	public final String getCompilerOptions() {
		return this.compilerOptions;
	}

	/**
	 * Sets the compiler options.
	 * @param options - information to be passed to the compiler
	 */
	public final void setCompilerOptions(final String options) {
		this.compilerOptions = options;
	}

}
