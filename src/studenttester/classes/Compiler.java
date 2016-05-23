package studenttester.classes;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Contains methods to call the compiler API.
 * @author Andres
 *
 */
public class Compiler {

    private List<File> toBeCompiled;
    private File tempDirectory, testRoot;

    /**
     * Creates a new compiler object.
     * @param toBeCompiled - files to compile
     * @param tempDirectory - folder to be put into classpath after compilation
     * @param testRoot - folder containing tests
     */
    public Compiler(final List<File> toBeCompiled, final File tempDirectory, final File testRoot) {
        this.toBeCompiled = toBeCompiled;
        this.tempDirectory = tempDirectory;
        this.testRoot = testRoot;
    }

    /**
     * Tries to compile files given in the constructor.
     * @return true if compilation was done, false otherwise
     */
    public final boolean run() {
        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                throw new UnsupportedOperationException("Compiler is null, is jdk used?");
            }
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();

            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(toBeCompiled);
            compiler.getTask(null, fileManager, diagnostics, null, null, compilationUnits).call();
            fileManager.close();

            // workaround to put the temporary folder to classpath
            URL url = tempDirectory.toURI().toURL();
            URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            Class<?> urlClass = URLClassLoader.class;
            Method method = urlClass.getDeclaredMethod("addURL", new Class[] {URL.class});
            method.setAccessible(true);
            method.invoke(urlClassLoader, new Object[]{url});

            // compilation errors were found
            if (diagnostics.getDiagnostics().size() > 0) {
                System.out.println("Compilation failed, cannot continue.");
                handleCompilationErrors(diagnostics.getDiagnostics());
                return false;
            } else {
                // compilation succeeded
                return true;
            }
        } catch (UnsupportedOperationException e) {
            StudentHelperClass.restoreStdOut();
            System.out.println("Couldn't get the compiler, testing cannot continue.");
            StudentHelperClass.log(e.toString());
        } catch (Exception e) {
            StudentHelperClass.restoreStdOut();
            System.out.println("Testing failed, cannot continue.");
            StudentHelperClass.log(e.toString());
        }
        return false;
    }

    /**
     * Prints out a detailed report with compilation errors.
     * Not invoked if the code compiled successfully.
     * @param diagnostics - list of compilation errors
     */
    private void handleCompilationErrors(final List<Diagnostic<? extends JavaFileObject>> diagnostics) {

        List<String> testFileNames = new ArrayList<String>();
        StudentHelperClass.populateFilenames(testRoot, testFileNames);    // get all test filenames

        String previousError = null;    // store previous error code to hide consecutive errors
        int sameErrorCounter = 0;        // amount of skipped errors

        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {

            // skip consecutive identical errors to keep the output clean
            if (StudentHelperClass.getVerbosity() < 2 && diagnostic.getCode() != null && diagnostic.getCode().equals(previousError)) {
                sameErrorCounter++;
                continue;
            } else {
                if (sameErrorCounter > 0) {
                    System.out.println("Skipped " + sameErrorCounter + " error(s) of the same type.");
                    sameErrorCounter = 0;
                }
            }

            String problematicFile = new File(diagnostic.getSource().getName()).getName();

            // do not show code from test files
            if (testFileNames.contains(problematicFile)) {
                System.out.println("Error in " + problematicFile + ": " + diagnostic.getMessage(null));
            } else {
                System.out.format("Error on line %d in %s\n", diagnostic.getLineNumber(),
                        diagnostic.toString().replace(tempDirectory.getAbsolutePath(), ""));
            }

            previousError = diagnostic.getCode();

            // simple switch statement to provide hints to common compilation problems
            if (sameErrorCounter == 0 && diagnostic.getCode() != null) {
                switch (diagnostic.getCode()) {
                case "compiler.err.cant.resolve.location.args":
                    System.out.println("Hint: does the method exist?");
                    break;

                case "compiler.err.illegal.char":
                    System.out.println("Hint: there seems to be an encoding error.");
                    if (diagnostic.getLineNumber() == 1 && diagnostic.getPosition() == 1) {
                        System.out.println("The file likely contains a Byte Order Mark (BOM). Please remove it.");
                    }
                    break;

                    // etc...

                default:
                    if (StudentHelperClass.getVerbosity() > 1) {
                        StudentHelperClass.log("The error code is " + diagnostic.getCode());
                    }
                    break;
                }
            }
        }
        if (sameErrorCounter > 0) {
            System.out.println("Skipped " + sameErrorCounter + " error(s) of the same type.");
        }
    }
}
