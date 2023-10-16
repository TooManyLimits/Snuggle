package builtin_types.types;

import ast.passes.TypePool;
import ast.typed.Type;
import ast.typed.def.method.BytecodeMethodDef;
import ast.typed.def.method.MethodDef;
import builtin_types.BuiltinType;
import builtin_types.types.numbers.IntegerType;
import compile.BytecodeHelper;
import exceptions.CompilationException;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import runtime.Unit;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

//System
public class SystemType implements BuiltinType {

    public static final SystemType INSTANCE = new SystemType();
    private SystemType() {}

    @Override
    public String name() {
        return "System";
    }


    //Generates a System.out.println() bytecode method def.
    //Has some weird params pre/post, but this is just to isolate the parts which change between definitions.
    private BytecodeMethodDef genPrintln(TypePool pool, Type unit, BuiltinType builtinType, String descriptor, Consumer<MethodVisitor> pre, Consumer<MethodVisitor> post) throws CompilationException {
        return new BytecodeMethodDef(true, "print", List.of(pool.getBasicBuiltin(builtinType)), unit, visitor -> {
            if (pre != null) pre.accept(visitor);
            String system = org.objectweb.asm.Type.getInternalName(System.class);
            String printstream = org.objectweb.asm.Type.getInternalName(PrintStream.class);
            visitor.visitFieldInsn(Opcodes.GETSTATIC, system, "out", "L" + printstream + ";");
            if (post != null) post.accept(visitor);
            visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, printstream, "println", descriptor, false);
            String unitClass = org.objectweb.asm.Type.getInternalName(Unit.class);
            visitor.visitFieldInsn(Opcodes.GETSTATIC, unitClass, "INSTANCE", "L" + unitClass + ";");
        });
    }

    @Override
    public List<? extends MethodDef> getMethods(TypePool pool) throws CompilationException {
        List<MethodDef> list = new ArrayList<>();
        Type unit = pool.getBasicBuiltin(UnitType.INSTANCE);

        //Helper
        Consumer<MethodVisitor> swap = v -> v.visitInsn(Opcodes.SWAP);

        //Printers
        //Boolean
        list.add(genPrintln(pool, unit, BoolType.INSTANCE, "(Z)V", null, swap));

        //Integers
        list.add(genPrintln(pool, unit, IntegerType.I8, "(I)V", null, swap));
        list.add(genPrintln(pool, unit, IntegerType.I16, "(I)V", null, swap));
        list.add(genPrintln(pool, unit, IntegerType.I32, "(I)V", null, swap));
        list.add(genPrintln(pool, unit, IntegerType.I64, "(J)V", null, BytecodeHelper::swapBigSmall));
        list.add(genPrintln(pool, unit, IntegerType.U8, "(I)V", BytecodeHelper::u8ToInt, swap));
        list.add(genPrintln(pool, unit, IntegerType.U16, "(I)V", BytecodeHelper::u16ToInt, swap));
        list.add(genPrintln(pool, unit, IntegerType.U32, "(J)V", BytecodeHelper::u32ToLong, BytecodeHelper::swapBigSmall));
        list.add(genPrintln(pool, unit, IntegerType.U64, "(J)V",
            //Bytecodes made from the implementation of Long.toUnsignedString
            v -> {
                //stack = val
                v.visitInsn(Opcodes.DUP2); //val, val
                v.visitInsn(Opcodes.ICONST_1); //val, val, 1
                v.visitInsn(Opcodes.LUSHR); //val, val >>> 1
                v.visitLdcInsn(5L); //val, val >>> 1, 5
                v.visitInsn(Opcodes.LDIV); //val, (val >>> 1) / 5
                v.visitInsn(Opcodes.DUP2); //val, (val >>> 1) / 5, (val >>> 1) / 5
                v.visitLdcInsn(10L); //val, (val >>> 1) / 5, (val >>> 1) / 5, 10
                v.visitInsn(Opcodes.LMUL); //val, (val >>> 1) / 5, (val >>> 1) / 5 * 10
                BytecodeHelper.swapBigBig(v); //val, (val >>> 1) / 5 * 10, (val >>> 1) / 5
            },
            v -> {
                //stack = val, (val >>> 1) / 5 * 10, (val >>> 1) / 5, System.out
                BytecodeHelper.swapBigSmall(v);//val, (val >>> 1) / 5 * 10, System.out, (val >>> 1) / 5
                String system = org.objectweb.asm.Type.getInternalName(System.class);
                String printstream = org.objectweb.asm.Type.getInternalName(PrintStream.class);
                v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, printstream, "print", "(J)V", false); //val, (val >>> 1) / 5 * 10
                v.visitInsn(Opcodes.LSUB); //val - (val >>> 1) / 5 * 10
                v.visitFieldInsn(Opcodes.GETSTATIC, system, "out", "L" + printstream + ";"); //val - (val >>> 1) / 5 * 10, System.out
                BytecodeHelper.swapBigSmall(v); //System.out, val - (val >>> 1) / 5 * 10
                //done!
            }
        ));

        return list;
    }

    @Override
    public String getDescriptor(int index) {
        throw new IllegalStateException("Cannot get descriptor for type System? Bug in compiler, please report!");
    }
}
