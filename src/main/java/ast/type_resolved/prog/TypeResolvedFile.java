package ast.type_resolved.prog;

import ast.passes.TypeChecker;
import ast.type_resolved.ResolvedType;
import ast.type_resolved.def.method.TypeResolvedMethodDef;
import ast.type_resolved.expr.TypeResolvedExpr;
import ast.type_resolved.expr.TypeResolvedExtensionMethod;
import ast.typed.prog.TypedFile;
import exceptions.compile_time.CompilationException;
import util.LateInit;
import util.ListUtils;

import java.util.List;

public record TypeResolvedFile(String name, List<ResolvedType.Basic> topLevelTypes, List<TypeResolvedExtensionMethod> topLevelExtensionMethods, List<TypeResolvedExpr> code) {

    public TypedFile type(TypeChecker checker) throws CompilationException {
        checker.importExtensionMethods(name, true, null);
        return new TypedFile(
                name,
//                ListUtils.map(imports, i -> i.infer(null, checker, List.of(), null)),
                new LateInit<>(() -> ListUtils.flatten(ListUtils.map(topLevelTypes, checker::getAllInstantiated))),
                ListUtils.map(code, e -> e.infer(null, checker, List.of(), List.of(), null))
        );
    }

}
