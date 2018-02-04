import org.testng.annotations.Test;
import org.testng.Assert;
public class TrivialStudentTest {

	@Test
	public void testSanity() {
		TrivialStudent c = new TrivialStudent();
		Assert.assertEquals(c.onePlusOne(), 2);
	}

}