package builtin_types.types;

import ast.passes.TypePool;
import ast.typed.Type;
import ast.typed.def.method.BytecodeMethodDef;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.BuiltinTypeDef;
import ast.typed.def.type.TypeDef;
import builtin_types.BuiltinType;
import builtin_types.helpers.DefineConstWithFallback;
import builtin_types.types.numbers.FloatType;
import builtin_types.types.numbers.IntegerType;
import compile.BytecodeHelper;
import exceptions.CompilationException;
import org.objectweb.asm.Opcodes;
import util.ListUtils;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

public class ArrayType implements BuiltinType {

    public static final ArrayType INSTANCE = new ArrayType();
    private ArrayType() {}


    @Override
    public List<? extends MethodDef> getMethods(List<Type> generics, TypePool pool) throws CompilationException {
        Type u32 = pool.getBasicBuiltin(IntegerType.U32);
        Type unit = pool.getBasicBuiltin(UnitType.INSTANCE);
        Type elementType = generics.get(0);
        ArrayOpcodes opcodes = getOpcodes(pool.getTypeDef(elementType));
        //Get the type name, if the opcode was ANEWARRAY:
        String typeName = opcodes.isReference() ? pool.getTypeDef(elementType).getRuntimeName() : null;

        return ListUtils.join(List.of(
                //new Array<T>(u32)
                List.of(new BytecodeMethodDef(false, "new", List.of(u32), unit, v -> {
                    if (opcodes.isReference)
                        v.visitTypeInsn(Opcodes.ANEWARRAY, typeName);
                    else
                        v.visitIntInsn(Opcodes.NEWARRAY, opcodes.newOpcodeArg());
                })),
                //arr.len() -> u32
                DefineConstWithFallback.defineUnary("len", (List<?> x) -> x.size(), u32, v -> {
                    v.visitInsn(Opcodes.ARRAYLENGTH);
                }),
                //arr.get(u32) -> elementType
                //arr[u32] -> elementType
                DefineConstWithFallback.defineBinary("get", (List<?> x, BigInteger i) -> x.get(i.intValue()), u32, elementType, v -> {
                    v.visitInsn(opcodes.loadOpcode);
                }),
                //arr.set(u32, elementType) -> elementType
                //(arr[u32] = elementType) -> elementType
                List.of(new BytecodeMethodDef(false, "set", List.of(u32, elementType), elementType, v -> {
                    v.visitInsn(opcodes.dupOpcode);
                    v.visitInsn(opcodes.storeOpcode);
                }))
        ));
    }


    //Get the proper store/load opcodes for this array
    private record ArrayOpcodes(boolean isReference, int newOpcodeArg, int dupOpcode, int storeOpcode, int loadOpcode) {}
    private static ArrayOpcodes getOpcodes(TypeDef elementTypeDef) {
        boolean isReference = false;
        int newOpcodeArg = -1;
        int dupOpcode = Opcodes.DUP_X2;
        int storeOpcode;
        int loadOpcode;
        if (elementTypeDef instanceof BuiltinTypeDef b) {
            if (b.builtin() instanceof IntegerType i) {
                switch (i.bits) {
                    case 8 -> {
                        newOpcodeArg = Opcodes.T_BYTE;
                        storeOpcode = Opcodes.BASTORE;
                        loadOpcode = Opcodes.BALOAD;
                    }
                    case 16 -> {
                        newOpcodeArg = Opcodes.T_SHORT;
                        storeOpcode = Opcodes.SASTORE;
                        loadOpcode = Opcodes.SALOAD;
                    }
                    case 32 -> {
                        newOpcodeArg = Opcodes.T_INT;
                        storeOpcode = Opcodes.IASTORE;
                        loadOpcode = Opcodes.IALOAD;
                    }
                    case 64 -> {
                        dupOpcode = Opcodes.DUP2_X2;
                        newOpcodeArg = Opcodes.T_LONG;
                        storeOpcode = Opcodes.LASTORE;
                        loadOpcode = Opcodes.LALOAD;
                    }
                    default -> throw new IllegalStateException("Illegal bit count, bug in compiler, please report!");
                }
            } else if (b.builtin() instanceof FloatType f) {
                switch (f.bits) {
                    case 32 -> {
                        newOpcodeArg = Opcodes.T_FLOAT;
                        storeOpcode = Opcodes.FASTORE;
                        loadOpcode = Opcodes.FALOAD;
                    }
                    case 64 -> {
                        dupOpcode = Opcodes.DUP2_X2;
                        newOpcodeArg = Opcodes.T_DOUBLE;
                        storeOpcode = Opcodes.DASTORE;
                        loadOpcode = Opcodes.DALOAD;
                    }
                    default -> throw new IllegalStateException("Illegal bit count, bug in compiler, please report!");
                }
            } else if (b.builtin() == BoolType.INSTANCE) {
                newOpcodeArg = Opcodes.T_BOOLEAN;
                storeOpcode = Opcodes.BASTORE;
                loadOpcode = Opcodes.BALOAD;
            } else {
                isReference = true;
                storeOpcode = Opcodes.AASTORE;
                loadOpcode = Opcodes.AALOAD;
            }
        } else {
            //For now, non-builtins are all reference types
            isReference = true;
            storeOpcode = Opcodes.AASTORE;
            loadOpcode = Opcodes.AALOAD;
        }
        return new ArrayOpcodes(isReference, newOpcodeArg, dupOpcode, storeOpcode, loadOpcode);
    }


    @Override
    public Set<Type> getSupertypes(List<Type> generics, TypePool pool) throws CompilationException {
        return Set.of(pool.getBasicBuiltin(ObjType.INSTANCE));
    }

    @Override
    public Type getTrueSupertype(List<Type> generics, TypePool pool) throws CompilationException {
        return pool.getBasicBuiltin(ObjType.INSTANCE);
    }

    @Override
    public String name() {
        return "Array";
    }

    @Override
    public String genericName(List<Type> generics, TypePool pool) {
        return name() + "(" + generics.get(0).name(pool) + ")";
    }

    @Override
    public int numGenerics() {
        return 1;
    }

    @Override
    public String getDescriptor(List<Type> generics, TypePool pool) {
        return "[" + pool.getTypeDef(generics.get(0)).getDescriptor();
    }

    @Override
    public String getRuntimeName(List<Type> generics, TypePool pool) {
        return null;
    }

    @Override
    public boolean extensible() {
        return false;
    }
}
