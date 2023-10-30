package ast.type_resolved.def.method;

import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import ast.passes.TypeChecker;
import ast.typed.def.method.MethodDef;

import java.util.List;

public interface TypeResolvedMethodDef {

    String name();

    default boolean isConstructor() {
        return name().equals("new");
    }

    /**
     * Note that this only instantiates *TYPE* generics,
     * NOT *METHOD* generics!
     */
    MethodDef instantiateType(List<? extends TypeResolvedMethodDef> methodDefs, TypeDef currentType, TypeChecker checker, List<TypeDef> generics) throws CompilationException;

}
