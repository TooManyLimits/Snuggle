package util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

//Denominator should never be negative or 0
public record Fraction(BigInteger numerator, BigInteger denominator) {

    /**
     * Creation
     */

    public static Fraction of(String str) {
        int dotIndex = str.indexOf('.');
        if (dotIndex == -1) return new Fraction(new BigInteger(str), BigInteger.ONE);
        BigInteger numerator = new BigInteger(str.substring(0, dotIndex) + str.substring(dotIndex + 1));
        int denomPower = str.length() - dotIndex - 1;
        BigInteger denominator = BigInteger.TEN.pow(denomPower);
        return reduce(numerator, denominator);
    }

    /**
     * Arithmetic
     */

    public Fraction add(Fraction other) {
        if (denominator.equals(other.denominator))
            return reduce(numerator.add(other.numerator), denominator);
        BigInteger newDenominator = denominator.multiply(other.denominator);
        return reduce(numerator.multiply(other.denominator).add(other.numerator.multiply(denominator)), newDenominator);
    }

    public Fraction subtract(Fraction other) {
        if (denominator.equals(other.denominator))
            return reduce(numerator.subtract(other.numerator), denominator);
        BigInteger newDenominator = denominator.multiply(other.denominator);
        return reduce(numerator.multiply(other.denominator).subtract(other.numerator.multiply(denominator)), newDenominator);
    }

    public Fraction multiply(Fraction other) {
        return reduce(numerator.multiply(other.numerator), denominator.multiply(other.denominator));
    }

    public Fraction divide(Fraction other) {
        return reduce(numerator.multiply(other.denominator), denominator.multiply(other.numerator));
    }

    public Fraction negate() {
        return new Fraction(numerator.negate(), denominator);
    }

    private static Fraction reduce(BigInteger numerator, BigInteger denominator) {
        BigInteger gcd = numerator.gcd(denominator);
        if (gcd.equals(BigInteger.ONE))
            return new Fraction(numerator, denominator);
        else
            return new Fraction(numerator.divide(gcd), denominator.divide(gcd));
    }

    /**
     * Conversion
     */

    public float floatValue() {
        MathContext context = new MathContext(10);
        return new BigDecimal(numerator).divide(new BigDecimal(denominator), context).floatValue();
    }

    public double doubleValue() {
        MathContext context = new MathContext(20);
        return new BigDecimal(numerator).divide(new BigDecimal(denominator), context).doubleValue();
    }

}
