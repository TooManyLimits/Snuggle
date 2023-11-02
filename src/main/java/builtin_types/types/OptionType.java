package builtin_types.types;

import ast.ir.instruction.stack.Pop;
import ast.passes.TypeChecker;
import ast.typed.def.field.BuiltinFieldDef;
import ast.typed.def.field.FieldDef;
import ast.typed.def.method.BytecodeMethodDef;
import ast.typed.def.method.ConstMethodDef;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedLiteral;
import ast.typed.expr.TypedMethodCall;
import builtin_types.BuiltinType;
import ast.ir.helper.BytecodeHelper;
import exceptions.runtime.SnuggleException;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import util.ListUtils;

import java.util.List;

public class OptionType implements BuiltinType {

    public static final OptionType INSTANCE = new OptionType();
    private OptionType() {}

    @Override
    public List<MethodDef> getMethods(TypeChecker checker, List<TypeDef> generics) {
        //Get some types
        TypeDef thisType = checker.getGenericBuiltin(INSTANCE, generics);
        TypeDef innerType = generics.get(0);
        TypeDef stringType = checker.getBasicBuiltin(StringType.INSTANCE);
        TypeDef boolType = checker.getBasicBuiltin(BoolType.INSTANCE);
        TypeDef unitType = checker.getBasicBuiltin(UnitType.INSTANCE);

        //Different set of methods if inner type is a reference type
        if (innerType.isReferenceType()) {
            return List.of(
                    //.get(): gets the value if present, panics if not present
                    new BytecodeMethodDef("get", false, thisType, List.of(), innerType, true, v -> {
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
                                return new TypedMethodCall(call.loc(), call.receiver(), new BytecodeMethodDef("get", false, thisType, List.of(), innerType, true, v -> {
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
                            return new TypedMethodCall(call.loc(), call.receiver(), new BytecodeMethodDef("get", false, thisType, List.of(stringType), innerType, true, v -> {
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
                    new BytecodeMethodDef("new", false, thisType, List.of(), unitType, true, v -> {
                        v.visitInsn(Opcodes.ACONST_NULL); //Literally just push null lmao
                    }),
                    new BytecodeMethodDef("new", false, thisType, List.of(innerType), unitType, true, v -> {
                        //Literally just do nothing lmao
                    }),
                    new BytecodeMethodDef("bool", false, thisType, List.of(), boolType, true, v -> {
                        Label ifPresent = new Label();
                        Label done = new Label();
                        v.visitJumpInsn(Opcodes.IFNONNULL, ifPresent);
                        v.visitInsn(Opcodes.ICONST_0);
                        v.visitJumpInsn(Opcodes.GOTO, done);
                        v.visitLabel(ifPresent);
                        v.visitInsn(Opcodes.ICONST_1);
                        v.visitLabel(done);
                    }),
                    //Same method def, just under the name "isPresent" instead in case people don't like calling .bool()
                    new BytecodeMethodDef("isPresent", false, thisType, List.of(), boolType, true, v -> {
                        Label ifPresent = new Label();
                        Label done = new Label();
                        v.visitJumpInsn(Opcodes.IFNONNULL, ifPresent);
                        v.visitInsn(Opcodes.ICONST_0);
                        v.visitJumpInsn(Opcodes.GOTO, done);
                        v.visitLabel(ifPresent);
                        v.visitInsn(Opcodes.ICONST_1);
                        v.visitLabel(done);
                    })
            );
        } else {
            //In this case, we are plural. The stack format is:
            //[innerType, bool]
            return List.of(
                    //.get(): gets the value if present, panics if not present
                    new BytecodeMethodDef("get", false, thisType, List.of(), innerType, true, v -> {
                        //Stack is [value, bool]
                        //If bool is empty
                        Label afterError = new Label();
                        v.visitJumpInsn(Opcodes.IFNE, afterError);
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
                                return new TypedMethodCall(call.loc(), call.receiver(), new BytecodeMethodDef("get", false, thisType, List.of(), innerType, true, v -> {
                                    //Stack is [value, bool]
                                    //If not empty, jump over the error throw
                                    Label afterError = new Label();
                                    v.visitJumpInsn(Opcodes.IFNE, afterError);
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
                            return new TypedMethodCall(call.loc(), call.receiver(), new BytecodeMethodDef("get", false, thisType, List.of(stringType), innerType, true, v -> {
                                //Stack is [value, bool, String]
                                v.visitInsn(Opcodes.SWAP); //[value, String, bool]
                                Label afterError = new Label();
                                v.visitJumpInsn(Opcodes.IFNE, afterError); //[value, String]
                                //Throw an error, with the item on the stack being the string
                                String exceptionName = org.objectweb.asm.Type.getInternalName(SnuggleException.class);
                                v.visitTypeInsn(Opcodes.NEW, exceptionName); //[value, String, SnuggleException]
                                v.visitInsn(Opcodes.DUP_X1); //[value, SnuggleException, String, SnuggleException]
                                v.visitInsn(Opcodes.DUP_X1); //[value, SnuggleException, SnuggleException, String, SnuggleException]
                                v.visitInsn(Opcodes.POP); //[value, SnuggleException, SnuggleException, String]
                                v.visitMethodInsn(Opcodes.INVOKESPECIAL, exceptionName, "<init>", "(Ljava/lang/String;)V", false); //[value, SnuggleException]
                                v.visitInsn(Opcodes.ATHROW); //Throw!
                                //Emit label
                                v.visitLabel(afterError); //[value, String]
                                v.visitInsn(Opcodes.POP); //[value]
                            }), call.args(), innerType);
                        }
                    }, null),
                    new BytecodeMethodDef("new", false, thisType, List.of(), unitType, true, v -> {
                        //Push a default innerType on the stack, then push false, then continue along
                        BytecodeHelper.pushDefaultValue(v, innerType);
                        v.visitInsn(Opcodes.ICONST_0);
                    }),
                    new BytecodeMethodDef("new", false, thisType, List.of(innerType), unitType, true, v -> {
                        //Just wrap the value with true lol
                        v.visitInsn(Opcodes.ICONST_1);
                    }),
                    new BytecodeMethodDef("bool", false, thisType, List.of(), boolType, true, v -> {
                        //Stack is [value, bool]
                        Label ifPresent = new Label();
                        Label done = new Label();
                        v.visitJumpInsn(Opcodes.IFNE, ifPresent); //[value]
                        new Pop(innerType).accept(null, v); //Pop inner off the stack: []. Pop doesn't use CodeBlock param.
                        v.visitInsn(Opcodes.ICONST_0); //Push false: [false]
                        v.visitJumpInsn(Opcodes.GOTO, done);
                        v.visitLabel(ifPresent); //[value]
                        new Pop(innerType).accept(null, v); //Pop inner off the stack: []. Pop doesn't use CodeBlock param.
                        v.visitInsn(Opcodes.ICONST_1); //Push true: [true]
                        v.visitLabel(done); //[true] or [false]
                    }),
                    //Same method def, different name "isPresent" in case people don't like .bool()
                    new BytecodeMethodDef("isPresent", false, thisType, List.of(), boolType, true, v -> {
                        //Stack is [value, bool]
                        Label ifPresent = new Label();
                        Label done = new Label();
                        v.visitJumpInsn(Opcodes.IFNE, ifPresent); //[value]
                        new Pop(innerType).accept(null, v); //Pop inner off the stack: []. Pop doesn't use CodeBlock param.
                        v.visitInsn(Opcodes.ICONST_0); //Push false: [false]
                        v.visitJumpInsn(Opcodes.GOTO, done);
                        v.visitLabel(ifPresent); //[value]
                        new Pop(innerType).accept(null, v); //Pop inner off the stack: []. Pop doesn't use CodeBlock param.
                        v.visitInsn(Opcodes.ICONST_1); //Push true: [true]
                        v.visitLabel(done); //[true] or [false]
                    })
            );
        }
    }

    @Override
    public List<FieldDef> getFields(TypeChecker checker, List<TypeDef> generics) {
        if (!isPlural(checker, generics))
            return List.of(); //Non-plural form: no fields
        //Plural form needs fields. An Option is a struct composed of:
        // - the inner, wrapped type
        // - a boolean
        TypeDef thisType = checker.getGenericBuiltin(INSTANCE, generics);
        TypeDef innerType = generics.get(0);
        TypeDef boolType = checker.getBasicBuiltin(BoolType.INSTANCE);
        return List.of(
                new BuiltinFieldDef("#value", thisType, innerType, false),
                new BuiltinFieldDef("#isPresent", thisType, boolType, false)
        );
    }

    @Override
    public String name() {
        return "Option";
    }

    @Override
    public String genericName(TypeChecker checker, List<TypeDef> generics) {
        return generics.get(0).name() + "?";
    }

    @Override
    public List<String> descriptor(TypeChecker checker, List<TypeDef> generics) {
        if (generics.get(0).isReferenceType())
            return generics.get(0).getDescriptor();
        return ListUtils.join(List.of(List.of("Z"), generics.get(0).getDescriptor()));
    }

    @Override
    public String returnDescriptor(TypeChecker checker, List<TypeDef> generics) {
        if (generics.get(0).isReferenceType())
            return generics.get(0).getReturnTypeDescriptor();
        return "V";
    }

    @Override
    public boolean isReferenceType(TypeChecker checker, List<TypeDef> generics) {
        return false;
    }

    @Override
    public boolean isPlural(TypeChecker checker, List<TypeDef> generics) {
        return !generics.get(0).isReferenceType();
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
    public boolean shouldGenerateStructClassAtRuntime(TypeChecker checker, List<TypeDef> generics) {
        return isPlural(checker, generics);
    }

    @Override
    public int stackSlots(TypeChecker checker, List<TypeDef> generics) {
        if (generics.get(0).isReferenceType())
            return 1;
        return generics.get(0).stackSlots() + 1;
    }

    @Override
    public int numGenerics() {
        return 1;
    }


}
