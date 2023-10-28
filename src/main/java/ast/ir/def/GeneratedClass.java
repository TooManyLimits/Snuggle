package ast.ir.def;

import ast.ir.helper.NameHelper;
import ast.typed.def.type.ClassDef;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import util.ListUtils;

import java.util.List;
import java.util.Objects;

public record GeneratedClass(String name, TypeDef supertype, List<GeneratedMethod> methods) {


    //If this TypeDef can't be made into a GeneratedClass, simply return null.
    public static GeneratedClass of(TypeDef typeDef) {
        if (!(typeDef.get() instanceof ClassDef))
            return null;

        return new GeneratedClass(
                typeDef.name(),
                typeDef.inheritanceSupertype(),
                ListUtils.filter(ListUtils.map(
                        typeDef.methods(),
                        GeneratedMethod::of
                ),      Objects::nonNull)
        );
    }


    //Compile this into a CompiledClass
    public Program.CompiledClass compile() throws CompilationException {
        String superName = supertype == null ? Type.getInternalName(Object.class) : supertype.runtimeName();
        ClassVisitor writer = NameHelper.generateClassWriter(name, superName, false);
        for (GeneratedMethod method : methods)
            method.compile(writer);

        ClassWriter asWriter = writer instanceof ClassWriter w ? w : (ClassWriter) writer.getDelegate();
        return new Program.CompiledClass(name, asWriter.toByteArray());
    }

}
