import builtin_types.reflect.annotations.SnuggleType;
import builtin_types.reflect.annotations.SnuggleWhitelist;
import builtin_types.reflect.annotations.Unsigned;

@SnuggleType(name = "TestClass2", supertype = TestReflectedClass.class)
public class TestReflectedClass2 extends TestReflectedClass {

    @Override
    @SnuggleWhitelist(rename = "beeg")
    public @Unsigned int beeg_2() {
        return 300;
    }

    @SnuggleWhitelist
    public static TestReflectedClass2 get() {
        return new TestReflectedClass2();
    }

}
