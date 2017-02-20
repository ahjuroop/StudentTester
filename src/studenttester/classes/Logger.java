package studenttester.classes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class Logger {

	private static Map<String, List<String>> privateMessages = new LinkedHashMap<String, List<String>>();
	private static Map<String, List<String>> publicMessages = new LinkedHashMap<String, List<String>>();

	/**
	 * Global verbosity level. TestNG has a maximum value of 10.
	 */
	private static int verbosity = 0;

	/**
	 * Prints strings to standard error output if verbosity is more than 1.
	 * @param string - message to print
	 */
	public static void log(final String string) {
		try {
			if (verbosity > 1) {
				System.err.format("[StudentTester@%s] %s\n",
						new Throwable().getStackTrace()[1].getMethodName(), string);
			}
		} catch (Exception e) {
			System.err.format("[StudentTester@%s] %s\n", "null", string);
		}
	}

	/**
	 * Logs a message to the given map.
	 * @param message - the value
	 * @param destination - the map where the value will be stored
	 */
	private static void logMessage(final String message, final Map<String, List<String>> destination) {
		String origin = new Throwable().getStackTrace()[2].getMethodName(); // potentially expensive operation
		log("Logging message from " + origin);
		System.err.println();
		if (!destination.containsKey(origin)) {
			destination.put(origin, new ArrayList<String>());
		}
		destination.get(origin).add(message);
	}

	/**
	 * Logs a message to be included in the JSON report and not displayed in the student output.
	 * Note that the command is fairly expensive and may impact execution time.
	 * @param message - message to be logged
	 */
	public static void logMessagePrivate(final String message) {
		logMessage(message, privateMessages);
	}

	/**
	 * Logs a message to be included in the JSON report and displayed in the student output.
	 * Note that the command is fairly expensive and may impact execution time.
	 * @param message - message to be logged
	 */
	public static void logMessagePublic(final String message) {
		logMessage(message, publicMessages);
	}

	/**
	 * Clears public and private messages.
	 */
	public static void clearMessages() {
		publicMessages.clear();
		privateMessages.clear();
	}

	/**
	 * Gets all private messages.
	 * @return private messages
	 */
	public static Map<String, List<String>> getPrivateMessages() {
		return privateMessages;
	}

	/**
	 * Gets all public messages.
	 * @return public messages
	 */
	public static Map<String, List<String>> getPublicMessages() {
		return publicMessages;
	}

	/**
	 * Gets the verbosity.
	 * @return verbosity
	 */
	public static int getVerbosity() {
		return verbosity;
	}

	/**
	 * Sets the verbosity.
	 * @param verbosity level
	 */
	public static void setVerbosity(final int verbosity) {
		Logger.verbosity = verbosity;
	}
}
