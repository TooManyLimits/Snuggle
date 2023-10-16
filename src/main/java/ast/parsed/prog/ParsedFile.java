package ast.parsed.prog;

import ast.parsed.def.type.ParsedTypeDef;
import ast.parsed.expr.ParsedExpr;
import ast.parsed.expr.ParsedImport;

import java.util.List;

/**
 * A single file in a program.
 * There is no analogous structure after we pass the Parsed phase. These are
 * removed during Type Resolution, since their main purpose is to group together
 * various different types and imports.
 */
public record ParsedFile(String name, List<ParsedImport> imports, List<ParsedTypeDef> typeDefs, List<ParsedExpr> code) {

}
