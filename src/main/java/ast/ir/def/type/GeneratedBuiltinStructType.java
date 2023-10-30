package ast.ir.def.type;

import ast.ir.def.Program;
import ast.ir.def.field.GeneratedField;
import ast.ir.helper.NameHelper;
import ast.typed.def.type.BuiltinTypeDef;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import util.ListUtils;

import java.util.List;

public record GeneratedBuiltinStructType(String name, List<GeneratedField> fields) implements GeneratedType {

    public static GeneratedBuiltinStructType of(BuiltinTypeDef builtin) {
        return new GeneratedBuiltinStructType(
                builtin.runtimeName(),
                ListUtils.map(builtin.fields(), f -> new GeneratedField(true, f))
        );
    }

    @Override
    public Program.CompiledClass compile() throws CompilationException {
        ClassVisitor writer = NameHelper.generateClassWriter(name, false);
        for (GeneratedField field : fields)
            field.compile(writer);
        ClassWriter asWriter = writer instanceof ClassWriter w ? w : (ClassWriter) writer.getDelegate();
        return new Program.CompiledClass(name, asWriter.toByteArray());
    }
}
