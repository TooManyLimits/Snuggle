package ast.parsed.prog;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds all the files; an entire program.
 */
public record ParsedAST(ArrayList<ParsedFile> files) {

}
