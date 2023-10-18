import builtin_types.BuiltinTypes;
import util.CompileAll;

import java.util.Map;

public class Main {

    public static void main(String[] args) {
        testExtension();
    }

    private static void testExtension() {
        test("""
                class A {
                    fn new() super()
                    fn get(): i32 5
                }
                class B: A {
                    fn new() super()
                    fn get(): i32 10
                }
                
                System.print(new A().get())
                System.print(new B().get())
            """);
    }

    private static void testFib() {
        test("""
                class Fib {
                    fn new();
                    fn fib(n: u32): u32 {
                        if n < 2 {
                            n
                        } else {
                            this.fib(n-1) + this.fib(n-2)
                        }
                    }
                }
                var x: u32 = 0
                while 20 > x = x + 1
                    System.print(new Fib().fib(x-1))
                """);
    }

    private static void testOverloads() {
        BuiltinTypes types = new BuiltinTypes()
                .reflect(TestReflectedClass.class);
        test(types, """
                class Silly {
                    fn new();
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


    private static void testMultipleCallable() {
        test("""
                class Test {
                    fn new();
                    fn a(): i32 1
                    fn a(): i64 2
                    //try commenting out one of the b()
                    fn b(x: i32) System.print(x)
                    fn b(x: i64) System.print(x)
                }
                var x = new Test()
                x.b(x.a()) //doesn't know which b() to call
                """);
    }

    private static void testMultipleReturn() {
        test("""
                class Test {
                    fn new();
                    fn a(): i32 1
                    fn a(): i64 2
                }
                var x: i32 = new Test().a() //1
                var y: i64 = new Test().a() //2
                var z: u32 = new Test().a() //error, no method a() works
                
                System.print(x)
                System.print(y)
                System.print(z)
                """);
    }

    private static void testReflectionExtend() {
        BuiltinTypes types = new BuiltinTypes()
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

    private static void testReflection() {
        BuiltinTypes types = new BuiltinTypes()
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

    private static void testMoreIntMath() {
        test("""
                var y: i32 = 5
                System.print(y % 3)
                System.print(~y)
                System.print(!y)
                System.print(!!y)
                System.print(-y)
                System.print(y / y)
                System.print(y & y)
                System.print(y ^ y)
                
                var x: i32 = ~10
                System.print(x)
                """);
    }

    private static void testIntPrints() {
        test("""
                var x: i32 = 2147483647;
                x = x + 2;
                var y: u32 = 2147483647;
                y = y + 2;
                
                var z: i64 = 9223372036854775807;
                z = z + 2;
                var w: u64 = 9223372036854775807;
                w = w + 2;
                System.print(x)
                System.print(y)
                System.print(z)
                System.print(w)
                
                var a: u32 = 4294967295
                a = a + 1
                System.print(a)
                a = a - 1
                System.print(a)
                
                var b: i32 = -1
                var c: u32 = 0
                c = c - 1;
                System.print(b * b)
                System.print(c * c)
                System.print(b / 10)
                System.print(c / 10)
                """);
    }

    private static void testWhile() {
        test("""
                var x: i32 = 10
                while x > 0 {
                    System.print(x = x - 1);
                }
                """);
    }

    private static void testIf() {
        test("""
                var x: i32 = if 2 > 1
                    0
                else
                    5
                System.print(x)
                
                var y: i32 = if 5 < 2 {
                    true //Condition is constant false, so it doesn't bother type-checking this branch
                } else {
                    10
                }
                System.print(y)
                
                """);
    }

    private static void testBlocks() {
        test("""
                class Plural {
                    pub fn new();
                    pub fn doMultipleThings() {
                        var x: i32 = 1;
                        System.print(x);
                        System.print(x + 1);
                        //var y: i16 = 2 //errors, since the last expression in a block cannot be a declaration
                    }
                }
                new Plural().doMultipleThings()
                """);
    }

    private static void testAssignment() {
        test("""
                var x: i8 = 1;
                var y: i8 = 2;
                var z: i8 = 3;
                System.print(x)
                System.print(y)
                System.print(z = 4)
                System.print(z)
                x = y = z
                System.print(x + y + z)
                """);
    }

    private static void testGeneric() {
        test("""
                class Thing<T> {
                    pub fn new();
                    pub fn add1(x: T): T
                        x + 1
                }
                var x = new Thing<u8>().add1(255)
                var y = new Thing<i8>().add1(127)
                System.print(x)
                System.print(y)
                """);
    }

    private static void testIntArithmetic() {
        test("""
                var x: u8 = 128
                System.print(x)
                System.print(x + 1)
                System.print(128 + x)
                """);
    }

    private static void testI84() {
        test("""
                class i84 {
                    pub fn new();
                    pub fn get(): i8 4
                }
                
                class Other {
                    pub fn new();
                    pub fn meow(): i8 4
                        new i84()
                }
                
                System.print(new Other().meow().get())
                
                """);
    }

    private static void testArgs() {
        test("""
                class Behold {
                    fn new();
                    fn arguments(a: i32, b: i16, c: i8)
                        System.print(c)
                }
                new Behold().arguments(1, 2, 3)
                """);
    }

    private static void test2Classes() {
        test("""
                class Thing {
                    pub fn new();
                    pub fn get5(): i32
                        1 + 1 + 1 + 1 + 1
                }
                class Thang {
                    pub fn new();
                    pub fn get5Again(thing: Thing): i32
                        thing.get5()
                }
                System.print(new Thang().get5Again(new Thing()))
                """);
    }

    private static void testThis() {
        test("""
                class Thing {
                    pub fn new();
                    pub fn doTheFirstThing(): i32
                        1 + 2 * 3
                    pub fn doTheFirstThing(): i16
                        1 + 2 * 4
                    pub fn doTheSecondThing(): i64
                        1 - 2 + 3 - 4
                    pub fn doYetAnotherThing() System.print(this.doTheSecondThing())
                }
                var a: i8 = 256 / 2 - 1
                var b: i32 = new Thing().doTheFirstThing()
                var c: i16 = new Thing().doTheFirstThing()
                var thing = new Thing()
                System.print(a)
                System.print(b)
                System.print(c)
                System.print(thing.doTheSecondThing())
                thing.doYetAnotherThing()
                """);
    }

    private static void test1() {
        test("""
                class Cutie {
                    pub fn new();
                    pub fn meow(): i64 42
                    pub fn pat(): i32
                        1 + 2 * 3
                    pub fn pat(): i16
                        1 + 2 * 4
                    pub fn snug(): i64
                        1 - 2 + 3 - 4
                    pub fn doSilly() System.print(this.snug())
                }
                var a: i8 = 256 / 2 - 1
                var b: i32 = new Cutie().pat()
                var c: i16 = new Cutie().pat()
                var aple = new Cutie()
                System.print(a)
                System.print(b)
                System.print(c)
                System.print(aple.snug())
                aple.doSilly()
                """);
    }

    private static void test(String main) {
        try {
            CompileAll.compileAll(new BuiltinTypes(), Map.of("main", main)).run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void test(BuiltinTypes types, String main) {
        try {
            CompileAll.compileAll(types, Map.of("main", main)).run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
