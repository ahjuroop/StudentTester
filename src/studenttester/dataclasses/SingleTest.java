package studenttester.dataclasses;
/**
 * Data class for holding temporary single test data.
 * @author Andres
 *
 */
public class SingleTest {

    private String name;
    private int code;
    private double percent;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
     */
    public SingleTest(int code, String name, double percent) {
        this.code = code;
        this.name = name;
        this.percent = percent;
    }
}