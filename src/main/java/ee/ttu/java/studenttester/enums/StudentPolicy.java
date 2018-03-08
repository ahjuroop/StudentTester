package ee.ttu.java.studenttester.enums;

import ee.ttu.java.studenttester.classes.StudentSecurity;
import ee.ttu.java.studenttester.interfaces.IStudentPolicy;

import java.lang.reflect.ReflectPermission;
import java.net.SocketPermission;
import java.security.Permission;
import java.util.List;
import java.util.function.BiConsumer;

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
     * Entrirely disable reflection. Breaks many things such as sockets.
     */
    DISABLE_REFLECTION_STRICT(StudentPolicy::disableReflectionStrict),
    /**
     * Disable reflection invoked directly from a blacklisted class.
     * TODO: find possible ways to perform arbitrary reflection
     */
    DISABLE_REFLECTION_SHALLOW(StudentPolicy::disableReflectionShallow),
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

    private final BiConsumer<Permission, List<Class>> permissionConsumer;
    private static final StudentSecurity secInstance = StudentSecurity.getInstance();
    StudentPolicy(BiConsumer<Permission, List<Class>> permissionConsumer) {
        this.permissionConsumer = permissionConsumer;
    }

    public BiConsumer<Permission, List<Class>> getConsumer() {
        return this.permissionConsumer;
    }

    /**
     * Checks if the permission is about exiting the VM.
     * @param p - permission to check
     */
    private static void disableExit(Permission p, List<Class> stack) {
        if (p.getName() != null && p.getName().contains("exitVM")) {
            throw new SecurityException("Illegal attempt to exit the JVM.");
        }
    }

    /**
     * Checks if the permission is about accessing any file. Good for disabling commands without an explicit path but not
     * much else.
     * @param p - permission to check
     */
    private static void disableAnyFileMatcher(Permission p, List<Class> stack) {
        if (p.getName().contains("<<ALL FILES>>")) {
            throw new SecurityException("Illegal attempt to access the file system.");
        }
    }

    /**
     * Checks if the permission is about changing the security manager.
     * @param p - permission to check
     */
    private static void disableSecurityManagerChange(Permission p, List<Class> stack) {
        if (p.getName().contains("setSecurityManager")) {
            throw new SecurityException("Illegal attempt to modify the security manager.");
        }
    }

    /**
     * Checks if the permission is about reflection.
     * @param p - permission to check
     */
    private static void disableReflectionStrict(Permission p, List<Class> stack) {
        if (p instanceof ReflectPermission) {
            throw new SecurityException("Illegal attempt to use reflection.");
        }
    }

    /**
     * Checks if the permission is about reflection and is invoked directly by a blacklisted class.
     * @param p - permission to check
     */
    private static void disableReflectionShallow(Permission p, List<Class> stack) {
        // only check the head of the stack
        if (p instanceof ReflectPermission && secInstance.getClasses().contains(stack.get(0))) {
            throw new SecurityException("Illegal attempt to use reflection.");
        }
    }

    /**
     * Checks if the permission has "execute" flag set.
     * @param p - permission to check
     */
    private static void disableExec(Permission p, List<Class> stack) {
        if (p.getActions().contains("execute")) {
            throw new SecurityException(String.format("Illegal attempt to execute a resource: %s", p.getName()));
        }
    }

    /**
     * Checks if the permission attempts to access any protected file.
     * @param p - permission to check
     */
    private static void disableTestSniffing(Permission p, List<Class> stack) {
        if (secInstance.getProtectedFiles().stream().anyMatch(p.getName()::contains)) {
            throw new SecurityException(String.format("Illegal attempt to access resource: %s", p.getName()));
        }
    }

    /**
     * Checks if the permission is about network sockets.
     * @param p - permission to check
     */
    private static void disableSockets(Permission p, List<Class> stack) {
        if (p instanceof SocketPermission) {
            throw new SecurityException(String.format("Illegal attempt to open a network socket: %s", p.getName()));
        }
    }
}
