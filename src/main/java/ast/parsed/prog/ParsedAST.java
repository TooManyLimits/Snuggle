package ast.parsed.prog;

import java.util.List;

/**
 * Holds all the files; an entire program.
 */
public record ParsedAST(List<ParsedFile> files) {

}
