package ast.type_resolved.def.field;

import ast.passes.TypeChecker;
import ast.typed.def.field.FieldDef;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;

import java.util.List;

public interface TypeResolvedFieldDef {

    boolean isStatic();
    FieldDef instantiateType(TypeDef currentType, TypeChecker checker, List<TypeDef> generics, TypeDef.InstantiationStackFrame cause) throws CompilationException;

}
