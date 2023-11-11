package ast.ir.instruction.objects;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.Instruction;
import ast.typed.def.field.FieldDef;
import ast.typed.def.method.BytecodeMethodDef;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

//Call the given method. The receiver/args are on the stack already.
//The result of the call should be on the stack at the end.
//If desiredFields is non-empty, then this method returns a struct (or is a struct constructor), and we
//should only push the desired fields on the stack instead of the whole struct.
public record MethodCall(boolean isSuperCall, MethodDef methodToCall, List<FieldDef> desiredFields) implements Instruction {

    @Override
    public void accept(CodeBlock block, MethodVisitor jvm) throws CompilationException {
        //Call the method:
        methodToCall.compileCall(isSuperCall, block, desiredFields, jvm);

        //If the method needs special handling afterwards (for plural topLevelTypes) then add more bytecodes
        if (methodToCall.returnType().isPlural()) {
            //If it's a bytecode method that leaves its return on the stack, no need to fetch plural fields
            if (!(methodToCall instanceof BytecodeMethodDef b) || !b.leavesReturnOnStack())
                fetchPluralFields(jvm, methodToCall.returnType());
        }
    }

    private void fetchPluralFields(MethodVisitor jvm, TypeDef pluralType) {
        StringBuilder namePrefix = new StringBuilder();
        TypeDef curType = pluralType;
        for (FieldDef def : desiredFields)
            namePrefix.append(def.name()).append("$");
        if (desiredFields.size() > 0) {
            namePrefix.deleteCharAt(namePrefix.length() - 1);
            curType = desiredFields.get(desiredFields.size() - 1).type();
        }
        fetchPluralFieldsRecurse(jvm, pluralType, namePrefix.toString(), curType);
    }

    //We want to fetch the fields of this plural type and put them on the stack
    //However, make sure we only push the fields requested by desiredFields, if desiredFields
    //is restricting them.
    private void fetchPluralFieldsRecurse(MethodVisitor jvm, TypeDef pluralType, String namePrefix, TypeDef curType) {
        String runtimeName = pluralType.runtimeName();
        if (curType.isPlural()) {
            for (FieldDef field : curType.nonStaticFields()) {
                if (namePrefix.length() == 0)
                    fetchPluralFieldsRecurse(jvm, pluralType, namePrefix + field.name(), field.type());
                else
                    fetchPluralFieldsRecurse(jvm, pluralType, namePrefix + "$" + field.name(), field.type());
            }
        } else {
            jvm.visitFieldInsn(Opcodes.GETSTATIC, runtimeName, namePrefix, curType.getDescriptor().get(0));
        }
    }


    @Override
    public long cost() throws CompilationException {
        long base = methodToCall instanceof BytecodeMethodDef bytecode ? bytecode.cost().get() : 1;
        //If plural return, then add more instructions
        if (methodToCall.returnType().isPlural())
            return base + (methodToCall.returnType().stackSlots() - 1) / 2;
        return base;
    }
}
