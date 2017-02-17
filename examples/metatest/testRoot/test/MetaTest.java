package test;

import java.lang.reflect.Method;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import studentcode.StudentCode;
import studentcode.StudentCode.metaTestType;
import studenttest.StudentTest;

public class MetaTest {
	
	static Class<?> testClass = StudentTest.class;
	static MiniStudentTester tester;
	
	@Test
	static void metaTest1() {
		StudentCode.currentMetaTest = metaTestType.INITIAL;
		Assert.assertTrue(tester.run());
		System.err.format("Failed methods were: %s\n", tester.getFailedTests());
	}
	
	@Test
	static void metaTest2() {
		StudentCode.currentMetaTest = metaTestType.HANG;
		Assert.assertFalse(tester.run());
		System.err.format("Failed methods were: %s\n", tester.getFailedTests());
	}
	
	@Test
	static void metaTest3() {
		StudentCode.currentMetaTest = metaTestType.ADD_NUMBERS_NOT_SO_WELL_IS_RIGHT;
		Assert.assertFalse(tester.run());
		System.err.format("Failed methods were: %s\n", tester.getFailedTests());
	}
	
	@Test(dependsOnMethods={"metaTest1", "metaTest2", "metaTest3"})
	static void metaTest4() {
		StudentCode.currentMetaTest = metaTestType.INITIAL;
		Assert.assertTrue(tester.run());
		System.err.format("Failed methods were: %s\n", tester.getFailedTests());
	}
	
	@BeforeMethod
	static void initTest(Method method) {
		tester = new MiniStudentTester(method.getName() + " (inner TestNG)", testClass);
	}

}
