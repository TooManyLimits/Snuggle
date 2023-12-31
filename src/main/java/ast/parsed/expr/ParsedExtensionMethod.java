package ast.parsed.expr;

import ast.parsed.ParsedType;
import ast.parsed.def.method.SnuggleParsedMethodDef;
import ast.passes.TypeResolver;
import ast.type_resolved.def.method.SnuggleTypeResolvedMethodDef;
import ast.type_resolved.expr.TypeResolvedExpr;
import ast.type_resolved.expr.TypeResolvedExtensionMethod;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import util.LateInitFunction;

//distinguished by its first parameter being named "this"
//fn name(this: receiverType, other params): returnType body
public record ParsedExtensionMethod(Loc loc, SnuggleParsedMethodDef methodDef,
                                    LateInitFunction<TypeResolver, TypeResolvedExtensionMethod, CompilationException> resolveCache) implements ParsedExpr {

    public ParsedExtensionMethod(Loc loc, SnuggleParsedMethodDef methodDef) {
        this(loc, methodDef, new LateInitFunction<>(resolver -> new TypeResolvedExtensionMethod(loc, methodDef.resolve(resolver))));
    }

    @Override
    public TypeResolvedExtensionMethod resolve(TypeResolver resolver) throws CompilationException {
        return resolveCache.get(resolver);
    }

}
