package ee.ttu.java.studenttester.dataclasses;
import java.util.ArrayList;
import java.util.List;

/**
 * Data class for storing temporary test data.
 * @author Andres
 *
 */
public class TestResults {

	public String getOutput() {
		return output;
	}

	public void setStudentOutput(String output) {
		this.output = output;
	}

	public double getPercent() {
		return percent;
	}

	public void setPercent(double percent) {
		this.percent = percent;
	}

	public List<SingleTest> getResultList() {
		return resultList;
	}

	private String output;
	private double percent;
	private List<SingleTest> resultList;

	public TestResults() {
		resultList = new ArrayList<SingleTest>();
	}

	/**
	 * Adds a new single test.
	 * @param code - test number
	 * @param name - test name
	 * @param percentage - test percentage
	 */
	public final void addTest(final int code, final String name, final double percentage) {
		resultList.add(new SingleTest(code, name, percentage, null, null));
	}

	/**
	 * Adds a diagnostic test with output.
	 * @param code - test number
	 * @param name - test name
	 * @param percentage - test percentage
	 * @param errorOutput - diagnostic output
	 */
	public final void addTest(final int code, final String name,
			final double percentage, final String errorOutput, final String studentOutput) {
		resultList.add(new SingleTest(code, name, percentage, errorOutput, studentOutput));
	}

	/**
	 * Adds a diagnostic test with output, percentage will be NaN.
	 * @param code - test number
	 * @param name - test name
	 * @param output - diagnostic output
	 */
	public final void addTest(final int code, final String name,
			final String errorOutput, final String studentOutput) {
		resultList.add(new SingleTest(code, name, Double.NaN, errorOutput, studentOutput));
	}

}
