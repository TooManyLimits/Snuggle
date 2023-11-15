package builtin_types.types.reflected;

import builtin_types.BuiltinType;
import builtin_types.reflect.Reflector;
import builtin_types.reflect.annotations.*;

@Rename("System")
@SnuggleType
@SnuggleWhitelist
public class SystemType {

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

    //C++ time
    public static void shl(String s) { System.out.println(s); }

    public static void print(Object o) { System.out.println(o); }

//    @SnuggleWhitelist
//    public static void print(String s) { System.out.println(s); }

}
