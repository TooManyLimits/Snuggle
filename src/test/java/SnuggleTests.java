import builtin_types.BuiltinTypes;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.ParsingException;
import exceptions.compile_time.TooManyMethodsException;
import exceptions.compile_time.TypeCheckingException;
import exceptions.runtime.SnuggleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import runtime.SnuggleInstance;
import util.CompileAll;

import java.io.File;
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
//            "algorithms/curve25519", //System methods for casting and shifting are gone
            "algorithms/fibfast",
            "algorithms/fib",
//            "algorithms/md5", //System methods for casting and shifting are gone

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
//            "uncategorized/i2l", //System methods for casting and shifting are gone
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

        test(new BuiltinTypes(), files, null);
    }

    @Test
    public void testCrimes() throws CompilationException, SnuggleException {
        test("""
                class Crime {
                    static var val: i32
                    static fn get(): i32
                        Crime.val
                    static fn subAssign(x: i32): i32
                        Crime.val -= x
                    static fn set(x: i32): i32
                        Crime.val = x * x
                    static fn get(a: i32, b: i32): i32
                        a * b
                    static fn invoke(x: i32): i32
                        x * x * x
                }
                
                Test.assertEquals(200, Crime[10, 20])
                Crime[] = 15
                Test.assertEquals(225, Crime[])
                Crime -= 10
                Test.assertEquals(215, Crime[])
                Test.assertEquals(64, Crime(4))
                """);
    }

    @Test
    public void testEvenShorterList() throws CompilationException, SnuggleException {
        test("""
                class List<T> {
                    var backing: Array<T>
                    var size: u32
                    fn new() {
                        super()
                        backing = new Array<T>(5)
                        size = 0;
                    }
                    fn addAssign(elem: T): List<T> {
                        backing[size] = elem
                        size += 1
                        if size == #backing {
                            var newBacking = new Array<T>(size * 2)
                            var i: u32 = 0
                            while i < #backing {
                                newBacking[i] = backing[i]
                                i += 1
                            }
                            backing = newBacking
                        }
                        this
                    }
                    fn get(index: u32): T backing[index]
                    fn size(): u32 size
                    fn backingSize(): u32 #backing
                }
                
                var a = new List<u32>()
                a += 1
                a += 3
                a += 5
                a += 2
                a += 7
                a += 4
                
                var i: u32 = 0
                while i < #a {
                    System.print(a[i])
                    i += 1
                }
                
                Test.assertEquals(1, a[0])
                Test.assertEquals(3, a[1])
                Test.assertEquals(5, a[2])
                Test.assertEquals(2, a[3])
                Test.assertEquals(7, a[4])
                Test.assertEquals(4, a[5])
                """);
    }

    @Test
    public void testAugmentedBasic() throws CompilationException, SnuggleException {
        test("""
                var x = 10u32
                System.print(x)
                x += 10
                System.print(x)
                Test.assertEquals(20, x)
                """);
    }

    @Test
    public void testTruthy() throws CompilationException, SnuggleException {
        test("""
                struct GreaterThanTen {
                    var value: f32
                    fn bool(): bool
                        value > 10
                }
                
                var x = new GreaterThanTen { 5 }
                var y = new GreaterThanTen { 15 }
                System.print(if x "x is over 10" else "x is under 10")
                System.print(if y "y is over 10" else "y is under 10")
                Test.assertFalse(x.bool())
                Test.assertFalse(if x true else false)
                Test.assertTrue(y.bool())
                Test.assertTrue(if y true else false)
                """);
    }

    @Test
    public void testIsSubtype() throws CompilationException, SnuggleException {
        test("""
                struct Vec2 {var x: f32 var y: f32}
                class IsMyGenericI32<T> {
                    fn new() super()
                    fn isIt(): bool
                        is T i32
                }
                class IsSameGeneric<A, B> {
                    fn new() super()
                    fn isSame(): bool
                        is A B && is B A
                }
                
                Test.assertTrue(is String Obj)
                Test.assertFalse(is Obj String)
                Test.assertFalse(is i64 i32)
                Test.assertTrue(is Array<Array<String>> Obj)
                Test.assertFalse(is Array<Vec2> Obj)
                Test.assertFalse(new IsMyGenericI32<f32>().isIt())
                Test.assertTrue(new IsMyGenericI32<i32>().isIt())
                Test.assertFalse(new IsSameGeneric<Obj, String>().isSame())
                Test.assertFalse(new IsSameGeneric<String, Obj>().isSame())
                Test.assertTrue(new IsSameGeneric<Vec2, Vec2>().isSame())
                """);
    }

    @Test
    public void testBitShifts() throws CompilationException, SnuggleException {
        test("""
                var x: i32 = 10
                Test.assertEquals(40, x << 2)
                Test.assertEquals(1, x >> 3)
                System << "its c++ time"
                """);
    }

    @Test
    public void testStructArrays() throws CompilationException, SnuggleException {
        test("""
                struct Vec3 {
                    var x: f32
                    var y: f32
                    var z: f32
                    fn eq(o: Vec3): bool
                        x == o.x && y == o.y && z == o.z
                    fn add(o: Vec3): Vec3
                        new Vec3 { x + o.x, y + o.y, z + o.z }
                    fn str(): String
                        x.str() + ", " + y.str() + ", " + z.str()
                }
                
                var arr = new Array<Vec3>(4)
                arr.set(0, new Vec3 {10, 20, 30})
                arr.set(1, new Vec3 {40, 50, 60})
                arr.set(3, new Vec3 {100, 110, 120})
                arr.set(2, new Vec3 {70, 80, 90})
                
                System.print(arr.get(0).x)
                System.print({arr.get(2) + arr.get(3)}.str())
                Test.assertTrue(arr.get(0) + arr.get(1) == new Vec3 {50, 70, 90})
                """);
    }

    @Test
    public void testValueTypeOptions() throws CompilationException, SnuggleException {
        test("""
                class GetMaybe {
                    fn new() super()
                    fn get(x: u64): u64?
                        if x % 6828 < 3345
                            new u64?(x % 6828)
                        else
                            new u64?()
                }
                var maybe = new GetMaybe()
                
                var perhaps = maybe.get(62896882877)
                System.print(if perhaps.isPresent()
                        "It was! The value is " + perhaps.get().str()
                    else
                        "It was not :( No value")
                
                perhaps = maybe.get(20978632037)
                System.print(if perhaps.isPresent()
                        "It was! The value is " + perhaps.get().str()
                    else
                        "It was not :( No value")
                
                """);
    }

    @Test
    public void testReferenceTypeOption() throws CompilationException, SnuggleException {
        test("""
                class A {
                    var x: String?
                    var y: B?
                    fn new() {
                        super()
                        x = new String?("i am an A")
                        y = new B?(new B());
                    }
                }
                class B {
                    var y: String?
                    fn new() {
                        super()
                        y = new String?("i am a B");
                    }
                }
                
                System.print(new A().y.get().y.get())
                """);
    }

    @Test
    public void testReturn() throws CompilationException, SnuggleException {
        test("""
                class Tester {
                    fn new() super()
                    fn abs(input: i32): i32 {
                        if input < 0 {
                            return -input;
                        }
                        return input
                    }
                }
                var t = new Tester()
                Test.assertEquals(10, t.abs(-10))
                Test.assertEquals(50, t.abs(50))
                """);
    }

    @Test
    public void testShorterList() throws CompilationException, SnuggleException {
        //Test field access syntax with implicit "this."
        test("""
                class List<T> {
                    var backing: Array<T>
                    var size: u32
                    fn new() {
                        super()
                        backing = new Array<T>(5)
                        size = 0;
                    }
                    fn push(elem: T) {
                        backing.set(size, elem)
                        size = size + 1
                        if size == #backing {
                            var newBacking = new Array<T>(size * 2)
                            var i: u32 = 0
                            while i < #backing {
                                newBacking.set(i, backing.get(i))
                                i = i + 1
                            }
                            backing = newBacking
                        };
                    }
                    fn get(index: u32): T backing.get(index)
                    fn size(): u32 size
                    fn backingSize(): u32 #backing
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
                    i = i + 1
                }
                
                Test.assertEquals(1, a.get(0))
                Test.assertEquals(3, a.get(1))
                Test.assertEquals(5, a.get(2))
                Test.assertEquals(2, a.get(3))
                Test.assertEquals(7, a.get(4))
                Test.assertEquals(4, a.get(5))
                
                """);
    }

    @Test
    public void testNestedStruct() throws CompilationException, SnuggleException {
        test("""
                struct Nester {
                    var x: f64
                    var y: Nested
                    var z: u32
                }
                struct Nested {
                    var a: i64
                    var b: f32
                }
                
                var c = new Nester {
                    x = 10,
                    y = new Nested {
                        a = 11,
                        b = 12
                    },
                    z = 13
                }
                System.print(c.y.b)
                """);
    }

    @Test
    public void testBox() throws CompilationException, SnuggleException {
        test("""
                class Box<T> {
                    var value: T
                    fn new(v: T) {
                        super()
                        this.value = v;
                    }
                }
                struct Thing {
                    var thing: u32
                }
                
                class Whatnot {
                    var elem: Box<Thing>
                    fn new() {
                        super()
                        elem = new Box<Thing>(new Thing{30});
                    }
                    fn getThing(): Thing {
                        elem.value
                    }
                    fn getBoxed(): Box<Thing> {
                        elem
                    }
                }
                
                var x = new Thing { 10 }
                Test.assertEquals(10, x.thing)
                x.thing = 5
                Test.assertEquals(5, x.thing)
                
                var y = new Box<Thing>(new Thing { 50 })
                Test.assertEquals(50, y.value.thing)
                y.value.thing = 100
                Test.assertEquals(100, y.value.thing)
                
                var z = new Whatnot()
                Test.assertEquals(30, z.getThing().thing)
                Test.assertEquals(30, z.getBoxed().value.thing)
                //z.getThing().thing = 50 //Error
                //Test.assertEquals(30, z.getThing().thing)
                z.getBoxed().value.thing = 50
                Test.assertEquals(50, z.getThing().thing)
                
                """);
    }

    @Test
    public void testStruct() throws CompilationException, SnuggleException {
        test("""
                struct Vec3 {
                    var x: f32
                    var y: f32
                    var z: f32
                    fn new(x: f32, y: f32, z: f32)
                        new Vec3 { x, y, z }
                    fn str(): String
                        "{" + x.str() + ", " + y.str() + ", " + z.str() + "}"
                    fn add(o: Vec3): Vec3
                        new Vec3 {x + o.x, y + o.y, z + o.z}
                }
                System.print(new Vec3 {1, 2, 3}.str())
                Test.assertEquals("{1.0, 2.0, 3.0}", new Vec3 {1, 2, 3}.str())
                System.print(new Vec3 { 16, 4, 10 }.z)
                Test.assertEquals(10, new Vec3 { 16, 4, 10 }.z)
                System.print({new Vec3 {2f32 * 3 + 1, 1, -10f32} + new Vec3(4, 10, 21)}.str())
                Test.assertEquals("{11.0, 11.0, 11.0}", {new Vec3 {2f32 * 3 + 1, 1, -10f32} + new Vec3(4, 10, 21)}.str())
                """, null); //new File("TestStruct.jar"));
    }

    @Test
    public void testLeapYear() throws CompilationException, SnuggleException {
        test("""
                class Leap {
                    fn new() super()
                    fn check(year: u32): bool
                        year % 4 == 0 && year % 100 != 0 || year % 400 == 0
                }
                var l = new Leap()
                Test.assertFalse(l.check(3))
                Test.assertTrue(l.check(16))
                Test.assertTrue(l.check(800))
                Test.assertFalse(l.check(700))
                """);
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
    public void testFunFakeVarargs() throws CompilationException, SnuggleException, IOException {
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
                        if this.size == this.backing.size() {
                            var newBacking = new Array<T>(this.size * 2);
                            var i: u32 = 0
                            while i < this.backing.size() {
                                newBacking.set(i, this.backing.get(i))
                                i = i + 1;
                            }
                            this.backing = newBacking;
                        };
                    }
                    fn get(index: u32): T this.backing.get(index)
                    fn size(): u32 this.size
                    fn backingSize(): u32 this.backing.size()
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
                """); //, new File("FunFakeVarargs.jar"));
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
                        if this.size == this.backing.size() {
                            var newBacking = new Array<T>(this.size * 2);
                            var i: u32 = 0
                            while i < this.backing.size() {
                                newBacking.set(i, this.backing.get(i))
                                i = i + 1;
                            }
                            this.backing = newBacking;
                        } else {}
                    }
                    fn get(index: u32): T this.backing.get(index)
                    fn size(): u32 this.size
                    fn backingSize(): u32 this.backing.size()
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
                
                Test.assertEquals(1, a.get(0))
                Test.assertEquals(3, a.get(1))
                Test.assertEquals(5, a.get(2))
                Test.assertEquals(2, a.get(3))
                Test.assertEquals(7, a.get(4))
                Test.assertEquals(4, a.get(5))
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
        test(new BuiltinTypes(), Map.of("main", main), null);
    }

    public void test(String main, File file) throws CompilationException, SnuggleException {
        test(new BuiltinTypes(), Map.of("main", main), file);
    }

//    public void test(BuiltinTypes types, @Language("TEXT") String main) throws CompilationException, SnuggleException {
    public void test(BuiltinTypes types, String main) throws CompilationException, SnuggleException {
        test(types, Map.of("main", main), null);
    }

    public void test(Map<String, String> files) throws CompilationException, SnuggleException {
        test(new BuiltinTypes(), files, null);
    }

    public void test(BuiltinTypes types, Map<String, String> files, File export) throws CompilationException, SnuggleException {
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
