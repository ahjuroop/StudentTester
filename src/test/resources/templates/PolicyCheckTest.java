import org.testng.Assert;
import org.testng.annotations.Test;

public class PolicyCheckTest {

	PolicyCheck r = new PolicyCheck();

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
}