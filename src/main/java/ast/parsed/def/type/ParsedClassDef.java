package ast.parsed.def.type;

import ast.parsed.def.field.SnuggleParsedFieldDef;
import exceptions.CompilationException;
import ast.parsed.ParsedType;
import ast.parsed.def.method.SnuggleParsedMethodDef;
import ast.passes.TypeResolver;
import ast.type_resolved.ResolvedType;
import ast.type_resolved.def.type.TypeResolvedClassDef;
import ast.type_resolved.def.type.TypeResolvedTypeDef;
import lexing.Loc;
import util.ListUtils;

import java.util.List;

/**
 * Tracks the file methodName because files no longer exist after Type Resolution.
 * After Type Resolution completes, file names are only useful for error
 * messages.
 */
public record ParsedClassDef(Loc loc, boolean pub, String name, int numGenerics, ParsedType.Basic supertype, List<SnuggleParsedMethodDef> methods, List<SnuggleParsedFieldDef> fields) implements ParsedTypeDef {

    @Override
    public TypeResolvedTypeDef resolve(TypeResolver resolver) throws CompilationException {
        return new TypeResolvedClassDef(
                loc,
                name,
                numGenerics,
                supertype == null ? null : (ResolvedType.Basic) supertype.resolve(loc, resolver),
                ListUtils.map(methods, m -> m.resolve(resolver)),
                ListUtils.map(fields, f -> f.resolve(resolver))
        );
    }

}
