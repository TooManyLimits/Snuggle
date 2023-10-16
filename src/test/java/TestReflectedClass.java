import builtin_types.reflect.annotations.SnuggleType;
import builtin_types.reflect.annotations.SnuggleWhitelist;
import builtin_types.reflect.annotations.Unsigned;

@SnuggleType(name = "TestClass")
public class TestReflectedClass {

    @SnuggleWhitelist
    public static void printSilly() {
        System.out.println("Silly!");
    }

    @SnuggleWhitelist
    public static TestReflectedClass get() {
        return new TestReflectedClass();
    }

    @SnuggleWhitelist
    public int beeg() {
        return 100;
    }

    @SnuggleWhitelist(rename = "beeg")
    public @Unsigned int beeg_2() {
        return 200;
    }

    @SnuggleWhitelist
    public static int add(int x) {
        return x + 1;
    }

}
