package builtin_types.types;

import ast.passes.TypePool;
import ast.typed.Type;
import ast.typed.def.method.BytecodeMethodDef;
import ast.typed.def.method.ConstMethodDef;
import ast.typed.def.method.MethodDef;
import ast.typed.expr.TypedLiteral;
import ast.typed.expr.TypedMethodCall;
import builtin_types.BuiltinType;
import builtin_types.helpers.DefineConstWithFallback;
import compile.BytecodeHelper;
import exceptions.compile_time.CompilationException;
import exceptions.runtime.SnuggleException;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import java.util.List;

public class OptionType implements BuiltinType {

    public static final OptionType INSTANCE = new OptionType();
    private OptionType() {}

    @Override
    public String name() {
        return "Option";
    }

    @Override
    public String genericName(List<Type> generics, TypePool pool) {
        return generics.get(0).name(pool) + "?";
    }

    @Override
    public List<? extends MethodDef> getMethods(List<Type> generics, TypePool pool) throws CompilationException {
        //Get some types
        Type thisType = pool.getGenericBuiltin(INSTANCE, generics);
        Type innerType = generics.get(0);
        Type stringType = pool.getBasicBuiltin(StringType.INSTANCE);
        Type boolType = pool.getBasicBuiltin(BoolType.INSTANCE);
        Type unitType = pool.getBasicBuiltin(UnitType.INSTANCE);

        //Different set of methods if inner type is a reference type
        if (pool.getTypeDef(innerType).isReferenceType()) {
            return List.of(
                    //.get(): gets the value if present, panics if not present
                    new BytecodeMethodDef(false, "get", List.of(), innerType, v -> {
                        //If non-null, jump over the error throw
                        Label afterError = new Label();
                        v.visitInsn(Opcodes.DUP);
                        v.visitJumpInsn(Opcodes.IFNONNULL, afterError);
                        //Throw an error
                        BytecodeHelper.createObject(v, SnuggleException.class, "Tried to use get() on empty Option!");
                        v.visitInsn(Opcodes.ATHROW);
                        //Emit label
                        v.visitLabel(afterError);
                    }),
                    //get() with special error message
                    new ConstMethodDef("get", 0, false, List.of(stringType), innerType, call -> {
                        //If error message is known at compile time, then bake it into the method definition
                        if (call.args().get(0) instanceof TypedLiteral literal) {
                            if (literal.obj() instanceof String constantErrorMessage) {
                                return new TypedMethodCall(call.loc(), call.receiver(), new BytecodeMethodDef(false, "get", List.of(), innerType, v -> {
                                    //If non-null, jump over the error throw
                                    Label afterError = new Label();
                                    v.visitInsn(Opcodes.DUP);
                                    v.visitJumpInsn(Opcodes.IFNONNULL, afterError);
                                    //Throw an error
                                    BytecodeHelper.createObject(v, SnuggleException.class, constantErrorMessage);
                                    v.visitInsn(Opcodes.ATHROW);
                                    //Emit label
                                    v.visitLabel(afterError);
                                }), List.of(), innerType);
                            } else {
                                throw new IllegalStateException("Method get() expects string, but was literal and not string? Bug in compiler, please report!");
                            }
                        } else {
                            return new TypedMethodCall(call.loc(), call.receiver(), new BytecodeMethodDef(false, "get", List.of(stringType), innerType, v -> {
                                //Stack is [this, String]
                                v.visitInsn(Opcodes.SWAP); //[String, this]
                                v.visitInsn(Opcodes.DUP); //[String, this, this]
                                Label afterError = new Label();
                                v.visitJumpInsn(Opcodes.IFNONNULL, afterError); //[String, this]
                                //Throw an error, with the item on the stack being the string
                                v.visitInsn(Opcodes.POP);
                                String exceptionName = org.objectweb.asm.Type.getInternalName(SnuggleException.class);
                                v.visitTypeInsn(Opcodes.NEW, exceptionName); //[String, SnuggleException]
                                v.visitInsn(Opcodes.DUP_X1); //[SnuggleException, String, SnuggleException]
                                v.visitInsn(Opcodes.DUP_X1); //[SnuggleException, SnuggleException, String, SnuggleException]
                                v.visitInsn(Opcodes.POP); //[SnuggleException, SnuggleException, String]
                                v.visitMethodInsn(Opcodes.INVOKESPECIAL, exceptionName, "<init>", "(Ljava/lang/String;)V", false); //[SnuggleException]
                                v.visitInsn(Opcodes.ATHROW); //Throw!
                                //Emit label
                                v.visitLabel(afterError); //[String, this]
                                v.visitInsn(Opcodes.SWAP); //[this, String]
                                v.visitInsn(Opcodes.POP); //[this]
                            }), call.args(), innerType);
                        }
                    }, null),
                    new BytecodeMethodDef(false, "new", List.of(), unitType, v -> {
                        v.visitInsn(Opcodes.ACONST_NULL); //Literally just push null lmao
                    }),
                    new BytecodeMethodDef(false, "new", List.of(innerType), unitType, v -> {
                        //Literally just do nothing lmao
                    })
            );
        } else {
            throw new IllegalStateException("Optional non-reference types not yet implemented!");
        }
    }

    @Override
    public int numGenerics() { return 1; }

    @Override
    public String getDescriptor(List<Type> generics, TypePool pool) {
        return null;
    }

    @Override
    public String getRuntimeName(List<Type> generics, TypePool pool) {
        return null;
    }

    @Override
    public boolean extensible() {
        return false;
    }

    @Override
    public boolean hasSpecialConstructor(List<Type> generics, TypePool pool) {
        //Special constructor if the inner type is a reference type
        return pool.getTypeDef(generics.get(0)).isReferenceType();
    }

    @Override
    public boolean isReferenceType(List<Type> generics, TypePool pool) {
        //Option is not itself considered a reference type, even if it contains a reference type.
        return false;
    }
}
