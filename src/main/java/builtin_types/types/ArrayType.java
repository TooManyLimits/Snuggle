package builtin_types.types;

import ast.passes.TypeChecker;
import ast.typed.def.method.BytecodeMethodDef;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.BuiltinTypeDef;
import ast.typed.def.type.TypeDef;
import builtin_types.BuiltinType;
import builtin_types.helpers.DefineConstWithFallback;
import builtin_types.types.numbers.FloatType;
import builtin_types.types.numbers.IntegerType;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.Opcodes;
import util.ListUtils;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

public class ArrayType implements BuiltinType {

    public static final ArrayType INSTANCE = new ArrayType();
    private ArrayType() {}

    @Override
    public List<MethodDef> getMethods(TypeChecker checker, List<TypeDef> generics) {
        TypeDef u32 = checker.getBasicBuiltin(IntegerType.U32);
//        Type u32 = pool.getBasicBuiltin(IntegerType.I32);
        TypeDef unit = checker.getBasicBuiltin(UnitType.INSTANCE);
        TypeDef elementType = generics.get(0);
        ArrayOpcodes opcodes = getOpcodes(elementType);
        //Get the type name, if the opcode was ANEWARRAY:
        String typeName = opcodes.isReference() ? elementType.name() : null;

        return ListUtils.join(List.of(
                //new Array<T>(u32)
                List.of(new BytecodeMethodDef("new", false, List.of(u32), unit, v -> {
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
                List.of(new BytecodeMethodDef("set", false, List.of(u32, elementType), elementType, v -> {
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
        if (elementTypeDef.builtin() instanceof IntegerType i) {
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
        } else if (elementTypeDef.builtin() instanceof FloatType f) {
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
        } else if (elementTypeDef.builtin() == BoolType.INSTANCE) {
            newOpcodeArg = Opcodes.T_BOOLEAN;
            storeOpcode = Opcodes.BASTORE;
            loadOpcode = Opcodes.BALOAD;
        } else {
            //For now, other types are reference types
            isReference = true;
            storeOpcode = Opcodes.AASTORE;
            loadOpcode = Opcodes.AALOAD;
        }
        return new ArrayOpcodes(isReference, newOpcodeArg, dupOpcode, storeOpcode, loadOpcode);
    }

    @Override
    public String name() {
        return "Array";
    }

    @Override
    public List<String> descriptor(TypeChecker checker, List<TypeDef> generics) {
        return ListUtils.map(generics.get(0).getDescriptor(), d -> "[" + d);
    }

    @Override
    public String returnDescriptor(TypeChecker checker, List<TypeDef> generics) {
        return "[" + generics.get(0).getReturnTypeDescriptor();
    }

    @Override
    public boolean isReferenceType(TypeChecker checker, List<TypeDef> generics) {
        return true;
    }

    @Override
    public boolean isPlural(TypeChecker checker, List<TypeDef> generics) {
        return false;
    }

    @Override
    public boolean extensible(TypeChecker checker, List<TypeDef> generics) {
        return false;
    }

    @Override
    public boolean hasSpecialConstructor(TypeChecker checker, List<TypeDef> generics) {
        return true;
    }

    @Override
    public int stackSlots(TypeChecker checker, List<TypeDef> generics) {
        return 1;
    }

    @Override
    public int numGenerics() {
        return 1;
    }

    @Override
    public TypeDef getInheritanceSupertype(TypeChecker checker, List<TypeDef> generics) {
        return checker.getBasicBuiltin(ObjType.INSTANCE);
    }
}
