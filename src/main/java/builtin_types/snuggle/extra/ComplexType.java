package builtin_types.snuggle.extra;

import builtin_types.snuggle.SnuggleDefinedType;

public class ComplexType extends SnuggleDefinedType {

    public static final ComplexType INSTANCE = new ComplexType();

    private ComplexType() {
        super("complex", true, """
                
                pub struct Complex<T> {
                    pub var real: T
                    pub var imag: T
                    
                    pub static var ONE: Complex<T>
                    pub static var I: Complex<T>
                    
                    static {
                        Complex::<T>.ONE = new {1, 0}
                        Complex::<T>.I = new {0, 1};
                    }
                    
                    pub static fn sumComponents(e: Complex<T>): T
                        e.real + e.imag
                    
                    pub fn eq(o: Complex<T>): bool
                        real == o.real && imag == o.imag
                    
                    pub fn add(o: Complex<T>): Complex<T>
                        new {real + o.real, imag + o.imag}
                    pub fn add(o: T): Complex<T>
                        new {real + o, imag}
                        
                    pub fn sub(o: Complex<T>): Complex<T>
                        new {real - o.real, imag - o.imag}
                    pub fn sub(o: T): Complex<T>
                        new {real - o, imag}
                    
                    pub fn len2(): T
                        real * real + imag * imag
                    
                    pub fn mul(o: Complex<T>): Complex<T>
                        new {real * o.real - imag * o.imag, real * o.imag + imag * o.real}
                    pub fn mul(o: T): Complex<T>
                        new {real * o, imag * o}
                    
                    pub fn div(o: Complex<T>): Complex<T> {
                        var len2 = o.len2()
                        new {
                            {real * o.real + imag * o.imag} / len2,
                            {imag * o.real - real * o.imag} / len2
                        }
                    }
                    pub fn div(o: T): Complex<T>
                        new {real / o, imag / o}
                    
                }
                
                """);
    }
}
