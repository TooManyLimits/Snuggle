package ast.type_resolved.def.field;

import ast.passes.TypeChecker;
import ast.typed.Type;
import ast.typed.def.field.FieldDef;
import exceptions.CompilationException;

import java.util.List;

public interface TypeResolvedFieldDef {

    FieldDef instantiateType(Type currentType, TypeChecker checker, List<Type> generics) throws CompilationException;

}
