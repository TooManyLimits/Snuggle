package ast.ir.def.type;

import ast.ir.def.field.GeneratedField;
import ast.ir.def.GeneratedMethod;
import ast.ir.def.Program;
import ast.ir.helper.NameHelper;
import ast.typed.def.type.StructDef;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import util.ListUtils;

import java.util.List;
import java.util.Objects;

public record GeneratedStruct(String name, List<GeneratedField> fields, List<GeneratedMethod> methods) implements GeneratedType {

    public static GeneratedStruct of(StructDef s) throws CompilationException {
        return new GeneratedStruct(
                s.name(),
                ListUtils.map(
                        s.fields(),
                        f -> new GeneratedField(true, f)
                ),
                ListUtils.filter(ListUtils.map(
                        s.methods(),
                        GeneratedMethod::of
                ),      Objects::nonNull)
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
