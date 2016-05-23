package studenttester.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import studenttester.enums.ReportMode;

/**
 * Annotation for defining some global class settings.
 * @author Andres
 *
 */
@Documented
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface GlobalConfiguration {
    /**
     * Test class verbosity level.
     * @return verbosity level
     */
    ReportMode mode() default ReportMode.NORMAL;
    /**
     * Text to be displayed before tests.
     * @return message
     */
    String welcomeMessage() default "";
}
