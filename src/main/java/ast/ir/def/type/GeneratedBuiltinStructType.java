package ast.ir.def.type;

import ast.ir.def.Program;
import ast.ir.def.GeneratedField;
import ast.ir.def.method.GeneratedMethod;
import ast.ir.helper.NameHelper;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import util.ListUtils;

import java.util.List;
import java.util.Objects;

public record GeneratedBuiltinStructType(String name, List<GeneratedField> fields, List<GeneratedMethod> methods) implements GeneratedType {

    public static GeneratedBuiltinStructType of(TypeDef builtin) throws CompilationException {
        //If the builtin is size 0 and has no static methods, then
        //don't generate anything for it.
        if (builtin.fields().size() == 0 && ListUtils.find(builtin.methods(), MethodDef::isStatic) == null)
            return null;
        return new GeneratedBuiltinStructType(
                builtin.runtimeName(),
                ListUtils.map(builtin.fields(), f -> new GeneratedField(true, f)),
                ListUtils.filter(ListUtils.map(builtin.methods(), GeneratedMethod::of), Objects::nonNull)
        );
    }

    @Override
    public Program.CompiledClass compile() throws CompilationException {
        ClassVisitor writer = NameHelper.generateClassWriter(name, false);
        for (GeneratedField field : fields)
            field.compile(writer);
        for (GeneratedMethod method : methods)
            method.compile(writer);
        ClassWriter asWriter = writer instanceof ClassWriter w ? w : (ClassWriter) writer.getDelegate();
        return new Program.CompiledClass(name, asWriter.toByteArray());
    }
}
