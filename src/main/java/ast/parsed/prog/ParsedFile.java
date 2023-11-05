package ast.parsed.prog;

import ast.parsed.def.type.ParsedTypeDef;
import ast.parsed.expr.ParsedExpr;
import ast.parsed.expr.ParsedImport;

import java.util.List;

/**
 * A single file in a program.
 */
public record ParsedFile(String name, List<ParsedImport> imports, List<ParsedTypeDef> typeDefs, List<ParsedExpr> code) {

}
