import builtin_types.reflect.annotations.Rename;
import builtin_types.reflect.annotations.SnuggleType;
import builtin_types.reflect.annotations.SnuggleWhitelist;
import builtin_types.reflect.annotations.Unsigned;

@Rename("TestClass2")
@SnuggleType
@SnuggleWhitelist
public class TestReflectedClass2 extends TestReflectedClass {

    @Override
    @Rename("beeg")
    public @Unsigned int beeg_2() {
        return 300;
    }

    public static TestReflectedClass2 get() {
        return new TestReflectedClass2();
    }

}
