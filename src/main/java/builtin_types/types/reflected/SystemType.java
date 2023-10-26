package builtin_types.types.reflected;

import builtin_types.BuiltinType;
import builtin_types.reflect.ReflectedBuiltin;
import builtin_types.reflect.annotations.*;

@SnuggleType(name = "System")
@SnuggleWhitelist
public class SystemType {

    @SnuggleBlacklist
    public static final BuiltinType INSTANCE = new ReflectedBuiltin(SystemType.class);

    public static long i2l(int i) { return i; }
    public static int l2i(long i) { return (int) i; }
    public static byte i2b(int i) { return (byte) i; }
    public static byte l2b(long i) { return (byte) i; }
    public static int b2i(byte i) { return i; }

    public static int shl(int x, int bits) { return x << bits; }
    @Rename("shl")
    public static int shl_u2(int x, @Unsigned int bits) { return x << bits; }
    public static int shr(int x, int bits) { return x >> bits; }
    @Rename("shr")
    public static int shr_u2(int x, @Unsigned int bits) { return x >> bits; }
    public static long shl(long x, int bits) { return x << bits; }
    public static long shr(long x, int bits) { return x >> bits; }
    public static int ushr(int x, int bits) { return x >>> bits; }
    @Rename("ushr")
    public static int ushr_u1(@Unsigned int x, int bits) { return x >>> bits; }
    public static long ushr(long x, int bits) { return x >>> bits; }


    public static void print(float f) { System.out.println(f); }
    public static void print(double d) { System.out.println(d); }
    public static void print(byte b) { System.out.println(b); }
    @Rename("print")
    public static void printU8(@Unsigned(8) int b) { System.out.println(b & 0xFF); }
    public static void print(short s) { System.out.println(s); }
    @Rename("print")
    public static void printU16(@Unsigned(16) int s) { System.out.println(s & 0xFFFF); }
    public static void print(int i) { System.out.println(i); }
    @Rename("print")
    public static void printU32(@Unsigned int i) { System.out.println(i & 0xFFFFFFFFL); }
    public static void print(long l) { System.out.println(l); }
    @Rename("print")
    public static void printU64(@Unsigned long l) { System.out.println(Long.toUnsignedString(l)); }

    public static void print(boolean b) { System.out.println(b); }

    public static void print(String s) { System.out.println(s); }

    public static void print(Object o) { System.out.println(o); }

//    @SnuggleWhitelist
//    public static void print(String s) { System.out.println(s); }

}
