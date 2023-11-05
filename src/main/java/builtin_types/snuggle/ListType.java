package builtin_types.snuggle;

public class ListType extends SnuggleDefinedType {

    public static ListType INSTANCE = new ListType();

    private ListType() {
        super("List", """
                
                pub class List<T> {
                
                    var backing: Array<MaybeUninit<T>>
                    var backingSize: u32
                    var size: u32 = 0
                    var empty: MaybeUninit<T>
                    
                    //Create a new List<T> with backing size 5
                    pub fn new() {
                        super()
                        backing = new Array<MaybeUninit<T>>(backingSize = 5)
                        empty = backing[0];
                    }
                    
                    //Create a new List<T> with the passed backing size
                    pub fn new(startingBackingSize: u32) {
                        if startingBackingSize == 0 {
                            //error :P
                            var x = 0i32
                            1i32 / x;
                        }
                        super()
                        backing = new Array<MaybeUninit<T>>(backingSize = startingBackingSize)
                        empty = backing[0];
                    }
                    
                    //Get from index
                    pub fn get(i: u32): T {
                        if i >= size {
                            //error :P
                            var x = 0i32
                            1i32 / x
                            empty.get()
                        } else {
                            backing[i].get()
                        }
                    }
                    
                    //Set from index
                    pub fn set(i: u32, elem: T): T {
                        if i >= size {
                            //error :P
                            var x = 0i32
                            1i32 / x;
                        } else {
                            backing[i] = new MaybeUninit<T>(elem);
                        }
                        elem
                    }
                    
                    //Size
                    pub fn size(): u32 size
                    
                    //Push an element into the list, double the size if it fills up
                    pub fn push(elem: T): List<T> {
                        if size == #backing {
                            var newBacking = new Array<MaybeUninit<T>>(size * 2)
                            var i: u32 = 0
                            while i < #backing {
                                newBacking[i] = new MaybeUninit<T>(backing[i].get())
                                i += 1
                            }
                            backing = newBacking
                        }
                        backing[size] = new MaybeUninit<T>(elem)
                        size += 1
                        this
                    }
                    //Operator overload for push, +=
                    pub fn plusAssign(elem: T): List<T>
                        this.push(elem)
                    
                    //Remove the last element of the list
                    pub fn pop(): T {
                        size -= 1
                        var elem = backing[size].get()
                        backing[size] = empty
                        elem
                    }
                    
                }
                
                """);
    }
}
