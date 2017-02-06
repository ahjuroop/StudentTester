package studentcode;

public class StudentTesterExample {
	
	private static int counter = 0;

	public StudentTesterExample() {
	}
	
	public int addNumbers(int a, int b) {
		return a + b;
	}
	
	public int addNumbersNotSoWell(int a, int b) {
		return a + b + 1;
	}
	
	public static void crashHorribly() {
		throw new NullPointerException("Oops!");
	}

	public static void hangHorribly() {
		for (int i = 0; i < 2; i++) {
			i--;
		}
	}
}