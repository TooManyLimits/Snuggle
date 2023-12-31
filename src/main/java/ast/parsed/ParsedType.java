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
            //Basic topLevelTypes; lookup their name
            Integer resolvedName = resolver.lookup(name);
            if (resolvedName == null)
                throw new UnknownTypeException("Type \"" + name + "\" could not be found in the current scope.", loc);
            return new ResolvedType.Basic(resolvedName, ListUtils.map(generics, g -> g.resolve(loc, resolver)));
        }

        @Override
        public String toString() {
            StringBuilder s = new StringBuilder(name);
            if (generics.size() > 0) {
                s.append("<");
                for (ParsedType t : generics)
                    s.append(t).append(", ");
                s.delete(s.length() - 2, s.length());
                s.append(">");
            }
            return s.toString();
        }
    }

    record Generic(int index, boolean isMethod) implements ParsedType {
        @Override
        public ResolvedType resolve(Loc loc, TypeResolver resolver) throws CompilationException {
            //Generics not ready yet, just copy data.
            return new ResolvedType.Generic(index, isMethod);
        }

        @Override
        public String toString() {
            return (isMethod ? "Method" : "Type") + ("Generic(" + index + ")");
        }
    }

    record Tuple(List<ParsedType> elements) implements ParsedType {
        public static final Tuple UNIT = new Tuple(List.of());

        @Override
        public ResolvedType resolve(Loc loc, TypeResolver resolver) throws CompilationException {
            return new ResolvedType.Tuple(ListUtils.map(elements, e -> e.resolve(loc, resolver)));
        }

        @Override
        public String toString() {
            return "Tuple" + elements;
        }
    }

    record Func(List<ParsedType> paramTypes, ParsedType resultType) implements ParsedType {

        @Override
        public ResolvedType resolve(Loc loc, TypeResolver resolver) throws CompilationException {
            return new ResolvedType.Func(ListUtils.map(paramTypes, p -> p.resolve(loc, resolver)), resultType.resolve(loc, resolver));
        }

        @Override
        public String toString() {
            if (paramTypes.size() == 0) {
                return "() -> " + resultType;
            } else if (paramTypes.size() == 1) {
                return paramTypes.get(0) + " -> " + resultType;
            } else {
                StringBuilder s = new StringBuilder("(");
                for (ParsedType p : paramTypes)
                    s.append(p).append(", ");
                s.delete(s.length() - 2, s.length());
                return s.toString();
            }
        }
    }

}
