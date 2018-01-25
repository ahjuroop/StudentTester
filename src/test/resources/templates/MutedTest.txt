import org.testng.annotations.Test;
import org.testng.Assert;
import ee.ttu.java.studenttester.annotations.TestContextConfiguration;
import ee.ttu.java.studenttester.enums.ReportMode;

@TestContextConfiguration(mode = ReportMode.MUTED)
public class MutedTest {

	@Test
	public void testSanity() {
		Muted c = new Muted();
		Assert.assertEquals(c.onePlusOne(), 2);
	}

}