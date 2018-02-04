package ee.ttu.java.studenttester.classes;
import ee.ttu.java.studenttester.exceptions.StudentTesterException;

import static ee.ttu.java.studenttester.classes.StudentLogger.log;

import java.io.File;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Contains methods to call the compiler API.
 * @author Andres
 *
 */
public class CompilerRunner {

	private List<String> toBeCompiled = new ArrayList<String>();
	private boolean compileSeparately = false;
	private File tempDirectory, testRoot;
	private List<String> options = new ArrayList<String>();
	private StandardJavaFileManager fileManager;
	private JavaCompiler compiler;
	private Writer compilerWriter;

	/**
	 * Creates a new compiler object.
	 * @param toBeCompiledRelative - names of files to compiled, must exist in tempDirectory and be given in relative paths
	 * @param tempDirectory - folder to be put into classpath after compilation
	 * @param testRoot - folder containing tests
	 */
	public CompilerRunner(final List<String> toBeCompiledRelative, final File tempDirectory, final File testRoot) {
		// convert relative paths to absolute ones for the compiler
		toBeCompiledRelative.forEach((name) -> this.toBeCompiled.add(new File(tempDirectory, name).getAbsolutePath()));
		this.tempDirectory = tempDirectory;
		this.testRoot = testRoot;
		// use utf8 encoding when compiling, pointer to source directory
		this.options.addAll(Arrays.asList("-encoding", "utf8", "-sourcepath", tempDirectory.getAbsolutePath()));
	}

	/**
	 * Adds additional javac options.
	 * @param compilerOptions - options as string
	 */
	public void addOptions(final String compilerOptions) {
		if (compilerOptions != null) {
			options.addAll(Arrays.asList(compilerOptions.split(" ")));
		}
	}

	/**
	 * Sets whether the compiler should compile files separately to skip classes having errors.
	 * @param separate - set true to compile independent files separately (default false)
	 */
	public void compileSeparately(final boolean separate) {
		this.compileSeparately = separate;
	}

	/**
	 * Tries to compile files given in the constructor.
	 * @return true if compilation was done, false otherwise
	 */
	public final boolean run() {

		try {
			compiler = ToolProvider.getSystemJavaCompiler();
			if (compiler == null) {
				throw new StudentTesterException("The platform did not provide the necessary compiler needed"
						+ " to run this tool. Please check the availability of JDK.");
			}
			if (toBeCompiled.size() == 0) {
				throw new StudentTesterException("Nothing to compile.");
			}

			fileManager = compiler.getStandardFileManager(null, null, null);
			DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
			compilerWriter = new StringWriter(); // compilation output
			log("Beginning compilation, " + toBeCompiled.size() + " files without dependencies in queue");
			if (options != null && options.size() > 0) {
				log("Compiler options: " + options);
			} else {
				log("No compiler options specified");
			}

			boolean atLeastOneSucess = false;
			if (compileSeparately) {
				for (String filename : toBeCompiled) {
					List<String> temp = new ArrayList<String>();
					temp.add(filename);
					if (compile(temp, diagnostics)) {
						atLeastOneSucess = true;
					}
				}
			} else {
				atLeastOneSucess = compile(toBeCompiled, diagnostics);
			}

			/* As of Java 9, the following method does not work anymore.
			if (atLeastOneSucess) {
				// workaround to put the temporary folder to classpath
				URL url = tempDirectory.toURI().toURL();
				URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
				Class<?> urlClass = URLClassLoader.class;
				Method method = urlClass.getDeclaredMethod("addURL", new Class[] {URL.class});
				method.setAccessible(true);
				method.invoke(urlClassLoader, new Object[]{url});
			}
			*/

			fileManager.close();

			// compilation errors were found
			if (atLeastOneSucess && diagnostics.getDiagnostics().size() > 0) {
				System.out.println("Compilation succeeded partially.");
				handleCompilationErrors(diagnostics.getDiagnostics());
				return true;
			} else if (atLeastOneSucess) {
				// compilation succeeded
				System.out.println("Compilation succeeded.\n");
				return true;
			} else {
				System.out.println("Compilation failed.");
				if (diagnostics.getDiagnostics().size() > 0) {
					handleCompilationErrors(diagnostics.getDiagnostics());
				}
				return false;
			}

		} catch (UnsupportedOperationException e) {
			System.out.println("Couldn't get the compiler, testing cannot continue.");
			log(e.toString());
		} catch (Exception e) {
			log(e.toString());
			e.printStackTrace();
		} finally {
			log(compilerWriter.toString());
		}
		System.out.println("Compilation failed.");
		return false;
	}

