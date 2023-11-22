package ast.parsed.expr;

import ast.parsed.def.method.SnuggleParsedMethodDef;
import ast.passes.TypeResolver;
import ast.type_resolved.expr.TypeResolvedExpr;
import exceptions.compile_time.CompilationException;
import lexing.Loc;

//fn type.name(args): returnType body
public record ParsedExtensionMethod(Loc loc, boolean pub, String name, SnuggleParsedMethodDef methodDef) implements ParsedExpr {

    @Override
    public TypeResolvedExpr resolve(TypeResolver resolver) throws CompilationException {
        throw new IllegalStateException("Extension functions not yet implemented");
    }

}
