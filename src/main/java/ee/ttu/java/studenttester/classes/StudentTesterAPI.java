package ee.ttu.java.studenttester.classes;

import ee.ttu.java.studenttester.interfaces.IStudentPolicy;

import java.util.*;

import static ee.ttu.java.studenttester.classes.StudentLogger.log;

/**
 * API for unit tests.
 */
public final class StudentTesterAPI {

    private static boolean apiEnabled = false;
    private static final Map<Class, StudentTesterAPI> apiObjects = new HashMap<>();
    private static final StudentSecurity secInstance = StudentSecurity.getInstance();
    private final Map<String, List<String>> privateMessages = new LinkedHashMap<String, List<String>>();
    private final Map<String, List<String>> publicMessages = new LinkedHashMap<String, List<String>>();

    private StudentTesterAPI() {}

    public static boolean hasInstance(Class clazz) {
        if (isApiDisabledPrintMsg()) return false;
        System.getSecurityManager().checkPermission(null);
        return apiObjects.containsKey(clazz);
    }

    /**
     * Gets a new API instance.
     * @param clazz the class to associate this API instance with
     * @return
     */
    public static StudentTesterAPI getInstance(Class clazz) {
        if (clazz == null) {
            throw new NullPointerException("The class must be defined");
        }
        // return mock API if it is inactive
        if (isApiDisabledPrintMsg()) return new StudentTesterAPI();
        // check for permission with an empty object,
        // SecurityManager must be configured to throw an exception in this case
        System.getSecurityManager().checkPermission(null);
        if (apiObjects.containsKey(clazz)) {
            return apiObjects.get(clazz);
        }
        StudentTesterAPI newInst = new StudentTesterAPI();
        apiObjects.put(clazz, newInst);
        return newInst;
    }

    /**
     * Adds a class from the blacklist, therefore subjecting it to various checks.
     * @param clazz the class to add
     */
    public void addClassToBlacklist(Class clazz) {
        if (isApiDisabledPrintMsg()) return;
        secInstance.addClass(clazz);
    }

    /**
     * Removes a class from the blacklist, therefore allowing it to do anything.
     * @param clazz the class to remove
     */
    public void removeClassFromBlacklist(Class clazz) {
        if (isApiDisabledPrintMsg()) return;
        secInstance.removeClass(clazz);
    }

    /**
     * Adds a policy, therefore forbidding actions defined by that policy.
     * @param policy
     */
    public void addSecurityPolicy(IStudentPolicy policy) {
        if (isApiDisabledPrintMsg()) return;
        secInstance.addPolicy(policy);
    }

    /**
     * Removes a policy, therefore allowing actions forbidden by that policy.
     * @param policy the policy to remove
     */
    public void removeSecurityPolicy(IStudentPolicy policy) {
        if (isApiDisabledPrintMsg()) return;
        secInstance.removePolicy(policy);
    }

    /**
     * Gets all currently active policies.
     * @return active policies
     */
    public Set<IStudentPolicy> getCurrentPolicies() {
        if (isApiDisabledPrintMsg()) return null;
        return secInstance.getCurrentPolicies();
    }

    /**
     * Gets all classes that are subject to additional checks.
     * @return restricted classes
     */
    public Set<Class> getClassBlacklist() {
        if (isApiDisabledPrintMsg()) return null;
        return secInstance.getClasses();
    }

    /**
     * Logs a message to be included in the JSON report and not displayed in the student output.
     * @param message message to be logged
     */
    public void logMessagePrivate(final String message) {
        if (isApiDisabledPrintMsg()) return;
        logMessage(message, privateMessages);
    }

    /**
     * Logs a message to be included in the JSON report and displayed in the student output.
     * @param message message to be logged
     */
    public void logMessagePublic(final String message) {
        if (isApiDisabledPrintMsg()) return;
        logMessage(message, publicMessages);
    }

    /**
     * Clears public and private messages.
     */
    public void clearMessages() {
        if (isApiDisabledPrintMsg()) return;
        publicMessages.clear();
        privateMessages.clear();
    }

    /**
     * Gets all private messages for this instance.
     * @return private messages
     */
    public Map<String, List<String>> getPrivateMessages() {
        if (isApiDisabledPrintMsg()) return null;
        return privateMessages;
    }

    /**
     * Gets all public messages for this instance.
     * @return public messages
     */
    public Map<String, List<String>> getPublicMessages() {
        if (isApiDisabledPrintMsg()) return null;
        return publicMessages;
    }

    /**
     * Returns whether the API is currently active or not. The API only works inside StudentTester.
     * When the API is not active, warning messages will be generated for any method.
     * @return
     */
    public static boolean isApiEnabled() {
        return apiEnabled;
    }

    /**
     * Sets the active state of the API.
     * @param apiEnabled API is functional
     */
    protected static void setApiEnabled(boolean apiEnabled) {
        StudentTesterAPI.apiEnabled = apiEnabled;
    }

    /**
     * Returns whether the API is currently disabled, also prints a warning message if that is the case.
     * @return API is not functional
     */
    private static boolean isApiDisabledPrintMsg() {
        if (!apiEnabled) {
            System.err.println("StudentTesterAPI: ignoring API command");
            return true;
        }
        return false;
    }

    /**
     * Logs a message to the given map.
     * @param message the value
     * @param destination the map where the value will be stored
     */
    private void logMessage(final String message, final Map<String, List<String>> destination) {
        String origin = new Throwable().getStackTrace()[2].getMethodName(); // potentially expensive operation
        log("Logging message from " + origin);
        // TODO: correct categories for inner and anonymous classes
        if (origin.matches("lambda\\$\\w+\\$\\d+")) {
            origin = origin.split("\\$")[1];
        } else if (origin.contains("$")) {
            log(String.format("Warning: logging messages from %s is unsupported.", origin));
        }
        if (!destination.containsKey(origin)) {
            destination.put(origin, new ArrayList<String>());
        }
        destination.get(origin).add(message);
    }
}
