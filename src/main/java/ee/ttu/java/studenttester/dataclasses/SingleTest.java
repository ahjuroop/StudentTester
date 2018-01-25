package ee.ttu.java.studenttester.dataclasses;

/**
 * Data class for holding temporary single test data.
 * @author Andres
 *
 */
public class SingleTest implements Comparable<SingleTest> {

	private String name, errors, output;
	private int code;
	private double percent;

	/**
	 * Returns 
	 * @return
	 */
	public String getOutput() {
		return output;
	}

	public void setOutput(String output) {
		this.output = output;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getErrorOutput() {
		return errors;
	}

	public void setErrorOutput(String output) {
		this.errors = output;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public double getPercent() {
		return percent;
	}

	public void setPercent(double percent) {
		this.percent = percent;
	}

	/**
	 * Creates a single test.
	 * @param code - test code
	 * @param name - test name
	 * @param percent - test percentage
	 * @param errorOutput - error output
	 * @param studentOutput - test output
	 */
	public SingleTest(final int code, final String name, final double percent,
			final String errorOutput, String studentOutput) {
		this.code = code;
		this.name = name;
		this.percent = percent;
		this.errors = errorOutput;
		this.output = studentOutput;
	}

	@Override
	public int compareTo(final SingleTest other) {
		return this.getCode() - other.getCode();
	}
}
