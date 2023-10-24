import builtin_types.BuiltinTypes;
import runtime.SnuggleInstance;
import util.CompileAll;

import java.io.File;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws Exception {
        testImports();
    }

    private static void testImports() {
        test(Map.of(
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
                            fn new() super()
                            fn bad() System.print(new Obj() as Cutie)
                            fn good() System.print("good cutie :D")
                        }
                        new Cutie().bad() //error!
                        System.print("Cutie!")
                        """
        ));
    }

    private static void testStackOverflow() {
        test("""
                class death {
                    fn new() super()
                    fn get() this.get()
                }
                new death().get()
                """);
    }

    private static void testObjectCast() {
        test("""
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
                """);
    }

    private static void testNumberCast() {
        test("""
                var x = 150i64
                var y = x as u8
                var z = x as f64 + 0.56
                System.print(y)
                System.print(z)
                """);
    }

    private static void testFloats() {
        test("""
                var x: f64 = 0.1
                var y: f64 = 0.2
                System.print(x + y == 0.3) //false
                System.print(0.1 + 0.2 == 0.3) //true
                System.print(0.1f64 + 0.2 == 0.3) //false
                """);
    }

    private static void testI2L() {
        test("""
                var x: i32 = 10
                var y: i64 = System.i2l(x)
                System.print(y)
                """);
    }

    private static void testList() {
        test("""
                class List<T> {
                    var backing: Array<T> = new Array<T>(5)
                    var size: u32 = 0
                    fn new() super()
                    fn push(elem: T) {
                        this.backing.set(this.size, elem)
                        this.size = this.size + 1;
                        if this.size == this.backing.len() {
                            var newBacking = new Array<T>(this.size * 2);
                            var i: u32 = 0
                            while i < this.backing.len() {
                                newBacking.set(i, this.backing.get(i))
                                i = i + 1
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
                    i = i + 1
                }
                
                """);
    }

    private static void testFields() {
        test("""
                class A {
                    var x: i32 = 11
                    fn new() {
                        super()
                        //this.x = 11;
                    }
                    pub fn get(): i32 this.x = this.x - 1
                }
                System.print(new A().get())
                System.print(new A().get())
                var a = new A()
                System.print(a.get())
                System.print(a.get())
                System.print(a.get())
                System.print(a.get())
                System.print(a.get())
                """);
    }

    private static void testChaCha20() {
        test("""
                class Thing {
                    fn new() super()
                    fn addOneToAll(x: Array<i32>) {
                        var i: u32 = 0;
                        while i < x.len() {
                            x.set(i, x.get(i) + 1)
                            i = i + 1
                        };
                    }
                    fn reduceQuick(x: Array<i32>) {
                        var temp = new Array<i32>(10)
                        var carry: i32 = 19;
                                    
                        var index: u32 = 0;
                        while index < 10 {
                            carry = carry + x.get(index);
                            temp.set(index, carry & 67108863);
                            carry = carry / 67108864;
                            index = index + 1;
                        }
                                    
                        var mask: i32 = -{temp.get(9) / 2097152 & 1};
                        temp.set(9, temp.get(9) & 2097151);
                        index = 0;
                        while index < 10 {
                            x.set(index, x.get(index) & ~mask | temp.get(index) & mask);
                            index = index + 1;
                        };
                    }
                }
                var arr = new Array<i32>(10)
                arr.set(0, 980736265)
                arr.set(1, 385596859)
                arr.set(2, 559296360)
                arr.set(3, 1073258648)
                arr.set(4, 1624787745)
                arr.set(5, 990138740)
                arr.set(6, 1176178535)
                arr.set(7, 1745964209)
                arr.set(8, 1591302209)
                arr.set(9, 1675809972)
                new Thing().reduceQuick(arr)
                var i: u32 = 0
                while i < 10 {
                    System.print(arr.get(i))
                    i = i + 1
                }
                """);
    }



    private static void testArrays() {
        test("""
                var x = new Array<i32>(5)
                x.set(3, 10)
                x.set(1, 4)
                System.print(x.get(3))
                """);
    }

    private static void testExtension() {
        test("""
                class A {
                    fn new() super()
                    fn get(): i32 5
                }
                class B: A {
                    fn new() super()
                    fn get10(): i32 10
                    fn get(): i32 8
                    fn get5(): i32 super.get()
                    fn get8(): i32 this.get()
                }
                
                System.print(new A().get())
                System.print(new B().get())
                System.print(new B().get8())
                System.print(new B().get10())
                System.print(new B().get5())
            """);
    }

    private static void testFib() {
        test("""
                class Fib {
                    fn new() super()
                    fn fib(n: u32): u32 {
                        if n < 2 {
                            n
                        } else {
                            this.fib(n-1) + this.fib(n-2)
                        }
                    }
                }
                System.print(new Fib().fib(45))
                
//                var x: u32 = 0
//                while 45 >= x = x + 1
//                    System.print(new Fib().fib(x-1))
                """);
    }

    private static void testOverloads() {
        BuiltinTypes types = new BuiltinTypes()
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


    private static void testMultipleCallable() {
        test("""
                class Test {
                    fn new() super()
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
                    fn new() super()
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
                    pub fn new() super()
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
                    pub fn new() super()
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
                
                var y: u32 = 0;
                y = y - 10; //-10 as a u32
                System.print(y / 5)
                var z: i32 = -10;
                System.print(z / -5)
                """);
    }

    private static void testI84() {
        test("""
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
                
                """);
    }

    private static void testArgs() {
        test("""
                class Behold {
                    fn new() super()
                    fn arguments(a: i32, b: i16, c: i8)
                        System.print(c)
                }
                new Behold().arguments(1, 2, 3)
                """);
    }

    private static void test2Classes() {
        test("""
                class Thing {
                    pub fn new() super()
                    pub fn get5(): i32
                        1 + 1 + 1 + 1 + 1
                }
                class Thang {
                    pub fn new() super()
                    pub fn get5Again(thing: Thing): i32
                        thing.get5()
                }
                System.print(new Thang().get5Again(new Thing()))
                """);
    }

    private static void testThis() {
        test("""
                class Thing {
                    pub fn new() super()
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
                    pub fn new() super()
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

    private static void testChaCha21() throws Exception {
//        CompileAll.compileAllToJar(new File("curve25519.jar"), new BuiltinTypes(), Map.of("main",
        test(
                """
                            class Curve25519 {
                                                
                                fn reduceQuick(x: Array<i32>): unit {
                                    var temp: Array<i32> = new Array<i32>(10);
                                    var index: i32 = 0;
                                    var carry: i32 = 19;
                                                
                                    while index < 10 {
                                        carry = carry + x.get(index);
                                        temp.set(index, carry & 67108863);
                                        carry = System.shr(carry, 26);
                                        index = index + 1;
                                    };
                                                
                                    var mask: i32 = -{ System.shr(temp.get(9), 21) & 1 };
                                    temp.set(9, temp.get(9) & 2097151);
                                    index = 0;
                                                
                                    while index < 10 {
                                        x.set(index, x.get(index) & ~mask | temp.get(index) & mask);
                                        index = index + 1;
                                    };
                                                
                                }
                                                
                                fn reduce(result: Array<i32>, x: Array<i32>, size: i32): unit {
                                    var index: i32 = 0;
                                    var carry: i32 = 0;
                                    var limb: i32 = System.shr(x.get(9), 21);
                                    x.set(9, x.get(9) & 2097151);
                                                
                                    while index < size {
                                        limb = limb + { System.shl(x.get(10 + index), 5) };
                                        carry = carry + { { limb & 67108863 } * 19 + x.get(index) };
                                        x.set(index, carry & 67108863);
                                        limb = System.shr(limb, 26);
                                        carry = System.shr(carry, 26);
                                        index = index + 1;
                                    };
                                                
                                    if size < 10 {
                                        index = size;
                                                
                                        while index < 10 {
                                            carry = carry + x.get(index);
                                            x.set(index, carry & 67108863);
                                            carry = System.shr(carry, 26);
                                            index = index + 1;
                                        };
                                                
                                    } else {};
                                    carry = { System.shr(x.get(9), 21) } * 19;
                                    x.set(9, x.get(9) & 2097151);
                                    index = 0;
                                                
                                    while index < 10 {
                                        carry = carry + x.get(index);
                                        result.set(index, carry & 67108863);
                                        carry = System.shr(carry, 26);
                                        index = index + 1;
                                    };
                                                
                                    this.reduceQuick(result);
                                }
                                                
                                fn multiply(result: Array<i32>, x: Array<i32>, y: Array<i32>): unit {
                                    var temp: Array<i64> = new Array<i64>(20);
                                    var i: i32 = 0;
                                    var j: i32 = 0;
                                    var v: i64 = System.i2l(x.get(0));
                                    i = 0;
                                                
                                    while i < 10 {
                                        temp.set(i, v * System.i2l(y.get(i)));
                                        i = i + 1;
                                    };
                                                
                                    i = 1;
                                                
                                    while i < 10 {
                                        v = System.i2l(x.get(i));
                                        j = 0;
                                                
                                        while j < 9 {
                                            temp.set(i + j, temp.get(i + j) + { v * System.i2l(y.get(j)) });
                                            j = j + 1;
                                        };
                                                
                                        temp.set(i + 9, v * System.i2l(y.get(9)));
                                        i = i + 1;
                                    };
                                                
                                    v = temp.get(0);
                                    var t2: Array<i32> = new Array<i32>(20);
                                    t2.set(0, System.l2i(v) & 67108863);
                                    i = 1;
                                                
                                    while i < 20 {
                                        v = { System.shr(v, 26) } + temp.get(i);
                                        t2.set(i, System.l2i(v) & 67108863);
                                        i = i + 1;
                                    };
                                                
                                    this.reduce(result, t2, 10);
                                }
                                                
                                fn square(result: Array<i32>, x: Array<i32>): unit {
                                    this.multiply(result, x, x);
                                }
                                                
                                fn mulA24(result: Array<i32>, x: Array<i32>): unit {
                                    var a24: i64 = 121665;
                                    var carry: i64 = 0;
                                    var index: i32 = 0;
                                    var t2: Array<i32> = new Array<i32>(20);
                                                
                                    while index < 10 {
                                        carry = carry + { a24 * System.i2l(x.get(index)) };
                                        t2.set(index, System.l2i(carry) & 67108863);
                                        carry = System.shr(carry, 26);
                                        index = index + 1;
                                    };
                                                
                                    t2.set(10, System.l2i(carry & 67108863));
                                    this.reduce(result, t2, 1);
                                }
                                                
                                fn add(result: Array<i32>, x: Array<i32>, y: Array<i32>): unit {
                                    var index: i32 = 1;
                                    var carry: i32 = x.get(0) + y.get(0);
                                    result.set(0, carry & 67108863);
                                                
                                    while index < 10 {
                                        carry = { System.shr(carry, 26) } + x.get(index) + y.get(index);
                                        result.set(index, carry & 67108863);
                                        index = index + 1;
                                    };
                                                
                                    this.reduceQuick(result);
                                }
                                                
                                fn sub(result: Array<i32>, x: Array<i32>, y: Array<i32>): unit {
                                    var index: i32 = 0;
                                    var borrow: i32 = 0;
                                                
                                    while index < 10 {
                                        borrow = x.get(index) - y.get(index) - { System.shr(borrow, 26) & 1 };
                                        result.set(index, borrow & 67108863);
                                        index = index + 1;
                                    };
                                                
                                    borrow = result.get(0) - { -{ System.shr(borrow, 26) & 1 } & 19 };
                                    result.set(0, borrow & 67108863);
                                    index = 1;
                                                
                                    while index < 10 {
                                        borrow = result.get(index) - { System.shr(borrow, 26) & 1 };
                                        result.set(index, borrow & 67108863);
                                        index = index + 1;
                                    };
                                                
                                    result.set(9, result.get(9) & 2097151);
                                }
                                                
                                fn swap(select: i32, x: Array<i32>, y: Array<i32>): unit {
                                    select = -select;
                                    {
                                        var index: i32 = 0;
                                                
                                        while index < 10 {
                                            var dummy: i32 = select & { x.get(index) ^ y.get(index) };
                                            x.set(index, x.get(index) ^ dummy);
                                            y.set(index, y.get(index) ^ dummy);
                                            index = index + 1;
                                        };
                                    }
                                }
                                                
                                fn pow250(result: Array<i32>, x: Array<i32>): unit {
                                    var A: Array<i32> = new Array<i32>(10);
                                    var i: i32 = 0;
                                    var j: i32 = 0;
                                    this.square(A, x);
                                                
                                    while j < 9 {
                                        this.square(A, A);
                                        j = j + 1;
                                    };
                                                
                                    this.multiply(result, A, x);
                                                
                                    while i < 23 {
                                        j = 0;
                                                
                                        while j < 10 {
                                            this.square(A, A);
                                            j = j + 1;
                                        };
                                                
                                        this.multiply(result, result, A);
                                        i = i + 1;
                                    };
                                                
                                    this.square(A, result);
                                    this.multiply(result, result, A);
                                    j = 0;
                                                
                                    while j < 8 {
                                        this.square(A, A);
                                        this.multiply(result, result, A);
                                        j = j + 1;
                                    };
                                                
                                }
                                                
                                fn modInv(result: Array<i32>, x: Array<i32>): unit {
                                    this.pow250(result, x);
                                    this.square(result, result);
                                    this.square(result, result);
                                    this.multiply(result, result, x);
                                    this.square(result, result);
                                    this.square(result, result);
                                    this.multiply(result, result, x);
                                    this.square(result, result);
                                    this.multiply(result, result, x);
                                }
                                                
                                fn curve25519(result: Array<i8>, privateKey: Array<i8>, hasPublicKey: bool, publicKey: Array<i8>): unit {
                                    var i: i32 = 0;
                                    var x_1: Array<i32> = new Array<i32>(10);
                                    var x_2: Array<i32> = new Array<i32>(10);
                                    var x_3: Array<i32> = new Array<i32>(10);
                                    var z_2: Array<i32> = new Array<i32>(10);
                                    var z_3: Array<i32> = new Array<i32>(10);
                                    var A: Array<i32> = new Array<i32>(10);
                                    var B: Array<i32> = new Array<i32>(10);
                                    var C: Array<i32> = new Array<i32>(10);
                                    var D: Array<i32> = new Array<i32>(10);
                                    var E: Array<i32> = new Array<i32>(10);
                                    var AA: Array<i32> = new Array<i32>(10);
                                    var BB: Array<i32> = new Array<i32>(10);
                                    var DA: Array<i32> = new Array<i32>(10);
                                    var CB: Array<i32> = new Array<i32>(10);
                                    if hasPublicKey {
                                        i = 0;
                                                
                                        while i < 32 {
                                            var bit: i32 = i * 8 % 26;
                                            var word: i32 = i * 8 / 26;
                                            var value: i32 = System.b2i(publicKey.get(i)) & 255;
                                            if bit <= 18 {
                                                x_1.set(word, x_1.get(word) | { System.shl(value, bit) });
                                            } else {
                                                x_1.set(word, x_1.get(word) | { System.shl(value, bit) });
                                                x_1.set(word, x_1.get(word) & 67108863);
                                                x_1.set(word + 1, x_1.get(word + 1) | { System.shr(value, 26 - bit) });
                                            };
                                            i = i + 1;
                                        };
                                                
                                        this.reduceQuick(x_1);
                                        this.reduceQuick(x_1);
                                    } else {
                                        x_1.set(0, 9);
                                    };
                                    x_2.set(0, 1);
                                    i = 0;
                                                
                                    while i < 10 {
                                        x_3.set(i, x_1.get(i));
                                        i = i + 1;
                                    };
                                                
                                    z_3.set(0, 1);
                                    var sposn: i32 = 31;
                                    var sbit: i32 = 6;
                                    var svalue: i32 = System.b2i(privateKey.get(sposn)) | 64;
                                    var swap: i32 = 0;
                                    var goOn: bool = true;
                                    while goOn {
                                        var select: i32 = System.shr(svalue, sbit) & 1;
                                        swap = swap ^ select;
                                        this.swap(swap, x_2, x_3);
                                        this.swap(swap, z_2, z_3);
                                        swap = select;
                                        this.add(A, x_2, z_2);
                                        this.square(AA, A);
                                        this.sub(B, x_2, z_2);
                                        this.square(BB, B);
                                        this.sub(E, AA, BB);
                                        this.add(C, x_3, z_3);
                                        this.sub(D, x_3, z_3);
                                        this.multiply(DA, D, A);
                                        this.multiply(CB, C, B);
                                        this.add(x_3, DA, CB);
                                        this.square(x_3, x_3);
                                        this.sub(z_3, DA, CB);
                                        this.square(z_3, z_3);
                                        this.multiply(z_3, z_3, x_1);
                                        this.multiply(x_2, AA, BB);
                                        this.mulA24(z_2, E);
                                        this.add(z_2, z_2, AA);
                                        this.multiply(z_2, z_2, E);
                                        if sbit > 0 {
                                            sbit = sbit - 1;
                                        } else if sposn == 0 {
                                            goOn = false;
                                        } else if sposn == 1 {
                                            sposn = 0;
                                            svalue = System.b2i(privateKey.get(0)) & 248;
                                            sbit = 7;
                                        } else {
                                            sposn = sposn - 1;
                                            svalue = System.b2i(privateKey.get(sposn));
                                            sbit = 7;
                                        };;;
                                    };
                                    this.swap(swap, x_2, x_3);
                                    this.swap(swap, z_2, z_3);
                                    this.modInv(z_3, z_2);
                                    this.multiply(x_2, x_2, z_3);
                                    {
                                        var index: i32 = 0;
                                                
                                        while index < 32 {
                                            var bit: i32 = index * 8 % 26;
                                            var word: i32 = index * 8 / 26;
                                            if bit <= 18 {
                                                result.set(index, System.i2b(System.shr(x_2.get(word), bit)));
                                            } else {
                                                result.set(index, System.i2b(System.shr(x_2.get(word), bit) | System.shl(x_2.get(word + 1), 26 - bit)));
                                            };
                                            index = index + 1;
                                        };
                                    }
                                }
                                fn new() super()
                            }
                            new Curve25519()
                        """);//);
    }

    private static void testMd5() throws Exception {
        CompileAll.compileAllToJar(new File("md5.jar"), new BuiltinTypes(), Map.of("main", """
                class MD5 {
                                        
                    fn rotateLeft(i: i32, distance: i32): i32 {
                        { System.shl(i, distance) } | { System.ushr(i, -distance) }
                    }
                                        
                    fn computeMD5(message: Array<i8>): Array<i8> {
                        var SHIFT_AMTS: Array<i32> = new Array<i32>(16);
                        {
                            SHIFT_AMTS.set(0, 7);
                            SHIFT_AMTS.set(1, 12);
                            SHIFT_AMTS.set(2, 17);
                            SHIFT_AMTS.set(3, 22);
                            SHIFT_AMTS.set(4, 5);
                            SHIFT_AMTS.set(5, 9);
                            SHIFT_AMTS.set(6, 14);
                            SHIFT_AMTS.set(7, 20);
                            SHIFT_AMTS.set(8, 4);
                            SHIFT_AMTS.set(9, 11);
                            SHIFT_AMTS.set(10, 16);
                            SHIFT_AMTS.set(11, 23);
                            SHIFT_AMTS.set(12, 6);
                            SHIFT_AMTS.set(13, 10);
                            SHIFT_AMTS.set(14, 15);
                            SHIFT_AMTS.set(15, 21);
                        }
                        var TABLE_T: Array<i32> = new Array<i32>(64);
                        {
                            TABLE_T.set(0, -680876936);
                            TABLE_T.set(1, -389564586);
                            TABLE_T.set(2, 606105819);
                            TABLE_T.set(3, -1044525330);
                            TABLE_T.set(4, -176418897);
                            TABLE_T.set(5, 1200080426);
                            TABLE_T.set(6, -1473231341);
                            TABLE_T.set(7, -45705983);
                            TABLE_T.set(8, 1770035416);
                            TABLE_T.set(9, -1958414417);
                            TABLE_T.set(10, -42063);
                            TABLE_T.set(11, -1990404162);
                            TABLE_T.set(12, 1804603682);
                            TABLE_T.set(13, -40341101);
                            TABLE_T.set(14, -1502002290);
                            TABLE_T.set(15, 1236535329);
                            TABLE_T.set(16, -165796510);
                            TABLE_T.set(17, -1069501632);
                            TABLE_T.set(18, 643717713);
                            TABLE_T.set(19, -373897302);
                            TABLE_T.set(20, -701558691);
                            TABLE_T.set(21, 38016083);
                            TABLE_T.set(22, -660478335);
                            TABLE_T.set(23, -405537848);
                            TABLE_T.set(24, 568446438);
                            TABLE_T.set(25, -1019803690);
                            TABLE_T.set(26, -187363961);
                            TABLE_T.set(27, 1163531501);
                            TABLE_T.set(28, -1444681467);
                            TABLE_T.set(29, -51403784);
                            TABLE_T.set(30, 1735328473);
                            TABLE_T.set(31, -1926607734);
                            TABLE_T.set(32, -378558);
                            TABLE_T.set(33, -2022574463);
                            TABLE_T.set(34, 1839030562);
                            TABLE_T.set(35, -35309556);
                            TABLE_T.set(36, -1530992060);
                            TABLE_T.set(37, 1272893353);
                            TABLE_T.set(38, -155497632);
                            TABLE_T.set(39, -1094730640);
                            TABLE_T.set(40, 681279174);
                            TABLE_T.set(41, -358537222);
                            TABLE_T.set(42, -722521979);
                            TABLE_T.set(43, 76029189);
                            TABLE_T.set(44, -640364487);
                            TABLE_T.set(45, -421815835);
                            TABLE_T.set(46, 530742520);
                            TABLE_T.set(47, -995338651);
                            TABLE_T.set(48, -198630844);
                            TABLE_T.set(49, 1126891415);
                            TABLE_T.set(50, -1416354905);
                            TABLE_T.set(51, -57434055);
                            TABLE_T.set(52, 1700485571);
                            TABLE_T.set(53, -1894986606);
                            TABLE_T.set(54, -1051523);
                            TABLE_T.set(55, -2054922799);
                            TABLE_T.set(56, 1873313359);
                            TABLE_T.set(57, -30611744);
                            TABLE_T.set(58, -1560198380);
                            TABLE_T.set(59, 1309151649);
                            TABLE_T.set(60, -145523070);
                            TABLE_T.set(61, -1120210379);
                            TABLE_T.set(62, 718787259);
                            TABLE_T.set(63, -343485551);
                        }
                        var messageLenBytes: i32 = message.len();
                        var numBlocks: i32 = { System.ushr({ messageLenBytes + 8 }, 6) } + 1;
                        var totalLen: i32 = System.shl(numBlocks, 6);
                        var paddingBytes: Array<i8> = new Array<i8>(totalLen - messageLenBytes);
                        paddingBytes.set(0, System.i2b(128));
                        var messageLenBits: i64 = System.shl(System.i2l(messageLenBytes), 3);
                        {
                            var i: i32 = 0;
                                        
                            while i < 8 {
                                paddingBytes.set(paddingBytes.len() - 8 + i, System.l2b(messageLenBits));
                                messageLenBits = System.ushr(messageLenBits, 8);
                                i = i + 1;
                            };
                        }
                        var a: i32 = 1732584193;
                        var b: i32 = System.l2i(4023233417);
                        var c: i32 = System.l2i(2562383102);
                        var d: i32 = 271733878;
                        var buffer: Array<i32> = new Array<i32>(16);
                        {
                            var i: i32 = 0;
                                        
                            while i < numBlocks {
                                var index: i32 = System.shl(i, 6);
                                {
                                    var j: i32 = 0;
                                        
                                    while j < 64 {
                                        buffer.set(System.ushr(j, 2), { System.shl(System.b2i(if { index < messageLenBytes } message.get(index) else paddingBytes.get(index - messageLenBytes)), 24) } | { System.ushr(buffer.get(System.ushr(j, 2)), 8) });
                                        j = j + 1;
                                        index = index + 1;
                                    };
                                }
                                var originalA: i32 = a;
                                var originalB: i32 = b;
                                var originalC: i32 = c;
                                var originalD: i32 = d;
                                {
                                    var j: i32 = 0;
                                        
                                    while j < 64 {
                                        var div16: i32 = System.ushr(j, 4);
                                        var f: i32 = 0;
                                        var bufferIndex: i32 = j;
                                        if div16 == 0 {
                                            f = { b & c } | { ~b & d };
                                        } else if div16 == 1 {
                                            f = { b & d } | { c & ~d };
                                            bufferIndex = { bufferIndex * 5 + 1 } & 15;
                                        } else if div16 == 2 {
                                            f = b ^ c ^ d;
                                            bufferIndex = { bufferIndex * 3 + 5 } & 15;
                                        } else {
                                            f = c ^ { b | ~d };
                                            bufferIndex = { bufferIndex * 7 } & 15;
                                        };;;
                                        var temp: i32 = b + this.rotateLeft(a + f + buffer.get(bufferIndex) + TABLE_T.get(j), SHIFT_AMTS.get({ System.shl(div16, 2) } | { j & 3 }));
                                        a = d;
                                        d = c;
                                        c = b;
                                        b = temp;
                                        j = j + 1;
                                    };
                                }
                                a = a + originalA;
                                b = b + originalB;
                                c = c + originalC;
                                d = d + originalD;
                                i = i + 1;
                            };
                        }
                        var md5: Array<i8> = new Array<i8>(16);
                        var count: i32 = 0;
                        {
                            var i: i32 = 0;
                                        
                            while i < 4 {
                                var n: i32 = if { i == 0 } a else { if { i == 1 } b else { if { i == 2 } c else d } };
                                var j: i32 = 0;
                                        
                                while j < 4 {
                                    md5.set(count, System.i2b(n));
                                    count = count + 1;
                                    n = System.ushr(n, 8);
                                    j = j + 1;
                                };
                                i = i + 1;
                            };
                        }
                        md5
                    }
                    fn new() super()
                }
                new MD5()
                        """));
    }

    private static void test(String main) {
        test(new BuiltinTypes(), Map.of("main", main));
    }

    private static void test(BuiltinTypes types, String main) {
        test(types, Map.of("main", main));
    }

    private static void test(Map<String, String> files) {
        test(new BuiltinTypes(), files);
    }

    private static void test(BuiltinTypes types, Map<String, String> files) {
        try {
            long before = System.nanoTime();
            SnuggleInstance instance = CompileAll.compileAllToInstance(types, files);
            long after = System.nanoTime();
            System.out.println("Compilation took " + (after - before) / 1000000 + " ms");
            before = after;
            instance.run();
            after = System.nanoTime();
            System.out.println("Running took " + (after - before) / 1000000 + " ms");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
