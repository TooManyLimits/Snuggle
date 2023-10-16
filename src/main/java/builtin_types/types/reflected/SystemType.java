package builtin_types.types.reflected;

import builtin_types.BuiltinType;
import builtin_types.reflect.ReflectedBuiltin;
import builtin_types.reflect.annotations.SnuggleType;
import builtin_types.reflect.annotations.SnuggleWhitelist;
import builtin_types.reflect.annotations.Unsigned;

@SnuggleType(name = "System")
public class SystemType {

    public static final BuiltinType INSTANCE = new ReflectedBuiltin(SystemType.class);

    @SnuggleWhitelist
    public static void print(byte b) { System.out.println(b); }
    @SnuggleWhitelist(rename = "print")
    public static void print_2(@Unsigned byte b) { System.out.println(b & 0xFF); }
    @SnuggleWhitelist
    public static void print(short s) { System.out.println(s); }
    @SnuggleWhitelist(rename = "print")
    public static void print_2(@Unsigned short s) { System.out.println(s & 0xFFFF); }
    @SnuggleWhitelist
    public static void print(int i) { System.out.println(i); }
    @SnuggleWhitelist(rename = "print")
    public static void print_2(@Unsigned int i) { System.out.println(i & 0xFFFFFFFFL); }
    @SnuggleWhitelist
    public static void print(long l) { System.out.println(l); }
    @SnuggleWhitelist(rename = "print")
    public static void print_2(@Unsigned long l) { System.out.println(Long.toUnsignedString(l)); }

//    @SnuggleWhitelist
//    public static void print(String s) { System.out.println(s); }

}
