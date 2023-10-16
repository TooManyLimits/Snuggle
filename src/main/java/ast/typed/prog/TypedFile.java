package ast.typed.prog;

import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedImport;

import java.util.List;

public record TypedFile(String name, List<TypedImport> imports, List<TypedExpr> code) {
}
