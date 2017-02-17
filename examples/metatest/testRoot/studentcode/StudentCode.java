package studentcode;


public class StudentCode {
	
	public static enum metaTestType {	INITIAL,
										ADD_NUMBERS_IS_WRONG,
										ADD_NUMBERS_NOT_SO_WELL_IS_RIGHT,
										NO_CRASH,
										HANG
									};
	public static metaTestType currentMetaTest = metaTestType.INITIAL;

	public static int addNumbers(int a, int b) {
		if (currentMetaTest == metaTestType.ADD_NUMBERS_IS_WRONG) {
			return addNumbersNotSoWell(a, b);
		}
		return a + b;
	}
	
	public static int addNumbersNotSoWell(int a, int b) {
		if (currentMetaTest == metaTestType.ADD_NUMBERS_NOT_SO_WELL_IS_RIGHT) {
			return addNumbers(a, b);
		}
		return a + b + 1;
	}
	
	public static void crashHorribly() {
		if (currentMetaTest == metaTestType.NO_CRASH) {
			return;
		}
		throw new NullPointerException("Oops!");
	}

	public static void maybeHangHorribly() {
		if (currentMetaTest == metaTestType.HANG) {
			for (int i = 0; i < 2; i++) {
				i--;
			}
		}
		return;
	}
}
