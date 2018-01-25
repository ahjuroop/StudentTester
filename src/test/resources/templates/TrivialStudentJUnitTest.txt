import org.junit.Test;
import static org.junit.Assert.*;
public class TrivialStudentJUnitTest {

	@Test
	public void testSanity() {
		TrivialStudentJUnit c = new TrivialStudentJUnit();
		assertEquals(c.onePlusOne(), 2);
	}

}