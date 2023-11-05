package ast.typed.prog;

import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedImport;
import exceptions.compile_time.CompilationException;
import util.LateInit;

import java.util.List;

public record TypedFile(String name, List<TypedImport> imports, LateInit<List<TypeDef>, CompilationException> types, List<TypedExpr> code) {

}
