import builtin_types.BuiltinTypes;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.ParsingException;
import exceptions.compile_time.TooManyMethodsException;
import exceptions.compile_time.TypeCheckingException;
import exceptions.runtime.SnuggleException;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import runtime.SnuggleInstance;
import runtime.SnuggleRuntime;
import util.CompileAll;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class SnuggleTests {

    @ParameterizedTest(name = ParameterizedTest.ARGUMENTS_PLACEHOLDER)
    @ValueSource(strings = {
            "algorithms/curve25519",
            "algorithms/fibfast",
            "algorithms/fib",
            "algorithms/md5",

            "array/simple",

            "control_flow/if",
            "control_flow/while",

            "uncategorized/2classes",
            "uncategorized/args",
            "uncategorized/assignment",
            "uncategorized/blocks",
            "uncategorized/extension",
            "uncategorized/field",
            "uncategorized/float",
            "uncategorized/generic",
            "uncategorized/i2l",
            "uncategorized/int_arithmetic",
            "uncategorized/int_prints",
            "uncategorized/test1",
            "uncategorized/this"
    })
    public void testCasesSimple(String testCase) throws CompilationException, SnuggleException {
        var files = new HashMap<String, String>();
        var root = Path.of("src/test/resources/case/" + testCase);

        try {
            Files.walkFileTree(root, new FileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    var name = root.relativize(file).toString();
                    name = name.substring(0, name.indexOf('.'));
                    files.put(name, Files.readString(file));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.TERMINATE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        test(new BuiltinTypes(), files);
    }

    @Test
    public void testImportsAgain() throws CompilationException, SnuggleException {
        test(Map.of("main", """
                import "lib"
                System.print(new Getter().get())
                
                """, "lib", """
                pub class Getter {
                    pub fn new() super()
                    pub fn get(): i32 10
                }
                
                """));
    }

    @Test
    public void testFunFakeVarargs() throws CompilationException, SnuggleException {
        test(
                """
                class List<T> {
                    var backing: Array<T>
                    var size: u32
                    fn new() {
                        super()
                        this.backing = new Array<T>(5)
                        this.size = 0;
                    }
                    fn push(elem: T) {
                        this.backing.set(this.size, elem)
                        this.size = this.size + 1;
                        if this.size == this.backing.len() {
                            var newBacking = new Array<T>(this.size * 2);
                            var i: u32 = 0
                            while i < this.backing.len() {
                                newBacking.set(i, this.backing.get(i))
                                i = i + 1;
                            }
                            this.backing = newBacking;
                        };
                    }
                    fn get(index: u32): T this.backing.get(index)
                    fn size(): u32 this.size
                    fn backingSize(): u32 this.backing.len()
                }
                
                class FakeVarargsPrinter<T> {
                    var elems: List<T>
                    fn new(firstElem: T) {
                        super()
                        this.elems = new List<T>()
                        this(firstElem);
                    }
                    fn invoke(elem: T): FakeVarargsPrinter<T> {
                        this.elems.push(elem)
                        this
                    }
                    fn invoke() {
                        var i = 0u32
                        while i < this.elems.size() {
                            System.print(this.elems.get(i))
                            i = i + 1;
                        };
                    }
                }
                
                new FakeVarargsPrinter<i32>(1)(3)(5)(7)(9)()
                """);
    }

    @Test
    public void testIfOption() {
        assertThrows(SnuggleException.class, () -> test("""
                var x = if true "hi";
                System.print(x.get())

                var z = 1i32
                var a = while z < 4 {
                    z = z + 1
                    "something a little silly"
                }
                System.print(a.get())

                var b = while false "lol"
                System.print(b.get("while loop never ran"))

                var y = if false "hi 2";
                System.print(y.get("if expression didnt happen :<"))
                """));
    }

    @Test
    public void testOption() {
        assertThrows(SnuggleException.class, () -> test("""
                var x = new String?()
                var y = new String?("hi")
                
                //x.get() //error, x is empty
                x.get("Silly custom error message") //error, x is empty
                
                System.print(y.get())
                System.print(y.get("Could not get value to print"))
                """));
    }

    @Test
    public void testImports() {
        assertThrows(ClassCastException.class, () -> test(Map.of(
                "main",
                """
                        if 1 < 2 {
                            import "cutie";
                            new Cutie().good()
                        } else;
                        //new Cutie().good() //type check error, type Cutie doesn't exist here
                        """,
                "cutie",
                """
                        pub class Cutie {
                            pub fn new() super()
                            fn bad() System.print(new Obj() as Cutie)
                            pub fn good() System.print("good cutie :D")
                        }
                        new Cutie().bad() //error!
                        System.print("Cutie!")
                        """
        )));
    }

    @Test
    public void testStackOverflow() {
        assertThrows(StackOverflowError.class, () -> test("""
                class death {
                    fn new() super()
                    fn get() this.get()
                }
                new death().get()
                """));
    }

    @Test
    public void testObjectCast() {
        assertThrows(ClassCastException.class, () -> test("""
                class A {fn new() super()}
                class B: A {fn new() super()}
                
                class DoBad {
                    fn new() super()
                    fn thing() {
                        System.print(new A() as B)
                    }
                    fn indirectThing() {
                        this
                            .
                                thing
                                    ()
                    }
                }
                
                var a = new A();
                var b = new B();
                
                System.print(b as A)
                new DoBad().indirectThing() //error
                """));
    }

    @Test
    public void testNumberCast() throws CompilationException, SnuggleException {
        test("""
                var x = 150i64
                var y = x as u8
                var z = x as f64 + 0.56
                Test.assertEquals(150u8, y) // todo: looks like masking isn't done to @Unsigned? idk
                Test.assertEquals(150.56f64, z)
                """);
    }

    @Test
    public void testList() throws CompilationException, SnuggleException {
        test("""
                class List<T> {
                    var backing: Array<T>
                    var size: u32
                    fn new() {
                        super()
                        this.backing = new Array<T>(5)
                        this.size = 0;
                    }
                    fn push(elem: T) {
                        this.backing.set(this.size, elem)
                        this.size = this.size + 1;
                        if this.size == this.backing.len() {
                            var newBacking = new Array<T>(this.size * 2);
                            var i: u32 = 0
                            while i < this.backing.len() {
                                newBacking.set(i, this.backing.get(i))
                                i = i + 1;
                            }
                            this.backing = newBacking;
                        } else {}
                    }
                    fn get(index: u32): T this.backing.get(index)
                    fn size(): u32 this.size
                    fn backingSize(): u32 this.backing.len()
                }
                
                var a = new List<u32>()
                a.push(1)
                a.push(3)
                a.push(5)
                a.push(2)
                a.push(7)
                a.push(4)
                
                var i: u32 = 0
                while i < a.backingSize() {
                    System.print(a.get(i))
                    i = i + 1;
                }
                
                """);
    }

    @Test
    public void testOverloads() throws CompilationException, SnuggleException {
        var types = new BuiltinTypes()
                .reflect(TestReflectedClass.class);
        test(types, """
                class Silly {
                    fn new() super()
                    fn invoke() {
                        var x: i32 = 100
                        System.print(x)
                    }
                    fn invoke(x: u32) System.print(x)
                    fn add(x: u32): u32
                        x + 1
                }
                new Silly()()
                new Silly()(10)
                System.print(new Silly() + 3)
                System.print(TestClass + 10)
                """);
    }

    @Test
    public void testMultipleCallable() {
        assertThrows(TooManyMethodsException.class, () -> test("""
                class Case {
                    fn new() super()
                    fn a(): i32 1
                    fn a(): i64 2
                    //try commenting out one of the b()
                    fn b(x: i32) System.print(x)
                    fn b(x: i64) System.print(x)
                }
                var x = new Case()
                x.b(x.a()) //doesn't know which b() to call
                """));
    }

    @Test
    public void testMultipleReturn() {
        assertThrows(TypeCheckingException.class, () -> test("""
                class Case {
                    fn new() super()
                    fn a(): i32 1
                    fn a(): i64 2
                }
                var x: i32 = new Case().a() //1
                var y: i64 = new Case().a() //2
                var z: u32 = new Case().a() //error, no method a() works
                
                System.print(x)
                System.print(y)
                System.print(z)
                """));
    }

    @Test
    public void testReflectionExtend() throws CompilationException, SnuggleException {
        var types = new BuiltinTypes()
                .reflect(TestReflectedClass.class)
                .reflect(TestReflectedClass2.class);
        test(types, """
                var x: TestClass = TestClass.get()
                var y: i32 = x.beeg()
                System.print(y)
                var z: u32 = x.beeg()
                System.print(z)
                
                var w = TestClass2.get()
                System.print(y = w.beeg())
                System.print(z = w.beeg())
                """);
    }

    @Test
    public void testReflection() throws CompilationException, SnuggleException {
        var types = new BuiltinTypes()
                .reflect(TestReflectedClass.class);
        test(types, """
                TestClass.printSilly()
                TestClass.printSilly()
               
                var a = TestClass.get()
                var x: i32 = a.beeg()
                var y: u32 = a.beeg()
                System.print(x)
                System.print(y)
                
                """);
    }

    @Test
    public void testMoreIntMath() throws CompilationException, SnuggleException {
        test("""
                var y: i32 = 5
                System.print(y % 3)
                System.print(~y)
                System.print(-y)
                System.print(y / y)
                System.print(y & y)
                System.print(y ^ y)
                
                var x: i32 = ~10
                System.print(x)
                """);
    }

    @Test
    public void testI84() throws CompilationException, SnuggleException {
        assertThrows(ParsingException.class, () -> test("""
                class i84 {
                    pub fn new() super()
                    pub fn get(): i8 4
                }
                
                class Other {
                    pub fn new() super()
                    pub fn meow(): i8 4
                        new i84()
                }
                
                System.print(new Other().meow().get())
                """));
    }

//    public void test(@Language("TEXT") String main) throws CompilationException, SnuggleException {
    public void test(String main) throws CompilationException, SnuggleException {
        test(new BuiltinTypes(), Map.of("main", main));
    }

//    public void test(BuiltinTypes types, @Language("TEXT") String main) throws CompilationException, SnuggleException {
    public void test(BuiltinTypes types, String main) throws CompilationException, SnuggleException {
        test(types, Map.of("main", main));
    }

    public void test(Map<String, String> files) throws CompilationException, SnuggleException {
        test(new BuiltinTypes(), files);
    }

    public void test(BuiltinTypes types, Map<String, String> files) throws CompilationException, SnuggleException {
        types.reflect(TestBindings.class);

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
        } catch (CompilationException | SnuggleException | RuntimeException e) {
            // propagate exceptions
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
