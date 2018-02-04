package ee.ttu.java.studenttester.classes;
import static ee.ttu.java.studenttester.classes.StudentLogger.log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import ee.ttu.java.studenttester.enums.TestClassType;
import ee.ttu.java.studenttester.exceptions.StudentTesterException;

/**
 * Helper class for common functions.
 * @author Andres Antonen
 *
 */
public final class StudentHelperClass {

	/**
	 * Saves the original PrintStream to restore printing
	 * functionality after redirecting the output to null.
	 */
	private static PrintStream defaultPrintStream = System.out;

	/**
	 * Stores the original error stream.
	 */
	private static PrintStream defaultErrorPrintStream = System.err;


	/**
	 * Creates a PrintStream which does nothing when printed to.
	 * Avoiding NUL or /dev/null to keep things cross-platfrom.
	 */
	private static PrintStream nullPrintStream = new PrintStream(
			new OutputStream() {
				public void write(final int b) {
				}
			});

	/**
	 * ByteArrayStream holding program output.
	 */
	private static ByteArrayOutputStream stdoutStream;

	/**
	 * PrintStream for redirecting output.
	 */
	private static PrintStream ps;

	/**
	 * Checks if any of the objects in the arguments are null.
	 * @param objects - list of objects
	 * @return true if null found
	 */
	public static boolean checkAnyNull(final Object... objects) {
		for (Object o : objects) {
			if (o == null) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Mutes the stdout stream.
	 */
	public static void muteStdOut() {
		System.setOut(nullPrintStream);
	}

	/**
	 * Redirects stdout to stderr.
	 */
	public static void stdoutToErr() {
		System.setOut(defaultErrorPrintStream);
	}

	/**
	 * Restores the stdout stream.
	 */
	public static void restoreStdOut() {
		System.out.flush();
		System.setOut(defaultPrintStream);
	}

	/**
	 * Redirects stdout to a variable.
	 */
	public static void redirectStdOut() {
		if (stdoutStream == null) {
			stdoutStream = new ByteArrayOutputStream();
			ps = new PrintStream(stdoutStream);
		}
		System.setOut(ps);
	}

	/**
	 * Returns a new ByteArrayOutputStream and redirects stdout to it.
	 * @return new ByteArrayOutputStream object
	 */
	public static ByteArrayOutputStream getNewStdoutObject() {
		ByteArrayOutputStream stdout2 = new ByteArrayOutputStream();
		PrintStream ps2 = new PrintStream(stdout2);
		System.setOut(ps2);
		return stdout2;
	}

	/**
	 * Clears the redirected stream.
	 */
	public static void clearRedirectedStdOut() {
		stdoutStream = new ByteArrayOutputStream();
		ps = new PrintStream(stdoutStream);
	}

	/**
	 * Gets the JAR version of StudentTester.
	 * From http://stackoverflow.com/a/1273432
	 * @return version of jar if it could be found
	 */
	public static String getSelfVersion() {
		try {
			Class<?> clazz = StudentHelperClass.class;
			String className = clazz.getSimpleName() + ".class";
			String classPath = clazz.getResource(className).toString();
			if (!classPath.startsWith("jar")) {
			  throw new StudentTesterException("Class not from JAR");
			}
			String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + 
			    "/META-INF/MANIFEST.MF";
			Manifest manifest = new Manifest(new URL(manifestPath).openStream());
			Attributes attr = manifest.getMainAttributes();
			return attr.getValue("Implementation-Version");
		} catch (Exception e) {
			StudentLogger.log(e.toString());
			StudentLogger.log("Does the attribute Implementation-Version exist?");
		}
		return "unknown";
	}

	/**
	 * Copies a directory tree to another directory.
	 * @param src - source dir
	 * @param dest - target dir
	 * @throws IOException when creating file fails
	 */
	public static void copyFolder(final File src, final File dest) throws IOException {
		if (src.isDirectory()) {
			if (!dest.exists()) {
				dest.mkdir();        // create folder if missing
				log("Copying from " + src + " to " + dest);
			}
			for (String file : src.list()) {    // for every filename/folder
				File srcFile = new File(src, file); // create source file
				File destFile = new File(dest, file); // and dest file
				copyFolder(srcFile, destFile); // and copy
			}
		} else {
			// copy file using stream
			InputStream in = new FileInputStream(src);
			OutputStream out = new FileOutputStream(dest);
			byte[] buffer = new byte[1024];
			int length;
			while ((length = in.read(buffer)) > 0) {
				out.write(buffer, 0, length);
			}
			in.close();
			out.close();
			// log("File copied from " + src + " to " + dest);
		}
	}

	/**
	 * Deletes a folder recursively.
	 * @param src folder to delete
	 * @return success
	 */
	public static boolean deleteFolder(final File src) {
		StudentLogger.log("Deleting " + src.getAbsolutePath());
		if (src.exists()) {
			File[] files = src.listFiles();
			if (files != null) {
				for (File f : files) {
					if (f.isDirectory()) {
						deleteFolder(f);
					} else {
						if (!f.delete()) {
							StudentLogger.log("Failed to delete " + f.getAbsolutePath());
						}
					}
				}
			}
		}
		return src.delete();
	}

	/**
	 * Builds a stack trace string that breaks on the first occurrence
	 * of a method name.
	 * @param throwable object to analyze
	 * @param breakOn method name to search for and break on.
	 * @return stack trace string
	 */
	public static String getStackTraceString(final Throwable throwable, final String breakOn) {
		StackTraceElement[] stack = throwable.getStackTrace();
		String stackTraceString = throwable.getClass().getName() + "\n";
		for (int i = 0; i < stack.length; i++) {
			stackTraceString += String.format("\t - at %s\n", stack[i].toString());
			if (stack[i].getMethodName().equals(breakOn)) {
				stackTraceString += String.format("\t ... %d more\n", stack.length - i);
				break;
			}
		}
		return stackTraceString;
	}

	/**
	 * Searches for a class in the stack.
	 * @param throwable object to analyze
	 * @param className class name to search for and break on.
	 * @return class is found in the stack
	 */
	public static boolean throwableIsFromClass(final Throwable throwable, final String className) {
		StackTraceElement[] stack = throwable.getStackTrace();
		for (int i = 0; i < stack.length; i++) {
			// System.err.println(stack[i].getClassName());
			if (stack[i].getClassName().equals(className)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Searches for a classes in the stack.
	 * @param throwable object to analyze
	 * @param classNames class names to search for and break on.
	 * @return class is found in the stack
	 */
	public static boolean throwableIsFromClasses(final Throwable throwable, final List<String> classNames) {
		StackTraceElement[] stack = throwable.getStackTrace();
		for (int i = 0; i < stack.length; i++) {
			// System.err.println(stack[i].getClassName());
			if (classNames.contains(stack[i].getClassName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns a list of java filenames found in a folder.
	 * @param src - source folder
	 * @param filenames - java files
	 * @param includePaths - include relative paths to files
	 */
	public static void populateFilenames(final File src, final List<String> filenames,
			final boolean includePaths) {
		// Actually just a wrapper for the real function.
		// We need to remember the original path so we can cut it out
		populateFilenames(src, filenames, src.getAbsolutePath() + File.separator, includePaths);
	}

	/**
	 * Returns a list of java filenames found in a folder.
	 * @param src - source folder
	 * @param filenames - java files
	 * @param root - root path string
	 * @param includePaths - include relative path in name
	 */
	private static void populateFilenames(final File src, final List<String> filenames,
			final String root, final boolean includePaths) {
		if (src.isDirectory()) {
			String[] files = src.list();
			Arrays.sort(files); // sort files to fix test order (somewhat)
			for (String file : files) {
				File srcFile = new File(src, file);
				populateFilenames(srcFile, filenames, root, includePaths);
			}
		} else {
			String filename;
			if (includePaths) {
				filename = src.getAbsolutePath().replace(root, "");
			} else {
				filename = src.getName();
			}
			if (filename.endsWith(".java")) {
				filenames.add(filename);
			}
		}
	}

	/**
	 * Explores a folder and returns any java files found.
	 * @param src - source dir
	 * @param toBeCompiled - list of java files found
	 */
	protected static void populateFiles(final File src, final List<File> toBeCompiled) {
		if (src.isDirectory()) {
			String[] files = src.list();
			Arrays.sort(files);
			for (String file : files) {
				File srcFile = new File(src, file);
				populateFiles(srcFile, toBeCompiled);
			}
		} else {
			String fileName = src.getName().toString();
			if (fileName.endsWith(".java")) {
				// StudentLogger.log("Found java file " + src);
				toBeCompiled.add(src);
			}
		}
	}

	/**
	 * Converts a classpath to relative file path.
	 * @param classPath - classpath (some.path.File)
	 * @return relative path (some/path/file.java)
	 */
	public static String classPathToFilePath(final String classPath) {
		return classPath.replace(".", File.separator) + ".java";
	}

	/**
	 * Converts a relative file path to classpath.
	 * The path should not start with a separator.
	 * @param filePath - relative path (some/path/file.java)
	 * @return classpath (some.path.File)
	 */
	public static String filePathToClassPath(final String filePath) {
		return filePath.replace(".java", "").replace(File.separator, ".");
	}

	/**
	 * Quick test to find out whether the class is a JUnit test or a TestNG test.
	 * The class must already be compiled and in the classpath.
	 * @param testClassPath relative filepath to the class, e.g. "mypackage/DummyTest.java"
	 * @return the type of class.
	 * @throws ClassNotFoundException when something goes wrong
	 */
	protected static TestClassType getClassType(final String testClassPath) throws ClassNotFoundException {
		Class<?> classToTest = Class.forName(filePathToClassPath(testClassPath));
		return getClassType(classToTest);
	}
	/**
	 * Quick test to find out whether the class is a JUnit test or a TestNG test.
	 * @param classToTest the class to test
	 * @return the type of class.
	 * @throws ClassNotFoundException when something goes wrong
	 */
	protected static TestClassType getClassType(final Class<?> classToTest) {
		boolean testNGfound = false, junitFound = false;
		// if the class contains at least one
		for (Method unitTest : classToTest.getDeclaredMethods()) {
			// JUnit method, assume it's a JUnit test
			if (unitTest.isAnnotationPresent(org.junit.Test.class)) {
				junitFound = true;
				// TestNG method, assume it's a TestNG test
			} else if (unitTest.isAnnotationPresent(org.testng.annotations.Test.class)) {
				testNGfound = true;
			}
		}
		if (junitFound && testNGfound) {
			return TestClassType.MIXED;
		} else if (junitFound) {
			return TestClassType.JUNIT;
		} else if (testNGfound) {
			return TestClassType.TESTNG;
		} else {
			return TestClassType.NOT_TEST_CLASS;
		}
	}
	/**
	 * Returns the output stream dump.
	 * @return ByteArrayOutputStream
	 */
	public static ByteArrayOutputStream getStdout() {
		return stdoutStream;
	}

	/**
	 * Private constructor.
	 */
	private StudentHelperClass() {
	}

}
