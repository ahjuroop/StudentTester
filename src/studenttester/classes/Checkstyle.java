package studenttester.classes;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonArrayBuilder;

/**
 * Contains methods to call Checkstyle.
 * @author Andres
 *
 */
public class Checkstyle {

    private String checkStyleXmlPath;
    private File contentRoot;
    private boolean jsonOutput;
    private JsonArrayBuilder singleResults;

    /**
     * Default field name in json results.
     */
    private final String JSON_FIELD_NAME = "Stylecheck_1";
    /**
     * Default code in json results.
     */
    private final int JSON_CODE = 101;

    /**
     * Creates a Checkstyle object.
     * @param checkStyleXmlPath - pathname to xml
     * @param contentRoot - path to content
     * @param jsonOutput - put items to json
     * @param singleResults - object where to put the results
     */
    public Checkstyle(String checkStyleXmlPath, File contentRoot, boolean jsonOutput, JsonArrayBuilder singleResults) {
        this.checkStyleXmlPath = checkStyleXmlPath;
        this.contentRoot = contentRoot;
        this.jsonOutput = jsonOutput;
        this.singleResults = singleResults;
    }

    /**
     * Executes the checkstyle jar and collects its results.
     */
    public final void run() {
        // save the the original System.out, whatever it is right now
        PrintStream original = System.out;
        // save original err as well to suppress Checkstyle errors
        PrintStream originalErr = System.err;
        // disable error stream temporarily if verbosity 0
        if (StudentHelperClass.getVerbosity() == 0) {
            System.setErr(null);
        }
        // capture checkstyle output to a variable
        ByteArrayOutputStream temp = StudentHelperClass.getNewStdoutObject();
        // disable System.exit() since Checkstyle likes to terminate the VM
        StudentHelperClass.disableSystemExit();

        try {
            System.out.println("Running Checkstyle...");
            com.puppycrawl.tools.checkstyle.Main.main("-c", checkStyleXmlPath, contentRoot.getAbsolutePath());
        } catch (IOException e1) {
            StudentHelperClass.log(e1.getMessage());
        } catch (SecurityException e) {
            // checkstyle exit caught
            StudentHelperClass.log(e.getMessage());
        } finally {
            StudentHelperClass.enableSystemExit();
        }
        String checkstyleResult = temp.toString();
        // restore streams
        System.setOut(original);
        System.setErr(originalErr);
        int checkstyleErrors = 0;

        // try to convert absolute paths to relative ones
        // to save space and conceal the system path
        checkstyleResult = checkstyleResult.replace(contentRoot.getAbsolutePath(), "");

        // extract error count from the result
        Pattern ERROR_PATTERN = Pattern.compile("Checkstyle ends with (\\d+) errors.");
        Matcher m = ERROR_PATTERN.matcher(checkstyleResult);

        try {
            while (m.find()) {
                checkstyleErrors = Integer.parseInt(m.group(1));
            }
        } catch (Exception e) {
            StudentHelperClass.log(e.getMessage());
        }

        // add data to json if needed
        if (jsonOutput) {
            singleResults.add(Json.createObjectBuilder()
                    .add("percent", checkstyleErrors == 0? 100 : 0)
                    .add("code", JSON_CODE)
                    .add("name", JSON_FIELD_NAME));
        }
        
        System.out.print(checkstyleResult);
    }
}
