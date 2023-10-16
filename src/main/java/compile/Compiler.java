package compile;

import ast.typed.def.type.SnuggleTypeDef;
import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedImport;
import ast.typed.prog.TypedAST;
import ast.typed.prog.TypedFile;
import exceptions.CompilationException;
import org.objectweb.asm.*;
import runtime.SnuggleInstance;
import runtime.SnuggleRuntime;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Compile a TypedAST into an actual runnable program!
 * Our ultimate goal is to produce a class that implements SnuggleRuntime, as well
 * as some other classes which will help out.
 *
 * Program Structure (classes generated):
 * - Runtime: A class that implements SnuggleRuntime. Interface to the code that runs.
 * - Files: Deals with imports, as well as top-level per-file code.
 * - Several others, maybe one for each SnuggleTypeDef. Stored in some data structure.
 */
public class Compiler {

    //Static method to do conversion
    public static SnuggleInstance compile(TypedAST ast) throws CompilationException {
        Compiler c = new Compiler(ast);
        return c.compile();
    }

    //Keep instances separate
    public static final AtomicInteger NEXT_ID = new AtomicInteger();

    private final TypedAST ast;
    public final int instanceId;
    public final Map<String, Integer> fileIndicesByName;

    private Compiler(TypedAST typedAST) {
        ast = typedAST;
        instanceId = NEXT_ID.getAndIncrement();

        //Generate the mapping of file names -> indices
        //{"A" -> 1, "B" -> 0} means that file A has import method "snuggleGeneratedImport_1" and so on.
        fileIndicesByName = new HashMap<>();
        for (TypedFile file : ast.files().values()) {
            int importId = fileIndicesByName.size();
            fileIndicesByName.put(file.name(), importId);
        }
    }

    public TypeDef getTypeDef(ast.typed.Type t) {
        if (t instanceof ast.typed.Type.Basic b)
            return ast.typeDefs().get(b.index());
        throw new IllegalStateException("Compiler cannot get type def of generic type? Bug in compiler, please report!");
    }

    private SnuggleInstance compile() throws CompilationException {
        //All classes other than the central "Runtime" class go here.
        List<byte[]> classes = new ArrayList<>();

        //Compile every typedef (that needs compiling)
        for (TypeDef anyTypeDef : ast.typeDefs()) {
            //Only compile snuggle type defs, not built in ones
            if (!(anyTypeDef instanceof SnuggleTypeDef typeDef)) continue;
            //Compile the TypeDef
            byte[] compiled = typeDef.compile(this);
            classes.add(compiled);
        }

        //Create the Files class
        ClassWriter filesWriter = NameHelper.generateClassWriter(NameHelper.getFilesClassName(this.instanceId));
        for (TypedFile file : ast.files().values())
            compileFile(filesWriter, file);
        filesWriter.visitEnd();
        classes.add(filesWriter.toByteArray());

        //Create the Runtime class. All it needs to do is import main.
        byte[] runtime = createRuntime();

        return new SnuggleInstance(runtime, classes);
    }

    private void compileFile(ClassWriter filesWriter, TypedFile file) throws CompilationException {
        int fileIndex = fileIndicesByName.get(file.name());
        //Generate the field
        filesWriter.visitField(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                NameHelper.getImportFieldName(fileIndex),
                "Z",
                null,
                null
        ).visitEnd();
        //Create the method
        MethodVisitor methodVisitor = filesWriter.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                NameHelper.getImportMethodName(fileIndex),
                "()V",
                null,
                null
        );
        methodVisitor.visitCode();
        //Emit bytecode into the method
        //First do the imports
        for (TypedImport typedImport : file.imports()) {
            typedImport.compile(this, null, methodVisitor);
            methodVisitor.visitInsn(Opcodes.POP);
        }
        //Now do the main code
        ScopeHelper scope = new ScopeHelper();
        for (TypedExpr code : file.code())
            code.compileAndPop(this, scope, methodVisitor);
        //Return
        methodVisitor.visitInsn(Opcodes.RETURN);
        //End the visitor
        methodVisitor.visitMaxs(0, 0); //Auto compute
        methodVisitor.visitEnd();
    }

    private byte[] createRuntime() throws CompilationException {
        ClassWriter runtimeWriter = NameHelper.generateClassWriter(NameHelper.getRuntimeClassName(this.instanceId), SnuggleRuntime.class);

        //Add necessary methods
        MethodVisitor runMethod = runtimeWriter.visitMethod(
                Opcodes.ACC_PUBLIC,
                "run",
                "()V",
                null,
                null
        );
        runMethod.visitCode();

        //Run method simply imports main
        String filesClass = NameHelper.getFilesClassName(instanceId);
        int fileId = fileIndicesByName.get("main");
        runMethod.visitInsn(Opcodes.ICONST_1);
        runMethod.visitFieldInsn(Opcodes.PUTSTATIC, filesClass, NameHelper.getImportFieldName(fileId), "Z");
        runMethod.visitMethodInsn(Opcodes.INVOKESTATIC, filesClass, NameHelper.getImportMethodName(fileId), "()V", false);
        runMethod.visitInsn(Opcodes.RETURN);
        runMethod.visitMaxs(0, 0); //Auto compute
        runMethod.visitEnd();

        //Default constructor is needed in order to instantiate it
        MethodVisitor defaultConstructor = runtimeWriter.visitMethod(
                Opcodes.ACC_PUBLIC,
                "<init>",
                "()V",
                null,
                null
        );
        defaultConstructor.visitCode();
        defaultConstructor.visitIntInsn(Opcodes.ALOAD, 0);
        defaultConstructor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(Object.class), "<init>", "()V", false);
        defaultConstructor.visitInsn(Opcodes.RETURN);
        defaultConstructor.visitMaxs(0, 0); //Auto compute
        defaultConstructor.visitEnd();
        runtimeWriter.visitEnd();
        return runtimeWriter.toByteArray();
    }
}
