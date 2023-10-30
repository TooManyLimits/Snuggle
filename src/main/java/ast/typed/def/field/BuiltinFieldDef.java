package ast.typed.def.field;

import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;

/**
 * An implementation of FieldDef that's useful for Builtin types.
 *
 * In some cases, if you don't want these fields to be accessible by
 * code, you should place some characters into the name that are not
 * allowed in Snuggle identifiers. (Don't use $ or () for this, because
 * those are added in certain other locations by the compiler.)
 */
public record BuiltinFieldDef(String name, TypeDef owningType, TypeDef type, boolean isStatic) implements FieldDef {

    @Override
    public void checkCode() throws CompilationException {

    }
}
