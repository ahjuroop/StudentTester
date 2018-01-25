public class TwoExceptions {

	public int getException1() {
		throw new NullPointerException();
	}
	
	public int getException2() {
		throw new IllegalArgumentException();
	}
	
}