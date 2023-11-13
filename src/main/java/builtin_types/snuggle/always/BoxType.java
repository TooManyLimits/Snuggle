package builtin_types.snuggle.always;

import builtin_types.snuggle.SnuggleDefinedType;

public class BoxType extends SnuggleDefinedType {

    public static final BoxType INSTANCE = new BoxType();

    private BoxType() {
        super("Box", false, """
                
                pub class Box<T> {
                    pub var v: T
                    pub fn new(e: T) {
                        super()
                        v = e;
                    }
                    pub fn get(): T v
                    pub fn set(e: T): T v = e
                }
                
                """);
    }
}
