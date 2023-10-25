import builtin_types.reflect.annotations.SnuggleType;
import builtin_types.reflect.annotations.SnuggleWhitelist;
import builtin_types.reflect.annotations.Unsigned;
import org.junit.jupiter.api.Assertions;

import java.util.HexFormat;

@SnuggleType(name = "Test")
@SnuggleWhitelist
public class TestHarness {

    public static void assertTrue(boolean condition) {
        Assertions.assertTrue(condition);
    }

    public static void assertFalse(boolean condition) {
        Assertions.assertFalse(condition);
    }

    public static void assertEquals(@Unsigned byte expected, @Unsigned byte actual) {
        Assertions.assertEquals(expected, actual);
    }

    public static void assertEquals(long expected, long actual) {
        Assertions.assertEquals(expected, actual);
    }

    public static void assertEquals(double expected, double actual) {
        Assertions.assertEquals(expected, actual);
    }

    // todo: (curve25519) only for byte[]
    public static void assertArrayEquals(Object a, Object b) {
        Assertions.assertArrayEquals((byte[]) a, (byte[]) b);
    }

    // todo: (curve25519) make this return a byte[]???
    public static Object hex(String s) {
        return HexFormat.of().parseHex(s);
    }
}
