package util;

import java.math.BigInteger;

//If bits is 0, then the annotatedType is unspecified, and signed is undefined.
public record IntLiteralData(BigInteger value, boolean signed, int bits) {

    public boolean isSpecified() {
        return bits != 0;
    }

}
