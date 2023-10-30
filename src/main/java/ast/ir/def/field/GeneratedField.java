package ast.ir.def.field;

import ast.typed.def.field.FieldDef;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

public record GeneratedField(boolean isMemberOfPluralType, FieldDef fieldDef) {

    public void compile(ClassVisitor writer) {
        compileRecursive(writer, "", fieldDef);
    }

    //If plural, recurse and add smaller fields
    private void compileRecursive(ClassVisitor writer, String namePrefix, FieldDef fieldDef) {
        if (fieldDef.type().isPlural()) {
            //If plural, recurse
            for (FieldDef nestedDef : fieldDef.type().fields()) {
                if (nestedDef.isStatic()) continue;
                compileRecursive(writer, fieldDef.name() + "$", nestedDef);
            }
        } else {
            //If non-plural, add the field.
            int access = Opcodes.ACC_PUBLIC;
            if (fieldDef.isStatic() || isMemberOfPluralType) access |= Opcodes.ACC_STATIC;
            writer.visitField(access, namePrefix + fieldDef.name(), fieldDef.type().getDescriptor().get(0), null, null).visitEnd();
        }
    }

}
