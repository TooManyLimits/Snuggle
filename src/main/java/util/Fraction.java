package util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

//Denominator should never be negative or 0
public class Fraction extends Number implements Comparable<Fraction> {

    public final BigInteger numerator, denominator;

    public Fraction(BigInteger numerator, BigInteger denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    /**
     * Creation
     */

    public static Fraction parseFraction(String str) {
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

    public Fraction remainder(Fraction other) {
        if (denominator.equals(other.denominator))
            return reduce(numerator.remainder(other.numerator), denominator);
        BigInteger newDenominator = denominator.multiply(other.denominator);
        return reduce(numerator.multiply(other.denominator).remainder(other.numerator.multiply(denominator)), newDenominator);
    }

    public int compareTo(Fraction other) {
        if (denominator.equals(other.denominator))
            return numerator.compareTo(other.numerator);
        return numerator.multiply(other.denominator).compareTo(other.numerator.multiply(denominator));
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

    @Override
    public byte byteValue() {
        return numerator.divide(denominator).byteValue();
    }

    @Override
    public short shortValue() {
        return numerator.divide(denominator).shortValue();
    }

    @Override
    public int intValue() {
        return numerator.divide(denominator).intValue();
    }

    @Override
    public long longValue() {
        return numerator.divide(denominator).longValue();
    }

    public float floatValue() {
        MathContext context = new MathContext(10);
        return new BigDecimal(numerator).divide(new BigDecimal(denominator), context).floatValue();
    }

    public double doubleValue() {
        MathContext context = new MathContext(20);
        return new BigDecimal(numerator).divide(new BigDecimal(denominator), context).doubleValue();
    }
}
