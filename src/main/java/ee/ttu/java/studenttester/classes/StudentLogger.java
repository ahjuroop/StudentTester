package ee.ttu.java.studenttester.classes;

public final class StudentLogger {

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
		StudentLogger.verbosity = verbosity;
	}
}
