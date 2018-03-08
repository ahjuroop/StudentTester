import ee.ttu.java.studenttester.classes.StudentTesterAPI;
import ee.ttu.java.studenttester.enums.StudentPolicy;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.MalformedURLException;

public class PolicyCheckTest {

	PolicyCheck r = new PolicyCheck();
	StudentTesterAPI api = StudentTesterAPI.getInstance(getClass());

	@Test(expectedExceptions = SecurityException.class)
	public void testWhoami() {
		r.whoami();
	}
	@Test(expectedExceptions = SecurityException.class)
	public void testExit() {
		r.exit();
	}
	@Test
	public void testValidAction() {
		r.innocent();
	}
	@Test
	public void testValidWriteRead() {
		String result = r.writeAndReadInnocent("lolwat");
		Assert.assertEquals(result, "lolwat");
	}
	@Test(expectedExceptions = SecurityException.class)
	public void testProcWithPath() {
		r.procWithPath();
	}
	@Test(expectedExceptions = SecurityException.class)
	public void testSniffFile() {
		r.sniffFile();
	}
	@Test(expectedExceptions = SecurityException.class)
	public void testHijackApi() {
		r.hijackApi();
	}
	@Test(expectedExceptions = SecurityException.class)
	public void testHijackSecurityManager() {
		r.hijackSecurityManager();
	}
	@Test
	public void testOpenSocket() throws Exception {
		// default implementation should succeed
		// also fails if no Internet connection
		r.openSocket();
	}
	@Test(expectedExceptions = SecurityException.class)
	public void testOpenSocketBad() throws Exception {
		// this implementation should not succeed
		api.addSecurityPolicy(StudentPolicy.DISABLE_SOCKETS);
		r.openSocket();
	}
}