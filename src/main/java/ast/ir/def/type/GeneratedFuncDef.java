package ast.ir.def.type;

import ast.ir.def.GeneratedField;
import ast.ir.def.Program;
import ast.ir.def.method.GeneratedMethod;
import ast.ir.helper.NameHelper;
import ast.typed.def.type.FuncTypeDef;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import util.ListUtils;

import java.util.List;
import java.util.Objects;

public record GeneratedFuncDef(String name, List<GeneratedMethod> methods) implements GeneratedType {

    public static GeneratedFuncDef of(FuncTypeDef funcTypeDef) throws CompilationException {
        return new GeneratedFuncDef(
                funcTypeDef.runtimeName(),
                ListUtils.filter(ListUtils.map(
                        funcTypeDef.methods(),
                        GeneratedMethod::of
                ),      Objects::nonNull)
        );
    }

    //Compiles to an interface
    @Override
    public Program.CompiledClass compile() throws CompilationException {
        ClassVisitor writer = NameHelper.generateInterfaceWriter(name);
        for (GeneratedMethod method : methods)
            method.compile(writer);

        ClassWriter asWriter = writer instanceof ClassWriter w ? w : (ClassWriter) writer.getDelegate();
        return new Program.CompiledClass(name, asWriter.toByteArray());
    }
}
