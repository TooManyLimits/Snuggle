package ast.ir.def.type;

import ast.ir.def.field.GeneratedField;
import ast.ir.def.GeneratedMethod;
import ast.ir.def.Program;
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

public record GeneratedClass(String name, TypeDef supertype, List<GeneratedField> fields, List<GeneratedMethod> methods) implements GeneratedType {


    public static GeneratedClass of(ClassDef c) throws CompilationException {
        return new GeneratedClass(
                c.name(),
                c.inheritanceSupertype(),
                ListUtils.map(
                        c.fields(),
                        f -> new GeneratedField(false, f)
                ),
                ListUtils.filter(ListUtils.map(
                        c.methods(),
                        GeneratedMethod::of
                ),      Objects::nonNull)
        );
    }


    //Compile this into a CompiledClass
    public Program.CompiledClass compile() throws CompilationException {
        String superName = supertype == null ? Type.getInternalName(Object.class) : supertype.runtimeName();
        ClassVisitor writer = NameHelper.generateClassWriter(name, superName, false);
        for (GeneratedField field : fields)
            field.compile(writer);
        for (GeneratedMethod method : methods)
            method.compile(writer);

        ClassWriter asWriter = writer instanceof ClassWriter w ? w : (ClassWriter) writer.getDelegate();
        return new Program.CompiledClass(name, asWriter.toByteArray());
    }

}
