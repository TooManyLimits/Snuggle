import builtin_types.reflect.annotations.Rename;
import builtin_types.reflect.annotations.SnuggleType;
import builtin_types.reflect.annotations.SnuggleWhitelist;
import builtin_types.reflect.annotations.Unsigned;
import org.junit.jupiter.api.Assertions;

import java.util.HexFormat;

@Rename("Test")
@SnuggleType
@SnuggleWhitelist
public class TestBindings {

    public static void assertTrue(boolean condition) {
        Assertions.assertTrue(condition);
    }

    public static void assertFalse(boolean condition) {
        Assertions.assertFalse(condition);
    }

    public static void assertEquals(byte expected, byte actual) {
        Assertions.assertEquals(expected, actual);
    }

    @Rename("assertEquals")
    public static void assertEqualsU8(@Unsigned(8) int expected, @Unsigned(8) int actual) {
        Assertions.assertEquals(expected, actual);
    }

    public static void assertEquals(short expected, short actual) {
        Assertions.assertEquals(expected, actual);
    }

    @Rename("assertEquals")
    public static void assertEqualsU16(@Unsigned(16) int expected, @Unsigned(16) int actual) {
        Assertions.assertEquals(expected, actual);
    }

    public static void assertEquals(int expected, int actual) {
        Assertions.assertEquals(expected, actual);
    }

    @Rename("assertEquals")
    public static void assertEqualsU32(@Unsigned int expected, @Unsigned int actual) {
        Assertions.assertEquals(expected, actual);
    }

    public static void assertEquals(long expected, long actual) {
        Assertions.assertEquals(expected, actual);
    }

    @Rename("assertEquals")
    public static void assertEqualsU64(@Unsigned long expected, @Unsigned long actual) {
        Assertions.assertEquals(expected, actual);
    }

    public static void assertEquals(float expected, float actual) {
        Assertions.assertEquals(expected, actual);
    }

    public static void assertEquals(double expected, double actual) {
        Assertions.assertEquals(expected, actual);
    }

    public static void assertEquals(String expected, String actual) {
        Assertions.assertEquals(expected, actual);
    }

    public static void assertArrayEquals(byte[] a, byte[] b) {
        Assertions.assertArrayEquals(a, b);
    }

    public static byte[] hex(String s) {
        return HexFormat.of().parseHex(s);
    }
}
