package ast.ir.def.type;

import ast.ir.def.Program;
import ast.typed.def.type.*;
import exceptions.compile_time.CompilationException;

public interface GeneratedType {

    Program.CompiledClass compile() throws CompilationException;

    //If this TypeDef can't be made into a GeneratedClass, simply return null.
    static GeneratedType of(TypeDef typeDef) throws CompilationException {
        typeDef = typeDef.get();
        if (typeDef instanceof ClassDef || typeDef instanceof FuncImplTypeDef)
            return GeneratedClass.of(typeDef);
        if (typeDef instanceof StructDef || typeDef instanceof EnumDef)
            return GeneratedValueType.of(typeDef);
        if (typeDef instanceof BuiltinTypeDef b && b.shouldGenerateStructClassAtRuntime() ||
            typeDef instanceof TupleTypeDef)
            return GeneratedBuiltinStructType.of(typeDef);
        if (typeDef instanceof FuncTypeDef f)
            return GeneratedFuncDef.of(f);
        return null;
    }

}
