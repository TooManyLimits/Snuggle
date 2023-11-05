package builtin_types.snuggle;

import ast.parsed.prog.ParsedFile;
import ast.passes.Parser;
import exceptions.compile_time.CompilationException;
import lexing.Lexer;

//Types provided by the outer environment, but defined in Snuggle code.
public class SnuggleDefinedType {

    public final ParsedFile parsedFile;

    //Pass the source code for this type.
    //The source code should just define a single type, and nothing else.
    protected SnuggleDefinedType(String fileName, String src) {
        try {
            parsedFile = Parser.parseFile(fileName, new Lexer(fileName, src));
        } catch (CompilationException e) {
            throw new IllegalStateException("Bug in environment - snuggle defined builtin type failed to compile? Please report!", e);
        }
    }

}
