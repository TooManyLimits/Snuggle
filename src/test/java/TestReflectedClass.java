import builtin_types.reflect.annotations.Rename;
import builtin_types.reflect.annotations.SnuggleType;
import builtin_types.reflect.annotations.SnuggleWhitelist;
import builtin_types.reflect.annotations.Unsigned;

@SnuggleType(name = "TestClass")
@SnuggleWhitelist
public class TestReflectedClass {

    public static void printSilly() {
        System.out.println("Silly!");
    }

    public static TestReflectedClass get() {
        return new TestReflectedClass();
    }

    public int beeg() {
        return 100;
    }

    @Rename("beeg")
    public @Unsigned int beeg_2() {
        return 200;
    }
    public static int add(int x) {
        return x + 1;
    }

}
