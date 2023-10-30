package ast.ir.def.type;

import ast.ir.def.Program;
import ast.typed.def.type.BuiltinTypeDef;
import ast.typed.def.type.ClassDef;
import ast.typed.def.type.StructDef;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;

public interface GeneratedType {

    Program.CompiledClass compile() throws CompilationException;

    //If this TypeDef can't be made into a GeneratedClass, simply return null.
    static GeneratedType of(TypeDef typeDef) throws CompilationException {
        if (typeDef.get() instanceof ClassDef c)
            return GeneratedClass.of(c);
        if (typeDef.get() instanceof StructDef s)
            return GeneratedStruct.of(s);
        if (typeDef.get() instanceof BuiltinTypeDef b && b.shouldGenerateStructClassAtRuntime())
            return GeneratedBuiltinStructType.of(b);
        return null;
    }

}
