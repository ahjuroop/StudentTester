package ee.ttu.java.studenttester.enums;

import ee.ttu.java.studenttester.classes.StudentSecurity;
import ee.ttu.java.studenttester.interfaces.IStudentPolicy;
import net.sf.saxon.type.Converter;

import java.lang.reflect.ReflectPermission;
import java.security.Permission;
import java.util.function.Consumer;

public enum StudentPolicy implements IStudentPolicy {
    /**
     * Disable System.exit().
     */
    DISABLE_EXIT(StudentPolicy::disableExit),
    /**
     * Disable <<ALL FILES>>-type file access.
     */
    DISABLE_ANY_FILE_MATCHER(StudentPolicy::disableAnyFileMatcher),
    /**
     * Disable System.setSecurityManager().
     */
    DISABLE_SECURITYMANAGER_CHANGE(StudentPolicy::disableSecurityManagerChange),
    /**
     * Disable reflection.
     */
    DISABLE_REFLECTION(StudentPolicy::disableReflection),
    /**
     * Disable invocation of external commands.
     */
    DISABLE_EXECUTION(StudentPolicy::disableExec),
    /**
     * Disable read/write access to test files.
     */
    DISABLE_TEST_SNIFFING(StudentPolicy::disableTestSniffing),
    /**
     * Disable Internet access.
     */
    DISABLE_SOCKETS(StudentPolicy::disableSockets);

    private final Consumer<Permission> permissionConsumer;
    private static final StudentSecurity secInstance = StudentSecurity.getInstance();
    StudentPolicy(Consumer<Permission> permissionConsumer) {
        this.permissionConsumer = permissionConsumer;
    }

    public Consumer<Permission> getConsumer() {
        return this.permissionConsumer;
    }

    /**
     * Checks if the permission is about exiting the VM.
     * @param p - permission to check
     */
    private static void disableExit(Permission p) {
        if (p.getName() != null && p.getName().contains("exitVM")) {
            throw new SecurityException("Illegal attempt to exit the JVM.");
        }
    }

    /**
     * Checks if the permission is about accessing any file. Good for disabling commands without an explicit path but not
     * much else.
     * @param p - permission to check
     */
    private static void disableAnyFileMatcher(Permission p) {
        if (p.getName().contains("<<ALL FILES>>")) {
            throw new SecurityException("Illegal attempt to access the file system.");
        }
    }

    /**
     * Checks if the permission is about changing the security manager.
     * @param p - permission to check
     */
    private static void disableSecurityManagerChange(Permission p) {
        if (p.getName().contains("setSecurityManager")) {
            throw new SecurityException("Illegal attempt to modify the security manager.");
        }
    }

    /**
     * Checks if the permission is about reflection.
     * @param p - permission to check
     */
    private static void disableReflection(Permission p) {
        if (p instanceof ReflectPermission) {
            throw new SecurityException("Illegal attempt to use reflection.");
        }
    }

    /**
     * Checks if the permission has "execute" flag set.
     * @param p - permission to check
     */
    private static void disableExec(Permission p) {
        if (p.getActions().contains("execute")) {
            throw new SecurityException(String.format("Illegal attempt to execute a resource: %s", p.getName()));
        }
    }

    /**
     * Checks if the permission attempts to access any protected file.
     * @param p - permission to check
     */
    private static void disableTestSniffing(Permission p) {
        if (secInstance.getProtectedFiles().stream().anyMatch(p.getName()::contains)) {
            throw new SecurityException(String.format("Illegal attempt to access resource: %s", p.getName()));
        }
    }

    /**
     * Checks if the permission is about network sockets.
     * @param p - permission to check
     */
    private static void disableSockets(Permission p) {
        // TODO
    }
}
