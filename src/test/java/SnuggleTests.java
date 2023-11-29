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
    public void testExtensionMethodImport() throws CompilationException, SnuggleException {
        test(Map.of("main", """
                import "lib" //Get the extension method
                7u32.printSquared() //print 49
                
                """, "lib", """
                //Removing "pub" should make this not work
                pub fn printSquared(this: u32)
                    System.print(this * this)
                """));
    }

    @Test
    public void testForLoop() throws CompilationException, SnuggleException {
        test("""
                //Original test
                var x = new List<i32>()
                x += 1
                x += 4
                x += 9
                for a: i32 in x System.print(a)
                
                //Extension methods, allow things like:
                //for a: i32 in x.iter()
                //for a: i32 in x.iter().indexed()
                
                //All iterators can now call .iter() and just return themselves
                pub fn iter<T>(this: () -> T?): () -> T? this
                
                //Can call .indexed() on an iterator to return a new iterator which is a pair of the element and its index
                pub fn indexed<T>(this: () -> T?): () -> (T, u32)? {
                    var i: Box<u32> = new(0); //store the current index
                    //return a new closure
                    () -> {
                        var t = this() //Call the base iterator
                        if t {
                            //If the original iterator was present, return a present tuple
                            *i += 1; //increment i
                            new((*t, *i-1)) //return a tuple of the original and the current index
                        } else {
                            //Return empty
                            new()
                        }
                    }
                }
                
                for a: i32 in x.iter() System.print(a)
                //This'll be better once we have pattern matching
                for pair: (i32, u32) in x.iter().indexed() System.print(pair.v1.str() + ": " + pair.v0.str())
                
                """);
    }

    @Test
    public void testSillyBindOperator() throws CompilationException, SnuggleException {
        test("""
                // silly little >>= operator overload
                pub fn shrAssign<T, R>(this: T?, func: T -> R?): R?
                    if this func(*this) else new()
                
                var a: str? = new("i am present")
                var b: str? = new() //i am not
                
                var alen: u32? = a >>= x -> new(#x)
                var blen: u32? = b >>= x -> new(#x)
                
                if alen System.print(*alen)
                if blen System.print(*blen)
                
                """);
    }

    @Test
    public void testMethodGenericInference() throws CompilationException, SnuggleException {
        test("""
                var x: List<i32> = new()
                x += 1
                x += 2
                x += 3
                
                x.map(i -> i.str()).forEach(i -> System.print("\\"" + i + "\\""))
                """);
    }

    @Test
    public void testExtensionMethod() throws CompilationException, SnuggleException {
        test("""
                fn mul(this: str, count: u32): str {
                    if count == 0 return ""
                    var res = this
                    while count > 1 {
                        res += this
                        count -= 1
                    }
                    res
                }
                
                System.print("hi" * 10)
                Test.assertEquals("CutieCutieCutie", "Cutie" * 3)
                """);
    }

    @Test
    public void testEvent() throws CompilationException, SnuggleException {
        test("""
                class Event<InputType> : List<InputType -> ()> {
                    pub fn new() super()
                    pub fn invoke(input: InputType)
                        this.forEach(e -> e(input))
                }
                
                var myEvent = new Event<f32>()
                myEvent += x -> System.print(x.str() + " is " + x.str())
                myEvent += x -> System.print(x.str() + " doubled is " + (x * 2).str())
                myEvent += x -> System.print(x.str() + " squared is " + (x * x).str())
                
                myEvent(3)
                myEvent(4)
                """);
    }

    @Test
    public void testClosure2() throws CompilationException, SnuggleException {
        test("""
                fn incrementor(): () -> i32 {
                    var x: Box<i32> = new(0);
                    () -> *x += 1
                }
                
                var inc = incrementor()
                Test.assertEquals(1, inc())
                Test.assertEquals(2, inc())
                Test.assertEquals(3, inc())
                """);
    }

    @Test
    public void testClosure() throws CompilationException, SnuggleException {
        test("""
                fn add(x: i32): i32 -> i32
                    y -> x + y
                    
                var add5 = add(5)
                Test.assertEquals(15, add5(10))
                Test.assertEquals(35, add5(add(10)(20)))
                """);
    }

    @Test
    public void testTopLevelFunctions() throws CompilationException, SnuggleException {
        test("""
                fn square(x: i32): i32 x * x
                fn genericSquare<T>(x: T): T x * x
                
                System.print(square(5))
                System.print(square(7))
                System.print(genericSquare(10.5f32))
                //System.print(genericSquare.invoke::<str>("hi")) //error, genericSquare<str> doesn't work
                
                Test.assertEquals(10001, square(square(10)) + 1)
                """);

    }

    @Test
    public void testFunnierTopLevelFunctions() throws CompilationException, SnuggleException {
        test("""
                struct square {
                    static fn invoke(x: i32): i32 x * x
                }
                
                System.print(square(5))
                System.print(square(7))
                
                Test.assertEquals(10001, square(square(10)) + 1)
                """);

    }

    @Test
    public void testLambdas() throws CompilationException, SnuggleException {
        test("""
                var l = new List<i32>()
                l += 1
                l += 2
                l += 3
                l += 4
                
                var c = l.map(x -> x.str());
                Test.assertEquals("1", c[0])
                Test.assertEquals("2", c[1])
                Test.assertEquals("3", c[2])
                Test.assertEquals("4", c[3])
                
                //explicit generic isn't necessary, just showing how it works
                var d = l.map::<i32>(x => x * x);
                Test.assertEquals(1, d[0])
                Test.assertEquals(4, d[1])
                Test.assertEquals(9, d[2])
                Test.assertEquals(16, d[3])
                
                var quoteWrap: str -> str = e -> "\\"" + e + "\\""
                c.map(quoteWrap).forEach(e -> System.print(e))
                d.forEach(e -> System.print(e))
                """);
    }

    @Test
    public void testMethodGenerics() throws CompilationException, SnuggleException {
        test("""
                class Mapper<T, R> {
                    var default: R //used because we have no abstract/interface
                    fn new(e: R) {
                        super()
                        default = e;
                    }
                    fn invoke(e: T): R default //need abstract/interface stuff eventually, currently default impl
                }
                
                class FunnyList<T> : List<T> {
                    fn new() super()
                    fn map<R>(func: Mapper<T, R>): FunnyList<R> {
                        var res = new FunnyList<R>()
                        var i = 0u32
                        while i < #this {
                            res += func(this[i])
                            i += 1;
                        }
                        res
                    }
                }
                
                class StringMapper : Mapper<i32, str> {
                    fn new() super("")
                    fn invoke(x: i32): str
                        x.str()
                }
                
                class SquaringMapper<T> : Mapper<T, T> {
                    fn new() super(0)
                    fn invoke(x: T): T
                        x * x
                }
                
                var l = new FunnyList<i32>()
                l += 1
                l += 2
                l += 3
                
                var c = l.map::<str>(new StringMapper());
                Test.assertEquals("1", c[0])
                Test.assertEquals("2", c[1])
                Test.assertEquals("3", c[2])
                
                var d = l.map::<i32>(new SquaringMapper<i32>());
                Test.assertEquals(1, d[0])
                Test.assertEquals(4, d[1])
                Test.assertEquals(9, d[2])
                """);
    }

    @Test
    public void testTuples() throws CompilationException, SnuggleException {
        test("""
                class Rect {
                    var p1: (f32, f32)
                    var p2: (f32, f32)
                    
                    pub fn new(p1: (f32, f32), p2: (f32, f32)) {
                        super()
                        this.p1 = p1
                        this.p2 = p2;
                    }
                    
                    pub fn dimensions(): (f32, f32)
                        (p2.v0 - p1.v0, p2.v1 - p1.v1)
                        
                    pub fn area(): f32
                        (p2.v0 - p1.v0) * (p2.v1 - p1.v1)
                }
                
                var rect: Rect = new((10, 20), (30, 40))
                System.print(rect.dimensions().v0)
                Test.assertEquals(20, rect.dimensions().v0)
                System.print(rect.dimensions().v1)
                Test.assertEquals(20, rect.dimensions().v1)
                System.print(rect.area())
                Test.assertEquals(400, rect.area())
                """);
    }

    @Test
    public void weirdSubtyping() throws CompilationException, SnuggleException {
        test("""
                class Mapper<T, R> {
                    var default: R //used because we have no abstract/interface
                    fn new(e: R) {
                        super()
                        default = e;
                    }
                    fn invoke(e: T): R default //need abstract/interface stuff eventually, currently default impl
                }
                
                class FunnyList<T> : List<T> {
                    fn new() super()
                    fn mapstr(func: Mapper<T, str>): List<str> {
                        var res = new List<str>()
                        var i = 0u32
                        while i < #this {
                            res += func(this[i])
                            i += 1;
                        }
                        res
                    }
                }
                
                class ToStringMapper<T> : Mapper<T, str> {
                    fn new() super("")
                    fn invoke(e: T): str
                        e.str()
                }
                
                var l = new FunnyList<i32>()
                l += 1
                l += 2
                l += 3
                
                var c = l.mapstr(new ToStringMapper<i32>())
                Test.assertEquals("1", c[0])
                """);
    }

    @Test
    public void testNestedTypes() throws CompilationException, SnuggleException {
        test("""
                class Outer<X> {
                    fn new() super()
                    fn func() {
                        struct Inner<Y> {
                            var a: X
                            var b: Y
                        }
                        var inner1 = new Inner<i32> {10, 30}
                        var inner2 = new Inner<str> {15, "cutie"}
                        Test.assertEquals(10, inner1.a)
                        Test.assertEquals(30, inner1.b)
                        Test.assertEquals(15, inner2.a)
                        Test.assertEquals("cutie", inner2.b)
                        System.print("yes, this actually happened, you did call the function to test things")
                    }
                }
                new Outer<i64>().func()
                """);


    }

    @Test
    public void testCursedStaticsOnGenerics() throws CompilationException, SnuggleException {
        test("""
                class Silly<T> {
                    fn new() super()
                    fn callStaticOnGeneric()
                        T::<>.hello()
                    static fn callStaticOnGenericFromStatic()
                        T::<>.hello()
                }
                class Hello {
                    static fn hello() System.print("hello")
                }
                new Silly<Hello>().callStaticOnGeneric()
                Silly::<Hello>.callStaticOnGenericFromStatic()
                
//                //Error :( str doesn't have a static .hello()
//                new Silly<str>().callStaticOnGeneric()
                """);
    }

    @Test
    public void testComplex() throws CompilationException, SnuggleException {
        test("""
                import "complex"
                var a: Complex<f32> = new {1, 2}
                var b: Complex<f32> = new {3, 4}

                Test.assertEquals(-5, {a * b}.real)
                Test.assertEquals(10, {a * b}.imag)
                Test.assertTrue(a * b == new { -5, 10 })
                Test.assertTrue(a * b == Complex::<f32>.ONE * -5 + Complex::<f32>.I * 10)
                Test.assertEquals(5, Complex::<f32>.sumComponents(a * b))
                """);
    }

    @Test
    public void testBadExtension() throws CompilationException, SnuggleException {
        assertThrows(TypeCheckingException.class, () -> test("""
                class A: f32 {
                    fn new();
                }
                var x: f32 = new A()
                """));
    }

    @Test
    public void testBox() throws CompilationException, SnuggleException {
        test("""
                struct Vec2 {
                    var x: f32
                    var y: f32
                }
                
                class ModifyStruct {
                    static fn dontModifyIt(a: Vec2) {
                        a.y += 1; //doesn't do anything, since structs are value types
                    }
                    static fn modifyIt(a: Box<Vec2>) {
                        a.v.y += 1; //modifies it, since it's boxed
                    }
                }
                
                var x: Vec2 = new {1, 2}
                var y: Box<Vec2> = new(x)
                
                ModifyStruct.dontModifyIt(x)
                ModifyStruct.modifyIt(y)

                Test.assertEquals(2, x.y)
                Test.assertEquals(3, y.v.y)
                """);
    }

    @Test
    public void testValueTypes() throws CompilationException, SnuggleException {
        test("""
                struct Vec3<T> {
                    var x: T
                    var y: T
                    var z: T
                    pub fn add(o: Vec3<T>): Vec3<T>
                        new {x + o.x, y + o.y, z + o.z}
                    pub fn sub(o: Vec3<T>): Vec3<T>
                        new {x - o.x, y - o.y, z - o.z}
                    pub fn neg(): Vec3<T>
                        new {-x, -y, -z}
                    pub fn mul(s: T): Vec3<T>
                        new {x * s, y * s, z * s}
                    pub fn dot(o: Vec3<T>): T
                        x * o.x + y * o.y + z * o.z
                }
                
                var a: Vec3<f32> = new {4, 5, 6}
                var b = a
                a.x = 7
                Test.assertEquals(7, a.x)
                Test.assertEquals(4, b.x)
                """);
    }

    @Test
    public void testStructNewInference() throws CompilationException, SnuggleException {
        test("""
                struct Vec3<T> {
                    var x: T
                    var y: T
                    var z: T
                    pub fn add(o: Vec3<T>): Vec3<T>
                        new {x + o.x, y + o.y, z + o.z}
                    pub fn sub(o: Vec3<T>): Vec3<T>
                        new {x - o.x, y - o.y, z - o.z}
                    pub fn neg(): Vec3<T>
                        new {-x, -y, -z}
                    pub fn mul(s: T): Vec3<T>
                        new {x * s, y * s, z * s}
                    pub fn dot(o: Vec3<T>): T
                        x * o.x + y * o.y + z * o.z
                }
                
                var a: Vec3<i64> = new {1, 2, 3}
                var b: Vec3<i64> = new {5, 6, 7}
                Test.assertEquals(480, -{a + b}.dot({a - b}) * 5)
                """);
    }

    @Test
    public void testConstructorInfer() throws CompilationException, SnuggleException {
        test("""
                var x: str? = new()
                x = new("helo cutie :D")
                System.print(x[])
                Test.assertEquals(#"helo cutie :D", #x[])
                Test.assertEquals("helo cutie :D", x.get())
                """);
    }

    @Test
    public void testMaybeUninit() throws CompilationException, SnuggleException {
        test("""
                var x = new Array<MaybeUninit<str>>(1)[0]
                var y = new MaybeUninit<str>("hi")
                System.print(y[])
                System.print(x[])
                """);
    }

    @Test
    public void testStaticInitializer() throws CompilationException, SnuggleException {
        test("""
                class catplant {
                    static var e: str
                    static {
                        catplant.e = ":catplant:";
                    }
                }
                System.print(catplant.e)
                Test.assertEquals(":catplant:", catplant.e)
                """);
    }

    @Test
    public void testEnum() throws CompilationException, SnuggleException {
        test("""
                struct LittleScary {
                    var x: str //non nullable struct-nested reference type!! aaa!!
                    var y: u64
                }
                
                struct Scary {
                    var little: LittleScary
                    var z: str //AAAA!!! ANOTHER ONE!!
                }
                
                enum Day(isWeekend: bool, emotion: str, scary: Scary) {
                    SUNDAY(true, ":)", new Scary { new LittleScary { "a", 1 }, "A" })
                    MONDAY(false, ":((", new Scary { new LittleScary { "aa", 2 }, "AA" })
                    TUESDAY(false, ":(", new Scary { new LittleScary { "aaa", 3 }, "AAA" })
                    WEDNESDAY(false, ":|", new Scary { new LittleScary { "aaaa", 4 }, "AAAA" })
                    THURSDAY(false, ":o", new Scary { new LittleScary { "aaaaa", 5 }, "AAAAA" })
                    FRIDAY(false, "^w^", new Scary { new LittleScary { "aaaaaa", 6 }, "AAAAAA" })
                    SATURDAY(true, ":D", new Scary { new LittleScary { "aaaaaaa", 7 }, "AAAAAAA" })
                }
                Test.assertTrue(Day.SUNDAY.isWeekend())
                Test.assertFalse(Day.WEDNESDAY.isWeekend())
                Test.assertEquals(":(", Day.TUESDAY.emotion())
                Test.assertEquals(":D", Day.SATURDAY.emotion())
                Test.assertEquals(1, Day.MONDAY.index())
                Test.assertEquals("aaaaa", Day.THURSDAY.scary().little.x)
                Test.assertEquals("AAAAAA", Day.FRIDAY.scary().z)
                """);
    }

    @Test
    public void testNPE() {
        assertThrows(NullPointerException.class, () -> test("""
                class NPE {
                    var x: f32
                    var y: str
                    fn new() {
                        super()
                        System.print(x)
                        System.print(#y) //NPE! y is not initialized
                    }
                }
                new NPE()
                """));
    }

    @Test
    public void testCrimes() throws CompilationException, SnuggleException {
        test("""
                class Crime {
                    fn new() {
                        var x: i32 = 0
                        super()
                    }
                    fn doThing() {
                        var x: i32 = 5;
                    }
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
                
                Test.assertTrue(is str Obj)
                Test.assertFalse(is Obj str)
                Test.assertFalse(is i64 i32)
                Test.assertTrue(is Array<Array<str>> Obj)
                Test.assertFalse(is Array<Vec2> Obj)
                Test.assertFalse(new IsMyGenericI32<f32>().isIt())
                Test.assertTrue(new IsMyGenericI32<i32>().isIt())
                Test.assertFalse(new IsSameGeneric<Obj, str>().isSame())
                Test.assertFalse(new IsSameGeneric<str, Obj>().isSame())
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
                    fn str(): str
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
                    var x: str?
                    var y: B?
                    fn new() {
                        super()
                        x = new str?("i am an A")
                        y = new B?(new B());
                    }
                }
                class B {
                    var y: str?
                    fn new() {
                        super()
                        y = new str?("i am a B");
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
    public void testStruct() throws CompilationException, SnuggleException {
        test("""
                struct Vec3 {
                    var x: f32
                    var y: f32
                    var z: f32
                    fn new(x: f32, y: f32, z: f32)
                        new Vec3 { x, y, z }
                    fn str(): str
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
                var x = new str?()
                var y = new str?("hi")
                
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
    public void testOverloads() throws CompilationException, SnuggleException {
        var types = new BuiltinTypes()
                .addType(TestReflectedClass.class);
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
                .addType(TestReflectedClass.class)
                .addType(TestReflectedClass2.class);
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
                .addType(TestReflectedClass.class);
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

//    public void test(@Language("TEXT") str main) throws CompilationException, SnuggleException {
    public void test(String main) throws CompilationException, SnuggleException {
        test(new BuiltinTypes(), Map.of("main", main), null);
    }

    public void test(String main, File file) throws CompilationException, SnuggleException {
        test(new BuiltinTypes(), Map.of("main", main), file);
    }

//    public void test(BuiltinTypes topLevelTypes, @Language("TEXT") str main) throws CompilationException, SnuggleException {
    public void test(BuiltinTypes types, String main) throws CompilationException, SnuggleException {
        test(types, Map.of("main", main), null);
    }

    public void test(Map<String, String> files) throws CompilationException, SnuggleException {
        test(new BuiltinTypes(), files, null);
    }

    public void test(BuiltinTypes types, Map<String, String> files, File export) throws CompilationException, SnuggleException {
        types.addType(TestBindings.class);
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
