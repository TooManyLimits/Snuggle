package ast.ir.instruction.flow;

import ast.ir.instruction.Instruction;
import ast.typed.def.field.FieldDef;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.TypeDef;
import builtin_types.types.BoolType;
import builtin_types.types.UnitType;
import builtin_types.types.numbers.FloatType;
import builtin_types.types.numbers.IntegerType;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import util.ListUtils;

public record Return(MethodDef methodDef, TypeDef returnType) implements Instruction {

    @Override
    public void accept(MethodVisitor jvm) {
        if (returnType.isPlural()) {
            //Plural returns need to set some static fields
            storePluralFieldsRecurse(jvm, returnType, "", returnType);
            jvm.visitInsn(Opcodes.RETURN);
        } else if (returnType.builtin() == UnitType.INSTANCE) {
            if (methodDef.isConstructor()) {
                //Constructors can't return any value in the jvm
                jvm.visitInsn(Opcodes.POP);
                jvm.visitInsn(Opcodes.RETURN);
            } else {
                jvm.visitInsn(Opcodes.ARETURN);
            }
        } else if (returnType.builtin() instanceof IntegerType i) {
            if (i.bits <= 32)
                jvm.visitInsn(Opcodes.IRETURN);
            else
                jvm.visitInsn(Opcodes.LRETURN);
        } else if (returnType.builtin() instanceof FloatType f) {
            if (f.bits == 32)
                jvm.visitInsn(Opcodes.FRETURN);
            else
                jvm.visitInsn(Opcodes.DRETURN);
        } else if (returnType.builtin() == BoolType.INSTANCE)
            jvm.visitInsn(Opcodes.IRETURN);
        else
            jvm.visitInsn(Opcodes.ARETURN);
    }

    //Implementation largely the same as in MethodCall instruction
    private void storePluralFieldsRecurse(MethodVisitor jvm, TypeDef pluralType, String namePrefix, TypeDef curType) {
        String runtimeName = pluralType.runtimeName();
        if (curType.isPlural()) {
            ListUtils.iterBackwards(curType.fields(), field -> {
                if (field.isStatic()) return;
                if (namePrefix.length() == 0)
                    storePluralFieldsRecurse(jvm, pluralType, namePrefix + field.name(), field.type());
                else
                    storePluralFieldsRecurse(jvm, pluralType, namePrefix + "$" + field.name(), field.type());
            });
        } else {
            jvm.visitFieldInsn(Opcodes.PUTSTATIC, runtimeName, namePrefix, curType.getDescriptor().get(0));
        }
    }

    @Override
    public long cost() {
        return 1;
    }
}
