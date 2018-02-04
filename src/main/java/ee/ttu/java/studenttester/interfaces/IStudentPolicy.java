package ee.ttu.java.studenttester.interfaces;

import java.security.Permission;
import java.util.function.Consumer;

/**
 * Interface for creating custom policies.
 */
public interface IStudentPolicy {
    /**
     * Gets a method that evaluates whether the permission object should be allowed or not.
     * @return consumer that accepts a permission object
     */
    Consumer<Permission> getConsumer();
}
