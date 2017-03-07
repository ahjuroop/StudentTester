package studenttester.classes;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.security.Permission;
import java.util.List;

import studenttester.enums.TestClassType;

import static studenttester.classes.Logger.log;

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
	 * Stores the original security manager.
	 */
	private static SecurityManager defaultSecurityManager = System.getSecurityManager();

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
	 * Exception message that SecurityException will use and StudentReporter must recognize.
	 */
	public static final String EXITVM_MSG = "exitVM call caught";

	/**
	 * Stores possible unit test class types.
	 * @author Andres
	 *
	 */
	public enum TEST_CLASS_TYPE { TESTNG, JUNIT, NOT_TEST_CLASS };

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
	 * Disables System.exit() by modifying the security manager.
	 * Useful for libraries that try to shut down the VM on an error.
	 */
	public static void disableSystemExit() {
		final SecurityManager securityManager = new SecurityManager() {
			public void checkPermission(final Permission permission) {
				if (permission.getName() != null && permission.getName().contains("exitVM")) {
					throw new SecurityException(EXITVM_MSG);
				}
			}
		};
		System.setSecurityManager(securityManager);
	}

	/**
	 * Restores the original security manager, hence enabling System.exit().
	 */
	public static void enableSystemExit() {
		System.setSecurityManager(defaultSecurityManager);
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
		Logger.log("Deleting " + src.getAbsolutePath());
		if (src.exists()) {
			File[] files = src.listFiles();
			if (files != null) {
				for (File f : files) {
					if (f.isDirectory()) {
						deleteFolder(f);
					} else {
						if (!f.delete()) {
							Logger.log("Failed to delete " + f.getAbsolutePath());
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
		System.err.println(breakOn);
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
	 * @param includePaths
	 */
	private static void populateFilenames(final File src, final List<String> filenames,
			final String root, final boolean includePaths) {
		if (src.isDirectory()) {
			for (String file : src.list()) {
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
	public static void populateFiles(final File src, final List<File> toBeCompiled) {
		if (src.isDirectory()) {
			for (String file : src.list()) {
				File srcFile = new File(src, file);
				populateFiles(srcFile, toBeCompiled);
			}
		} else {
			String fileName = src.getName().toString();
			if (fileName.endsWith(".java")) {
				log("Found java file " + src);
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
		return classPath.replace(".", File.separator) + ". java";
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
	 * Quick test to find out whether the file is a JUnit test or a TestNG test.
	 * The file must already be compiled and in the classpath.
	 * @param testClassPath relative filepath to the class, e.g. "mypackage/Test.java"
	 * @return the type of class.
	 * @throws ClassNotFoundException when something goes wrong
	 */
	protected static TestClassType getClassType(final String testClassPath) throws ClassNotFoundException {
		Class<?> classToTest = Class.forName(filePathToClassPath(testClassPath));
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
