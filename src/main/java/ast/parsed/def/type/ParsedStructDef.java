package ast.parsed.def.type;

import ast.parsed.ParsedType;
import ast.parsed.def.field.SnuggleParsedFieldDef;
import ast.parsed.def.method.SnuggleParsedMethodDef;
import ast.passes.TypeResolver;
import ast.type_resolved.def.type.TypeResolvedStructDef;
import ast.type_resolved.def.type.TypeResolvedTypeDef;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import util.ListUtils;

import java.util.List;

public record ParsedStructDef(Loc loc, boolean pub, String name, int numGenerics, boolean nested, List<SnuggleParsedMethodDef> methods, List<SnuggleParsedFieldDef> fields) implements ParsedTypeDef {

    @Override
    public TypeResolvedTypeDef resolve(TypeResolver resolver) throws CompilationException {
        return new TypeResolvedStructDef(
                loc,
                name,
                numGenerics,
                nested,
                ListUtils.map(methods, m -> m.resolve(resolver)),
                ListUtils.map(fields, f -> f.resolve(resolver))
        );
    }
}
