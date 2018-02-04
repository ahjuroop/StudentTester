import ee.ttu.java.studenttester.classes.StudentTesterAPI;
import org.testng.annotations.Test;
import org.testng.Assert;
public class APILogTest {

	StudentTesterAPI api = StudentTesterAPI.getInstance(getClass());

	@Test
	public void testLog() {
		api.logMessagePrivate("Let's hope this isn't seen");
		APILog c = new APILog();
		Assert.assertEquals(c.onePlusOne(), 2);
		api.logMessagePublic("This is fine");
	}

}