package util;

import ast.ir.def.Program;
import ast.parsed.prog.ParsedAST;
import ast.passes.GenericVerifier;
import ast.passes.Parser;
import ast.passes.TypeChecker;
import ast.passes.TypeResolver;
import ast.type_resolved.prog.TypeResolvedAST;
import ast.typed.def.type.TypeDef;
import ast.typed.prog.TypedAST;
import builtin_types.BuiltinTypes;
import exceptions.compile_time.CompilationException;
import lexing.Lexer;
import runtime.SnuggleInstance;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class that performs all the compilation steps from start to finish.
 */
public class CompileAll {

    /**
     * Keys are file names.
     * Values are source code.
     */
    public static SnuggleInstance compileAllToInstance(BuiltinTypes types, Map<String, String> files) throws CompilationException {
        //1. Create the lexers
        Map<String, Lexer> lexers = new HashMap<>();
        for (var file : files.entrySet())
            lexers.put(file.getKey(), new Lexer(file.getKey(), file.getValue()));
        //2. Parse to ParsedAST
        ParsedAST parsedAST = Parser.parse(lexers);
        System.out.println(parsedAST);
        //3. Resolve topLevelTypes to TypeResolvedAST
        TypeResolvedAST typeResolvedAST = TypeResolver.resolve(types, parsedAST);
        System.out.println(typeResolvedAST);
        //4. Verify generics
        GenericVerifier.verifyGenerics(typeResolvedAST);
        //5. Type check to TypedAST
        TypedAST typedAST = TypeChecker.type(typeResolvedAST);
        for (TypeDef d : typedAST.typeDefs())
            System.out.println(d);
        //6. Compile to instance and return
        return Program.of(typedAST).compileToInstance();
    }

    public static void compileAllToJar(File targetFile, BuiltinTypes types, Map<String, String> files) throws CompilationException, IOException {
        //1. Create the lexers
        Map<String, Lexer> lexers = new HashMap<>();
        for (var file : files.entrySet())
            lexers.put(file.getKey(), new Lexer(file.getKey(), file.getValue()));
        //2. Parse to ParsedAST
        ParsedAST parsedAST = Parser.parse(lexers);
        System.out.println(parsedAST);
        //3. Resolve topLevelTypes to TypeResolvedAST
        TypeResolvedAST typeResolvedAST = TypeResolver.resolve(types, parsedAST);
        //4. Verify generics
        GenericVerifier.verifyGenerics(typeResolvedAST);
        //5. Type check to TypedAST
        TypedAST typedAST = TypeChecker.type(typeResolvedAST);
//        for (TypeDef d : typedAST.topLevelTypeDefs())
//            System.out.println(d);
        //6. Compile to instance and return
        Program.of(typedAST).compileToJar(targetFile);
    }

}
