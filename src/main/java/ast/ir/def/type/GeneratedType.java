package ast.ir.def.type;

import ast.ir.def.Program;
import ast.typed.def.type.*;
import exceptions.compile_time.CompilationException;

public interface GeneratedType {

    Program.CompiledClass compile() throws CompilationException;

    //If this TypeDef can't be made into a GeneratedClass, simply return null.
    static GeneratedType of(TypeDef typeDef) throws CompilationException {
        typeDef = typeDef.get();
        if (typeDef instanceof ClassDef c)
            return GeneratedClass.of(c);
        if (typeDef instanceof StructDef || typeDef instanceof EnumDef)
            return GeneratedValueType.of(typeDef);
        if (typeDef instanceof BuiltinTypeDef b && b.shouldGenerateStructClassAtRuntime() ||
            typeDef instanceof TupleTypeDef t)
            return GeneratedBuiltinStructType.of(typeDef);
        return null;
    }

}
