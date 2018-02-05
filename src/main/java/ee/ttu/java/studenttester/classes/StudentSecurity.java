package ee.ttu.java.studenttester.classes;

import static ee.ttu.java.studenttester.enums.StudentPolicy.*;
import ee.ttu.java.studenttester.enums.StudentPolicy;
import ee.ttu.java.studenttester.interfaces.IStudentPolicy;

import java.security.Permission;
import java.util.*;

import static ee.ttu.java.studenttester.classes.StudentLogger.log;

public class StudentSecurity {

    private static final StudentSecurity instance = new StudentSecurity();
    /**
     * Stores the original security manager.
     */
    private static final SecurityManager defaultSecurityManager = System.getSecurityManager();
    /**
     * Stores classes that are subject to checks.
     */
    private static final Set<Class> classBlacklist = new HashSet<>();
    /**
     * Stores filenames that are "protected" (files with these names cannot be read or written to).
     */
    private static final Set<String> protectedFiles = new HashSet<>();
    /**
     * Restores the original security manager and clears all variables.
     */
    public void restoreSecurityManager() {
        log("Restoring original SecurityManager and clearing policies.");
        this.getClasses().clear();
        this.getCurrentPolicies().clear();
        this.getProtectedFiles().clear();
        System.setSecurityManager(defaultSecurityManager);
    }
    /**
     * Sets the new security manager.
     */
    public void setCustomSecurityManager() {
        log("Setting custom SecurityManager, enabled policies: " + policies);
        System.setSecurityManager(securityManager);
    }

    /**
     * Sets the default restrictions.
     */
    public void setDefaultRestrictions() {
        addPolicy(DISABLE_EXIT);
        addPolicy(DISABLE_ANY_FILE_MATCHER);
        addPolicy(DISABLE_SECURITYMANAGER_CHANGE);
        addPolicy(DISABLE_REFLECTION);
        addPolicy(DISABLE_EXECUTION);
        addPolicy(DISABLE_TEST_SNIFFING);
    }

    /**
     * The custom security manager.
     */
    private static final SecurityManager securityManager = new SecurityManager() {

        /**
         * Decides whether to allow an action or not based on the current active policies, blacklisted classes
         * and the current execution stack. If any of the blacklisted classes are present in the stack, it can be
         * presumed that the action originates from that class.
         * @param permission
         */
        public void checkPermission(final Permission permission) {
            List<Class> stack = Arrays.asList(getClassContext());
            // if no blacklisted classes are in the stack
            if (Collections.disjoint(stack, classBlacklist)) {
                // allow everything
                return;
            }
            // if testing with an empty object via API, throw an exception
            // is this safe?
            if (permission == null) {
                throw new SecurityException("Security check failed.");
            }
            // log(String.format("Attempting: %s",  permission.toString()));
            // else iterate over all active policies and call their respective methods
            for (IStudentPolicy policy : policies) {
                try {
                    policy.getConsumer().accept(permission);
                } catch (Exception e) {
                    // Illegal attempt caught, log an error or do smth
                    log(String.format("Illegal attempt caught: %s",  permission.toString()));
                    throw e;
                }

            }
        }
    };

    public void addPolicy(IStudentPolicy policy) {
        policies.add(policy);
    }
    public void removePolicy(IStudentPolicy policy) {
        policies.remove(policy);
    }
    public void addProtectedFile(String filename) {
        protectedFiles.add(filename);
    }
    public void removeProtectedFile(String filename) {
        protectedFiles.remove(filename);
    }
    public void addClass(Class clazz) {
        classBlacklist.add(clazz);
    }
    public void removeClass(Class clazz) {
        classBlacklist.remove(clazz);
    }
    public Set<IStudentPolicy> getCurrentPolicies() {
        return policies;
    }

    public Set<Class> getClasses() {
        return classBlacklist;
    }
    public Set<String> getProtectedFiles() {
        return protectedFiles;
    }
    /**
     * Holds all the functions responsible for checking various permissions.
     */
    private static Set<IStudentPolicy> policies = new HashSet<>();

    private StudentSecurity() {

    }

    /**
     * Returns the singleton of this class if the security manager has not been changed or the caller is allowed to
     * access this instance.
     * @return the singleton of this class, if the action was allowed
     */
    public static StudentSecurity getInstance() {
        // if the security manager has been changed, see if the caller is allowed to access it
        if (securityManager.equals(System.getSecurityManager())) {
            System.getSecurityManager().checkPermission(null);
        }
        return instance;
    }

}