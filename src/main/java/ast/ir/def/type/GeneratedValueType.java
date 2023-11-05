package ast.ir.def.type;

import ast.ir.def.field.GeneratedField;
import ast.ir.def.GeneratedMethod;
import ast.ir.def.Program;
import ast.ir.helper.NameHelper;
import ast.typed.def.type.StructDef;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import util.ListUtils;

import java.util.List;
import java.util.Objects;

public record GeneratedValueType(String name, List<GeneratedField> fields, List<GeneratedMethod> methods) implements GeneratedType {

    public static GeneratedValueType of(TypeDef typeDef) throws CompilationException {
        return new GeneratedValueType(
                typeDef.name(),
                ListUtils.map(
                        typeDef.fields(),
                        f -> new GeneratedField(typeDef.isPlural(), f)
                ),
                ListUtils.filter(ListUtils.map(
                        typeDef.methods(),
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
