package ast.type_resolved.def.method;

import exceptions.CompilationException;
import ast.passes.TypeChecker;
import ast.typed.Type;
import ast.typed.def.method.MethodDef;

import java.util.List;

public interface TypeResolvedMethodDef {

    /**
     * Note that this only instantiates *TYPE* generics,
     * NOT *METHOD* generics!
     */
    MethodDef instantiateType(Type currentType, TypeChecker checker, List<Type> generics) throws CompilationException;

}
