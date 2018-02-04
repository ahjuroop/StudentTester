package ee.ttu.java.studenttester.classes;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import static ee.ttu.java.studenttester.classes.StudentLogger.log;

public class CompilerErrorHints {

    /**
     * Analyzes a diagnostics object and provides a hint, where applicable.
     * @param diagnostic the diagnostics object to analyze
     * @return string containing hints or an empty string
     */
    public static String getDiagnosticStr(Diagnostic<? extends JavaFileObject> diagnostic) {

        String str = "";
        switch (diagnostic.getCode()) {
            case "compiler.err.cant.resolve.location.args":
                str += "Hint: does the method exist?\n";
                break;

            case "compiler.err.illegal.char":
                str += "Hint: there seems to be an encoding error.\n";
                if (diagnostic.getLineNumber() == 1 && diagnostic.getPosition() == 1) {
                    str += "The file likely contains a Byte Order Mark (BOM). Please remove it.\n";
                }
                break;

            case "compiler.err.cant.resolve.location":
                str += "Hint: have you declared all necessary variables/types?\n";
                break;

            case "compiler.err.prob.found.req":
                str += "Hint: casting one type to another might help.\n";
                break;

            case "compiler.err.unreachable.stmt":
                str += "Hint: remove either the statement causing the code to be unreachable or the code itself.\n";
                break;
            case "compiler.err.class.public.should.be.in.file":
                str += "Hint: is the file name same as the class defined in it?\n";
                break;

            case "compiler.err.unreported.exception.need.to.catch.or.throw":
                str += "Hint: handle the exception inside the function or "
                        + "include \"throws <exception type>\" in the function's declaration.\n";
                break;

            case "compiler.err.not.stmt":
                str += "Hint: this might be a typo.\n";
                break;

            case "compiler.err.expected":
                str += "Hint: did you miss a name or character?\n";
                break;

            case "compiler.err.invalid.meth.decl.ret.type.req":
                str += "Hint: you must specify what the function returns.\n";
                break;

            case "compiler.err.missing.ret.stmt":
                // str += "Hint: the function expects to return something.\n";
                break;

            case "compiler.err.premature.eof":
                str += "Hint: part of the file might be missing.\n";
                break;

            case "compiler.err.void.not.allowed.here":
                str += "Hint: is the function 'void' but actually should return something?\n";
                break;

            default:
                if (StudentLogger.getVerbosity() > 1) {
                    log("The error code is " + diagnostic.getCode());
                }
                break;
        }
        return str;
    }
}
