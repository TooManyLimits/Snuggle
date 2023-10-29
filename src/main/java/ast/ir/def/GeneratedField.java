package ast.ir.def;

import ast.typed.def.field.FieldDef;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

public record GeneratedField(FieldDef fieldDef) {

    public static List<GeneratedField> of(FieldDef fieldDef) {
        if (fieldDef.type().isPlural())
            throw new IllegalStateException("Plural types not yet supported");
        return List.of(new GeneratedField(fieldDef));
    }

    public void compile(ClassVisitor writer) {
        int access = Opcodes.ACC_PUBLIC;
        if (fieldDef.isStatic()) access += Opcodes.ACC_STATIC;
        //Create writer
        FieldVisitor fieldWriter = writer.visitField(access, fieldDef.name(), fieldDef.type().getDescriptor().get(0), null, null);
        //Visit end
        fieldWriter.visitEnd();
    }

}