	/**
	 * Compiles files given.
	 * @param filenames list of files to be compiled
	 * @param diagnostics object to store diagnostics in
	 * @return success
	 */
	private boolean compile(final List<String> filenames, DiagnosticCollector<JavaFileObject> diagnostics) {
		Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromStrings(filenames);
		boolean compileSuccess = compiler.getTask(compilerWriter, null, diagnostics, options, null, compilationUnits).call();
		log((compileSuccess? "Compilation appears to have succeeded for " : "Compilation failed for ") + filenames);
		return compileSuccess;
	}

	/**
	 * Prints out a detailed report with compilation errors.
	 * Not invoked if the code compiled successfully.
	 * @param diagnostics - list of compilation errors
	 */
	private void handleCompilationErrors(final List<Diagnostic<? extends JavaFileObject>> diagnostics) {

		List<String> testFileNames = new ArrayList<String>();
		StudentHelperClass.populateFilenames(testRoot, testFileNames, false);    // get all test filenames

		// String previousError = null;    // store previous error code to hide consecutive errors
		// int sameErrorCounter = 0;       // amount of skipped errors
		boolean errorsSkipped = false;     // there are skipped errors
		// replace counting the same error with only one message per error type.
		// will look nicer than 100 alternating errors.
		List<String> pastErrors = new ArrayList<String>();

		for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {

			if (pastErrors.contains(diagnostic.getCode())) {
				log(String.format("Skipping already existing error %s at line %d.\n",
						diagnostic.getCode(), diagnostic.getLineNumber()));
				errorsSkipped = true;
				continue;
			}

			pastErrors.add(diagnostic.getCode());

			// skip consecutive identical errors to keep the output cleaner
			/* if (diagnostic.getCode() != null && diagnostic.getCode().equals(previousError)) {
				sameErrorCounter++;
				continue;
			} else {
				if (sameErrorCounter > 0) {
					System.out.println("Skipped " + sameErrorCounter + " error(s) of the same type.");
					sameErrorCounter = 0;
				}
			}

			previousError = diagnostic.getCode();
			*/

			String problematicFile = new File(diagnostic.getSource().getName()).getName();

			// do not show code from test files
			if (testFileNames.contains(problematicFile)) {
				log(problematicFile + " is a test class, will not display the full error.");
				log(String.format("Error on line %d in %s\n", diagnostic.getLineNumber(),
						diagnostic.toString()));
				System.out.format("Error on line %d in %s: %s\n",
						diagnostic.getLineNumber(), problematicFile, diagnostic.getMessage(null));
			} else {
				System.out.format("Error on line %d in %s\n", diagnostic.getLineNumber(),
						diagnostic.toString().replace(tempDirectory.getAbsolutePath(), ""));
			}

			if (/* sameErrorCounter == 0 && */ diagnostic.getCode() != null) {
				System.out.print(CompilerErrorHints.getDiagnosticStr(diagnostic));
			}
		}
		if (errorsSkipped) {
			System.out.println("Skipped some errors of the same type. "
					+ "Try fixing the ones mentioned above first and check if the problem persists.");
		}
		/*
		if (sameErrorCounter > 0) {
			System.out.println("Skipped " + sameErrorCounter + " error(s) of the same type.");
		}
		*/
		System.out.println();
	}
}
