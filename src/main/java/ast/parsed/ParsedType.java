package ast.parsed;

import ast.passes.TypeResolver;
import ast.type_resolved.ResolvedType;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.UnknownTypeException;
import lexing.Loc;
import util.ListUtils;

import java.util.List;

/**
 * Only contains PARSED information. This does NOT refer to an actual annotatedType!
 * Only the *methodName* of a annotatedType! This is *very important* and is a distinction
 * that we didn't properly respect in a previous attempt of this project.
 */
public sealed interface ParsedType {

    ResolvedType resolve(Loc loc, TypeResolver resolver)throws CompilationException;

    record Basic(String name, List<ParsedType> generics) implements ParsedType {
        @Override
        public ResolvedType resolve(Loc loc, TypeResolver resolver) throws CompilationException {
            //Basic types; lookup their methodName
            Integer resolvedName = resolver.lookup(name);
            if (resolvedName == null)
                throw new UnknownTypeException("Type \"" + this + "\" could not be found in the current scope.", loc);
            return new ResolvedType.Basic(resolvedName, ListUtils.map(generics, g -> g.resolve(loc, resolver)));
        }
    }

    record Generic(int index, boolean isMethod) implements ParsedType {
        @Override
        public ResolvedType resolve(Loc loc, TypeResolver resolver) throws CompilationException {
            //Generics not ready yet, just copy data.
            return new ResolvedType.Generic(index, isMethod);
        }
    }

    record Tuple(List<ParsedType> values) implements ParsedType {
        public static final Tuple UNIT = new Tuple(List.of());

        @Override
        public ResolvedType resolve(Loc loc, TypeResolver resolver) throws CompilationException {
            throw new UnsupportedOperationException("Tuples are TODO");
        }
    }

}
