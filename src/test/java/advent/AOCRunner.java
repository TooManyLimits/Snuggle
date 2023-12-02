package advent;

import builtin_types.BuiltinTypes;
import exceptions.compile_time.CompilationException;
import exceptions.runtime.SnuggleException;
import runtime.SnuggleInstance;
import util.CompileAll;

import java.io.File;
import java.util.Map;

public class AOCRunner {

    public static void test(String main) throws CompilationException, SnuggleException {
        test(BuiltinTypes.standard(), Map.of("main", main), null);
    }

    public static void test(String main, File file) throws CompilationException, SnuggleException {
        test(BuiltinTypes.standard(), Map.of("main", main), file);
    }

    //    public void test(BuiltinTypes topLevelTypes, @Language("TEXT") str main) throws CompilationException, SnuggleException {
    public static void test(BuiltinTypes types, String main) throws CompilationException, SnuggleException {
        test(types, Map.of("main", main), null);
    }

    public static void test(Map<String, String> files) throws CompilationException, SnuggleException {
        test(BuiltinTypes.standard(), files, null);
    }

    public static void test(BuiltinTypes types, Map<String, String> files, File export) throws CompilationException, SnuggleException {
        try {
            var before = System.nanoTime();
            var instance = CompileAll.compileAllToInstance(types, files);
            var after = System.nanoTime();
            System.out.println("Compilation took " + (after - before) / 1000000 + " ms");
            before = after;
            SnuggleInstance.INSTRUCTIONS = 0;
            instance.run();
            after = System.nanoTime();
            System.out.println("Running took " + (after - before) / 1000000 + " ms");
            System.out.println("Cost was " + SnuggleInstance.INSTRUCTIONS);
            SnuggleInstance.INSTRUCTIONS = 0; //reset just in case
            if (export != null)
                CompileAll.compileAllToJar(export, types, files);
        } catch (CompilationException | SnuggleException | RuntimeException e) {
            // propagate exceptions
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
