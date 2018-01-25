import org.testng.annotations.Test;

public class TwoExceptionsTest {

	@Test(expectedExceptions = NullPointerException.class)
	public void testExceptionSuccess() {
		TwoExceptions c = new TwoExceptions();
		c.getException1();
	}
	
	@Test(expectedExceptions = NullPointerException.class)
	public void testExceptionFail() {
		TwoExceptions c = new TwoExceptions();
		c.getException2();
	}

}