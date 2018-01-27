package testcode;

import org.testng.Assert;
import org.testng.annotations.Test;

import studentcode.StudentTesterExample;
import ee.ttu.java.studenttester.annotations.Gradeable;
import ee.ttu.java.studenttester.annotations.TestContextConfiguration;
import ee.ttu.java.studenttester.enums.ReportMode;

@TestContextConfiguration(mode = ReportMode.VERBOSE, welcomeMessage = "This file for demonstration purposes only.")
public class TestNGTest1 {
	
	StudentTesterExample se = new StudentTesterExample();
	
	// TestNG parameter order: assertEquals(actual, expected)
	
	@Test
	public void testSucceedNoAnnotation() {
		Assert.assertEquals(se.addNumbers(2, 3), 5);
	}

	@Test
	public void testFailNoAnnotation() {
		Assert.assertEquals(se.addNumbersNotSoWell(2, 3), 5);
	}
	
	@Test
	public void testCrashHorribly() {
		StudentTesterExample.crashHorribly();
	}

	@Gradeable(printExceptionMessage = true)
	@Test(timeOut = 500)
	public void testHangHorribly() {
		StudentTesterExample.hangHorribly();
	}

	@Gradeable(description = "This is an annotated TestNG test. I should be visible in the report.")
	@Test
	public void testSucceedAnnotation() {
		Assert.assertEquals(se.addNumbers(2, 3), 5);
	}

	@Gradeable(weight = 50)
	@Test
	public void testSucceedWeightedAnnotation() {
		Assert.assertEquals(se.addNumbers(2, 3), 5);
	}
	
	@Gradeable(printStackTrace = true, weight = 40)
	@Test
	public void testCrashHorriblyWithStacktrace() {
		StudentTesterExample.crashHorribly();
	}
}
