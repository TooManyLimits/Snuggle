package ast.parsed.def.type;

import exceptions.compile_time.CompilationException;
import ast.passes.TypeResolver;
import ast.type_resolved.def.type.BuiltinTypeResolvedTypeDef;
import ast.type_resolved.def.type.TypeResolvedTypeDef;
import builtin_types.BuiltinType;

public record BuiltinParsedTypeDef(BuiltinType builtin) implements ParsedTypeDef {

    @Override
    public String name() {
        return builtin.name();
    }

    @Override
    public boolean pub() {
        return true;
    }

    @Override
    public boolean nested() {
        return false;
    }

    @Override
    public TypeResolvedTypeDef resolve(TypeResolver resolver) throws CompilationException {
        return new BuiltinTypeResolvedTypeDef(builtin);
    }
}
