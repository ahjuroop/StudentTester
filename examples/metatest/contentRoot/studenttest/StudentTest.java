package studenttest;

import static org.junit.Assert.*;

import org.junit.Test;

import studentcode.StudentCode;

public class StudentTest {

	// JUnit parameter order: assertEquals(expected, actual)
	
	@Test
	public void testAddNumbers() {
		assertEquals(5, StudentCode.addNumbers(2, 3));
	}

	@Test
	public void testAddNumbersBadly() {
		assertFalse(StudentCode.addNumbersNotSoWell(2, 3) == 5);
	}
	
	@Test(expected = NullPointerException.class)
	public void testCrashHorribly() {
		StudentCode.crashHorribly();
	}

	@Test(timeout = 500)
	public void testMaybeHangHorribly() {
		StudentCode.maybeHangHorribly();
	}
}
