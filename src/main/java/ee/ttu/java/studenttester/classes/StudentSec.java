package ee.ttu.java.studenttester.classes;

import java.security.Permission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class StudentSec {
    /**
     * Stores the original security manager.
     */
    private static final SecurityManager defaultSecurityManager = System.getSecurityManager();
    /**
     * Restores the original security manager.
     */
    public static void restoreSecurityManager() {
        System.setSecurityManager(defaultSecurityManager);
    }
    /**
     * Sets the new security manager.
     */
    public static void setCustomSecurityManager() {
        permissionFuncs.add(StudentSec::disableExit);
        permissionFuncs.add(StudentSec::disableAnyFileMatcher);
        // permissionFuncs.add(StudentSec::disableSecurityManagerChange); TODO: must differentiate between tester and code
        System.setSecurityManager(securityManager);
    }
    /**
     * Exception message for System.exit().
     */
    public static final String EXITVM_MSG = "exitVM call caught";
    /**
     * The custom security manager.
     */
    private static final SecurityManager securityManager = new SecurityManager() {
        public void checkPermission(final Permission permission) {
            //System.err.println(String.format("%s : %s", permission.getName(), permission.getActions()));
            for (Consumer<Permission> consumer : permissionFuncs) {
                consumer.accept(permission);
            }
        }
    };

    public static void addRule(Consumer<Permission> rule) {
        permissionFuncs.add(rule);
    }
    public static void removeRule(Consumer<Permission> rule) {
        permissionFuncs.remove(rule);
    }
    public Set<Consumer<Permission>> getCurrentRules() {
        return permissionFuncs;
    }
    /**
     * Holds all the functions responsible for checking various permissions.
     */
    private static Set<Consumer<Permission>> permissionFuncs = new HashSet<>();

    private StudentSec() {

    }

    /**
     * Checks if the permission is about exiting the VM.
     * @param p - permission to check
     */
    public static void disableExit(Permission p) {
        if (p.getName() != null && p.getName().contains("exitVM")) {
            throw new SecurityException(EXITVM_MSG);
        }
    }

    /**
     * Checks if the permission is about accessing any file. Good for disabling commands without explicit path but not
     * much else.
     * @param p - permission to check
     */
    public static void disableAnyFileMatcher(Permission p) {
        if (p.getName().contains("<<ALL FILES>>")) {
            throw new SecurityException();
        }
    }

    /**
     * Checks if the permission is about changing the security manager.
     * @param p - permission to check
     */
    public static void disableSecurityManagerChange(Permission p) {
        if (p.getName().contains("setSecurityManager")) {
            throw new SecurityException();
        }
    }
}