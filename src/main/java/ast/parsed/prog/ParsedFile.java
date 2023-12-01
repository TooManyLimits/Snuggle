package ast.parsed.prog;

import ast.parsed.def.method.SnuggleParsedMethodDef;
import ast.parsed.def.type.ParsedTypeDef;
import ast.parsed.expr.ParsedExpr;
import ast.parsed.expr.ParsedExtensionMethod;
import ast.parsed.expr.ParsedImport;

import java.util.List;

/**
 * A single file in a program.
 */
public record ParsedFile(String name, List<ParsedImport> topLevelImports, List<ParsedTypeDef> topLevelTypeDefs, List<ParsedExtensionMethod> topLevelExtensionMethods, List<ParsedExpr> code) {

}
