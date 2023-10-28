package ast.type_resolved.prog;

import exceptions.compile_time.CompilationException;
import ast.passes.TypeChecker;
import ast.type_resolved.expr.TypeResolvedExpr;
import ast.type_resolved.expr.TypeResolvedImport;
import ast.typed.expr.TypedImport;
import ast.typed.prog.TypedFile;
import util.ListUtils;

import java.util.List;

public record TypeResolvedFile(String name, List<TypeResolvedExpr> code) {

    public TypedFile type(TypeChecker checker) throws CompilationException {
        return new TypedFile(
                name,
                ListUtils.map(code, e -> e.infer(null, checker, List.of()))
        );
    }

}
