package ee.ttu.java.studenttester.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import ee.ttu.java.studenttester.enums.ReportMode;

/**
 * Annotation for defining some global class settings.
 * @author Andres
 *
 */
@Documented
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface TestContextConfiguration {
	/**
	 * DummyTest class verbosity level.
	 * @return verbosity level
	 */
	ReportMode mode() default ReportMode.NORMAL;
	/**
	 * Text to be displayed before tests.
	 * @return message
	 */
	String welcomeMessage() default "";
	/**
	 * A number that identifies this test and
	 * should be unique and non-negative.
	 * Default value is -1.
	 * @return indentifier number
	 */
	int identifier() default -1;
}
