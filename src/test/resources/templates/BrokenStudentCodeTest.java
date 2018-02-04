import org.testng.annotations.Test;
import org.testng.Assert;
public class BrokenStudentCodeTest {

	@Test
	public void testSanity() {
		BrokenStudentCode c = new BrokenStudentCode();
		Assert.assertEquals(c.onePlusOne(), 2);
	}

}