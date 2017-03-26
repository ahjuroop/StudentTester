package ee.ttu.java.studenttester.enums;

/**
 * Global verbosity settings.
 * @author Andres
 *
 */
public enum ReportMode {
	/**
	 * Reporter defaults.
	 */
	NORMAL,
	/**
	 * Print exception messages.
	 */
	VERBOSE,
	/**
	 * Print exception messages and stack trace.
	 */
	MAXVERBOSE,
	/**
	 * Show only grade.
	 */
	ANONYMOUS,
	/**
	 * Only acknowledge the test was run.
	 */
	MUTED;
}
