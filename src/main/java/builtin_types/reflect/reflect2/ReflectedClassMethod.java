package builtin_types.reflect.reflect2;

import ast.passes.TypeChecker;
import ast.typed.def.method.BytecodeMethodDef;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.TypeDef;
import builtin_types.reflect.annotations.Rename;
import builtin_types.reflect.annotations.SnuggleBlacklist;
import builtin_types.reflect.annotations.SnuggleWhitelist;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import util.ListUtils;
import util.throwing_interfaces.ThrowingConsumer;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * A method on a reflected class
 */
public class ReflectedClassMethod {

    private final String origName, name, owner, descriptor;
    private final boolean isStatic;
    private final ReflectionUtil.TypeGetter ownerTypeGetter, returnTypeGetter;
    private final List<ReflectionUtil.TypeGetter> paramTypeGetters;
    private final ThrowingConsumer<MethodVisitor, CompilationException> bytecodeVisitor;

    private ReflectedClassMethod(
            String origName, String name, String owner, String descriptor,
            boolean isStatic,
            ReflectionUtil.TypeGetter ownerTypeGetter,
            ReflectionUtil.TypeGetter returnTypeGetter,
            List<ReflectionUtil.TypeGetter> paramTypeGetters,
            ThrowingConsumer<MethodVisitor, CompilationException> bytecodeVisitor) {
        this.origName = origName;
        this.name = name;
        this.owner = owner;
        this.descriptor = descriptor;
        this.isStatic = isStatic;
        this.ownerTypeGetter = ownerTypeGetter;
        this.returnTypeGetter = returnTypeGetter;
        this.paramTypeGetters = paramTypeGetters;
        this.bytecodeVisitor = bytecodeVisitor;
    }

    //Get the method. Return null if the method should not be reflected.
    public static ReflectedClassMethod of(Method method) {
        //Return null if we shouldn't reflect
        if (method.isAnnotationPresent(SnuggleBlacklist.class) || (
                !method.getDeclaringClass().isAnnotationPresent(SnuggleWhitelist.class) &&
                !method.isAnnotationPresent(SnuggleWhitelist.class))
        ) return null;

        //Reflect!
        //Create some variables
        String origName = method.getName();
        String name = method.isAnnotationPresent(Rename.class) ? method.getAnnotation(Rename.class).value() : origName;
        String owner = Type.getInternalName(method.getDeclaringClass());
        String descriptor = Type.getMethodDescriptor(method);
        boolean isStatic = Modifier.isStatic(method.getModifiers());
        //Create the method
        return new ReflectedClassMethod(
                origName, name, owner, descriptor, isStatic,
                (checker, loc, cause) -> checker.getReflectedBuiltin(method.getDeclaringClass()),
                ReflectionUtil.getTypeGetter(method.getAnnotatedReturnType()),
                ListUtils.map(List.of(method.getAnnotatedParameterTypes()), ReflectionUtil::getTypeGetter),
                jvm -> jvm.visitMethodInsn(
                        isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL,
                        owner,
                        origName,
                        descriptor,
                        false
                )
        );
    }

    //Generate the method def
    public MethodDef get(TypeChecker checker, List<TypeDef> typeGenerics, Loc instantiationLoc, TypeDef.InstantiationStackFrame cause) {
        return new BytecodeMethodDef(
                name,
                isStatic,
                ownerTypeGetter.get(checker, instantiationLoc, cause),
                ListUtils.map(paramTypeGetters, g -> g.get(checker, instantiationLoc, cause)),
                returnTypeGetter.get(checker, instantiationLoc, cause),
                false,
                v -> {}
        );
    }


}
