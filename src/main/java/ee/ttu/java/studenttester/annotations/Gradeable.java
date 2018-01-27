package ee.ttu.java.studenttester.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for tests.
 * @author Andres
 *
 */
@Documented
@Target(ElementType.METHOD)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface Gradeable {
	/**
	 * Field for test weight.
	 * @return default weight 1
	 */
	int weight() default 1;
	/**
	 * Field for test description.
	 * @return default description
	 */
	String description() default "No description.";
	/**
	 * Determines whether detailed exception message
	 * will be printed.
	 * @return default false
	 */
	boolean printExceptionMessage() default false;
	/**
	 * Determines whether stack trace
	 * will be printed.
	 * @return default false
	 */
	boolean printStackTrace() default false;
}