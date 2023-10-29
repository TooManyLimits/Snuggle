package ast.ir.def;

import ast.ir.helper.NameHelper;
import ast.ir.instruction.stack.Pop;
import ast.typed.expr.TypedExpr;
import ast.typed.prog.TypedAST;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import runtime.SnuggleInstance;
import runtime.SnuggleRuntime;
import util.ListUtils;
import util.MapUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Holds the full program in IR form.
 * Many elements of the IR refer to TypeDef objects, so those
 * need to stay around.
 */
public record Program(List<GeneratedClass> generatedClasses, Map<String, CodeBlock> topLevelCode) {

    public static Program of(TypedAST typedAST) throws CompilationException {
        //Create the generatedClasses:
        List<GeneratedClass> classes = ListUtils.filter(ListUtils.map(
                typedAST.typeDefs(),
                GeneratedClass::of
        ),      Objects::nonNull);

        //Create the top-level code
        Map<String, CodeBlock> topLevelCode = MapUtils.mapValues(typedAST.files(), file -> {
            CodeBlock codeBlock = new CodeBlock();
            for (TypedExpr expr : file.code()) {
                expr.compile(codeBlock);
                codeBlock.emit(new Pop(expr.type().get()));
            }
            return codeBlock;
        });

        return new Program(classes, topLevelCode);
    }

    private CompileResult compile() throws CompilationException {
        CompiledClass runtimeClass = createRuntime();
        List<CompiledClass> otherClasses = new ArrayList<>();
        otherClasses.add(createFiles());
        for (GeneratedClass generatedClass : generatedClasses)
            otherClasses.add(generatedClass.compile());
        return new CompileResult(runtimeClass, otherClasses);
    }

    public SnuggleInstance compileToInstance() throws CompilationException {
        return new SnuggleInstance(compile());
    }


    public void compileToJar(File targetFile) throws IOException, CompilationException {

        FileOutputStream fos = new FileOutputStream(targetFile);
        Manifest manifest = new Manifest();
        JarOutputStream jos = new JarOutputStream(fos, manifest);

        CompileResult res = compile();

        for (CompiledClass compiled : ListUtils.join(List.of(res.runtime), res.otherClasses)) {
            //Add the .class file to the jar
            JarEntry e = new JarEntry(compiled.name() + ".class");
            jos.putNextEntry(e);
            jos.write(compiled.bytes());
            jos.closeEntry();
        }
        jos.close();
    }

    public record CompileResult(CompiledClass runtime, List<CompiledClass> otherClasses) {}
    public record CompiledClass(String name, byte[] bytes) {}

    private CompiledClass createFiles() throws CompilationException {
        ClassVisitor filesWriter = NameHelper.generateClassWriter(NameHelper.getFilesClassName(), false);
        for (Map.Entry<String, CodeBlock> code : topLevelCode.entrySet())
            createFile(filesWriter, code.getKey(), code.getValue());
        filesWriter.visitEnd();
        ClassWriter asWriter = filesWriter instanceof ClassWriter w ? w : (ClassWriter) filesWriter.getDelegate();
        return new CompiledClass(NameHelper.getFilesClassName(), asWriter.toByteArray());
    }

    private void createFile(ClassVisitor filesWriter, String fileName, CodeBlock code) throws CompilationException {
        //Create the field
        filesWriter.visitField(
                Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC,
                NameHelper.getImportFieldName(fileName),
                "Z",
                null, null
        ).visitEnd();
        //Create the method
        MethodVisitor methodVisitor = filesWriter.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                NameHelper.getImportMethodName(fileName),
                "()V",
                null,
                null
        );
        methodVisitor.visitCode();
        //Emit bytecode into the method
        code.writeJvmBytecode(methodVisitor);
        //Return
        methodVisitor.visitInsn(Opcodes.RETURN);
        //End the method
        methodVisitor.visitMaxs(0, 0); //Auto compute
        methodVisitor.visitEnd();
    }


    private CompiledClass createRuntime() {
        ClassVisitor runtimeWriter = NameHelper.generateClassWriter(NameHelper.getRuntimeClassName(), true, SnuggleRuntime.class);
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
        String filesClass = NameHelper.getFilesClassName();
        runMethod.visitInsn(Opcodes.ICONST_1);
        runMethod.visitFieldInsn(Opcodes.PUTSTATIC, filesClass, NameHelper.getImportFieldName("main"), "Z");
        runMethod.visitMethodInsn(Opcodes.INVOKESTATIC, filesClass, NameHelper.getImportMethodName("main"), "()V", false);
        runMethod.visitInsn(Opcodes.RETURN);
        runMethod.visitMaxs(0, 0); //Auto compute
        runMethod.visitEnd();

        //Default constructor is needed in order to instantiate it
        ClassWriter asWriter = runtimeWriter instanceof ClassWriter w ? w : (ClassWriter) runtimeWriter.getDelegate();
        return new CompiledClass(NameHelper.getRuntimeClassName(), asWriter.toByteArray());
    }

}
