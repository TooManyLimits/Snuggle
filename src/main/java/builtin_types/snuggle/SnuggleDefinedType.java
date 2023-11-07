package builtin_types.snuggle;

import ast.parsed.prog.ParsedFile;
import ast.passes.Parser;
import exceptions.compile_time.CompilationException;
import lexing.Lexer;

//Types provided by the outer environment, but defined in Snuggle code.
public class SnuggleDefinedType {

    public final ParsedFile parsedFile;

    //If "needsImport", then the parsed file will run when imported.
    //Otherwise, add the parsedFile's typedefs to every scope.
    public final boolean needsImport;

    //Pass the source code for this type.
    //The source code should just define a single type, and nothing else.
    protected SnuggleDefinedType(String fileName, boolean needsImport, String src) {
        this.needsImport = needsImport;
        try {
            parsedFile = Parser.parseFile(fileName, new Lexer(fileName, src));
        } catch (CompilationException e) {
            throw new IllegalStateException("Bug in environment - snuggle defined builtin type failed to compile? Please report!", e);
        }
    }

}
