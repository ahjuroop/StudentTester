package ee.ttu.java.studenttester.enums;

/**
 * Stores possible unit test class types.
 * @author Andres
 */
public enum TestClassType {
	/**
	 * A TestNG test class.
	 */
	TESTNG,
	/**
	 * A JUnit test class.
	 */
	JUNIT,
	/**
	 * A class containing tests from both frameworks.
	 * It is better to throw an error than allow such
	 * practices.
	 */
	MIXED,
	/**
	 * No annotations for either frameworks found.
	 */
	NOT_TEST_CLASS
}
