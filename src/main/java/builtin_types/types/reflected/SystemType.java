package builtin_types.types.reflected;

import builtin_types.BuiltinType;
import builtin_types.reflect.ReflectedBuiltin;
import builtin_types.reflect.annotations.*;

@SnuggleType(name = "System")
@SnuggleWhitelist
public class SystemType {

    @SnuggleBlacklist
    public static final BuiltinType INSTANCE = new ReflectedBuiltin(SystemType.class);

    public static void print(byte b) { System.out.println(b); }
    @Rename("print")
    public static void print_2(@Unsigned byte b) { System.out.println(b & 0xFF); }
    public static void print(short s) { System.out.println(s); }
    @Rename("print")
    public static void print_2(@Unsigned short s) { System.out.println(s & 0xFFFF); }
    public static void print(int i) { System.out.println(i); }
    @Rename("print")
    public static void print_2(@Unsigned int i) { System.out.println(i & 0xFFFFFFFFL); }
    public static void print(long l) { System.out.println(l); }
    @Rename("print")
    public static void print_2(@Unsigned long l) { System.out.println(Long.toUnsignedString(l)); }

    public static void print(boolean b) { System.out.println(b); }

//    @SnuggleWhitelist
//    public static void print(String s) { System.out.println(s); }

}
