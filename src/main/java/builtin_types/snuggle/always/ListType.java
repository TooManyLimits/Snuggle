package builtin_types.snuggle.always;

import builtin_types.snuggle.SnuggleDefinedType;

public class ListType extends SnuggleDefinedType {

    public static ListType INSTANCE = new ListType();

    private ListType() {
        super("List", false, """
                
                pub class List<T> {
                
                    var backing: MaybeUninit<T>[]
                    var backingSize: u32
                    var size: u32 = 0
                    
                    //Create a new List<T> with backing size 5
                    pub fn new() {
                        super()
                        backing = new(backingSize = 5);
                    }
                    
                    //Create a new List<T> with the passed backing size
                    pub fn new(startingBackingSize: u32) {
                        super()
                        backing = new(backingSize = startingBackingSize);
                    }
                    
                    //Get from index
                    pub fn get(i: u32): T {
                        if i >= size {
                            //error :P
                            var x = 0i32
                            1i32 / x
                            new MaybeUninit<T>().get()
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
                            backing[i] = new(elem);
                        }
                        elem
                    }
                    
                    //Size
                    pub fn size(): u32 size
                    
                    //Push an element into the list, double the size if it fills up
                    pub fn push(elem: T): List<T> {
                        if size == #backing {
                            var newBacking = new MaybeUninit<T>[](size * 2 + 1)
                            var i: u32 = 0
                            while i < #backing {
                                newBacking[i] = new(backing[i].get())
                                i += 1
                            }
                            backing = newBacking
                        }
                        backing[size] = new(elem)
                        size += 1
                        this
                    }
                    //Operator overload for push, +=
                    pub fn addAssign(elem: T): List<T>
                        this.push(elem)
                    
                    //Remove the last element of the list
                    pub fn pop(): T {
                        size -= 1
                        var elem = backing[size].get()
                        backing[size] = new()
                        elem
                    }
                    
                    //Functional helpers
                    
                    pub fn forEach(func: T -> ()) {
                        var i = 0u32
                        while i < #this {
                            func(this[i])
                            i += 1;
                        };
                    }
                    
                    pub fn map<R>(func: T -> R): List<R> {
                        var res = new List<R>(#this)
                        var i = 0u32
                        while i < #this {
                            res += func(this[i])
                            i += 1;
                        }
                        res
                    }
                    
                    pub fn mapIndexed<R>(func: (T, u32) -> R): List<R> {
                        var res = new List<R>(#this)
                        var i = 0u32
                        while i < #this {
                            res += func(this[i], i)
                            i += 1;
                        }
                        res
                    }
                    
                    pub fn filter(func: T -> bool): List<T> {
                        var res = new List<T>()
                        var i = 0u32
                        while i < #this {
                            if func(this[i])
                                res += this[i]
                            i += 1;
                        }
                        res
                    }
                    
                }
                
                """);
    }
}
