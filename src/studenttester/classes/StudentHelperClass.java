package studenttester.classes;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.Permission;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Helper class for common functions.
 * @author Andres Antonen
 *
 */
public final class StudentHelperClass {

    /**
     * Global verbosity level. TestNG has a maximum value of 10.
     */
    private static int verbosity = 0;

    /**
     * Saves the original PrintStream to restore printing
     * functionality after redirecting the output to null.
     */
    private static PrintStream defaultPrintStream = System.out;

    /**
     * Stores the original error stream.
     */
    private static PrintStream defaultErrorPrintStream = System.err;

    /**
     * Stores the original security manager.
     */
    private static SecurityManager defaultSecurityManager = System.getSecurityManager();

    /**
     * Creates a PrintStream which does nothing when printed to.
     * Avoiding NUL or /dev/null to keep things cross-platfrom.
     */
    private static PrintStream nullPrintStream = new PrintStream(
            new OutputStream() {
                public void write(final int b) {
                }
            });

    /**
     * ByteArrayStream holding program output.
     */
    private static ByteArrayOutputStream stdoutStream;

    /**
     * PrintStream for redirecting output.
     */
    private static PrintStream ps;

    /**
     * Checks if any of the objects in the arguments are null.
     * @param objects - list of objects
     * @return true if null found
     */
    public static boolean checkAnyNull(final Object... objects) {
        for (Object o : objects) {
            if (o == null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Mutes the stdout stream.
     */
    public static void muteStdOut() {
        System.setOut(nullPrintStream);
    }

    /**
     * Redirects stdout to stderr.
     */
    public static void stdoutToErr() {
        System.setOut(defaultErrorPrintStream);
    }

    /**
     * Restores the stdout stream.
     */
    public static void restoreStdOut() {
        System.out.flush();
        System.setOut(defaultPrintStream);
    }

    /**
     * Redirects stdout to a variable.
     */
    public static void redirectStdOut() {
        if (stdoutStream == null) {
            stdoutStream = new ByteArrayOutputStream();
            ps = new PrintStream(stdoutStream);
        }
        System.setOut(ps);
    }

    /**
     * Returns a new ByteArrayOutputStream and redirects stdout to it.
     * @return new ByteArrayOutputStream object
     */
    public static ByteArrayOutputStream getNewStdoutObject() {
        ByteArrayOutputStream stdout2 = new ByteArrayOutputStream();
        PrintStream ps2 = new PrintStream(stdoutStream);
        System.setOut(ps2);
        return stdoutStream;
    }

    /**
     * Clears the redirected stream.
     */
    public static void clearRedirectedStdOut() {
        stdoutStream = new ByteArrayOutputStream();
        ps = new PrintStream(stdoutStream);
    }

    /**
     * Disables System.exit() by modifying the security manager.
     * Useful for libraries that try to shut down the VM on an error.
     */
    public static void disableSystemExit() {
        final SecurityManager securityManager = new SecurityManager() {
            public void checkPermission(final Permission permission) {
                if (permission.getName() != null && permission.getName().contains("exitVM")) {
                    throw new SecurityException("exitVM call caught.");
                }
            }
        };
        System.setSecurityManager(securityManager);
    }

    /**
     * Restores the original security manager, hence enabling System.exit().
     */
    public static void enableSystemExit() {
        System.setSecurityManager(defaultSecurityManager);
    }

    /**
     * Copies a directory tree to another directory.
     * @param src - source dir
     * @param dest - target dir
     * @throws IOException when creating file fails
     */
    public static void copyFolder(final File src, final File dest) throws IOException {
        if (src.isDirectory()) {
            if (!dest.exists()) {
                dest.mkdir();        // create folder if missing
                log("Copying from " + src + " to " + dest);
            }
            for (String file : src.list()) {    // for every filename/folder
                File srcFile = new File(src, file); // create source file
                File destFile = new File(dest, file); // and dest file
                copyFolder(srcFile, destFile); // and copy
            }
        } else {
            // copy file using stream
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            in.close();
            out.close();
            log("File copied from " + src + " to " + dest);
        }
    }

    /**
     * Deletes a folder recursively.
     * @param src folder to delete
     * @return success
     */
    public static boolean deleteFolder(final File src) {
        if (src.exists()) {
            File[] files = src.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        deleteFolder(f);
                    } else {
                        log("Deleting " + f.getAbsolutePath());
                        f.delete();
                    }
                }
            }
        }
        return src.delete();
    }

    /**
     * Executes a command and returns its stdout.
     * @param command - command to execute
     * @return stdout of the command
     */
/*
    public static String getOutputFromCommand(final String command) {
        final int TIMEOUT_AMOUNT = 15;
        Process proc;
        StringBuffer output = new StringBuffer();

        try {
            proc = Runtime.getRuntime().exec(command);
            proc.waitFor(TIMEOUT_AMOUNT, TimeUnit.SECONDS); // wait 15 secs at most before killing
            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line = "";
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
            }
        } catch (Exception e) {
            log(e.getMessage());
        }

        return output.toString();
    }
*/
    /**
     * Prints strings to standard error output if verbosity is more than 1.
     * @param string - message to print
     */
    public static void log(final String string) {
        if (verbosity > 1) {
            System.err.println("[DEBUG] " + string);
        }
    }

    /**
     * Returns a list of java filenames found in a folder.
     * @param src - source folder
     * @param testFileNames - java files
     */
    public static void populateFilenames(final File src, final List<String> testFileNames) {
        if (src.isDirectory()) {
            for (String file : src.list()) {
                File srcFile = new File(src, file);
                populateFilenames(srcFile, testFileNames);
            }
        } else {
            String fileName = src.getName().toString();
            if (fileName.endsWith(".java")) {
                testFileNames.add(fileName);
            }
        }
    }

    /**
     * Explores a folder and returns any java files found.
     * @param src - source dir
     * @param toBeCompiled - list of java files found
     */
    public static void populateFiles(final File src, final List<File> toBeCompiled) {
        if (src.isDirectory()) {
            for (String file : src.list()) {
                File srcFile = new File(src, file);
                populateFiles(srcFile, toBeCompiled);
            }
        } else {
            String fileName = src.getName().toString();
            if (fileName.endsWith(".java")) {
                log("Found java file " + src);
                toBeCompiled.add(src);
            }
        }
    }

    /**
     * Sets the verbosity.
     * @param verbosity level
     */
    public static void setVerbosity(final int verbosity) {
        StudentHelperClass.verbosity = verbosity;
    }

    /**
     * Returns the output stream dump.
     * @return ByteArrayOutputStream
     */
    public static ByteArrayOutputStream getStdout() {
        return stdoutStream;
    }

    /**
     * Gets the verbosity.
     * @return verbosity
     */
    public static int getVerbosity() {
        return verbosity;
    }

    /**
     * Private constructor.
     */
    private StudentHelperClass() {
    }
}