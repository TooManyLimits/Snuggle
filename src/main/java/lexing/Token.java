package lexing;

import exceptions.compile_time.CompilationException;
import exceptions.compile_time.LexingException;
import util.Fraction;
import util.IntLiteralData;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public record Token(Loc loc, TokenType type, Object value) {

    public String string() {
        return (String) value;
    }

    private static final Pattern WORD_REGEX = Pattern.compile("[a-zA-Z_]\\w*");

    public static Token of(String fileName, String text, int line, int col) throws CompilationException {
        if (text.isBlank() || text.startsWith("//") || text.startsWith("/*"))
            return null;

        int endLine = line; //Currently redundant since tokens can't span lines, but maybe useful later
        int endCol = col + text.length();
        Loc loc = new Loc(fileName, line, col, endLine, endCol);

        //Check if there's a basic token with this text, if so return one right away
        if (BASIC_TOKENS.containsKey(text))
            return BASIC_TOKENS.get(text).get(fileName, line, col);

        //Otherwise, check special cases:
        if (Character.isDigit(text.charAt(0))) {

            //Floating point
            boolean floatAnnotated = text.indexOf('f') != -1;
            if (floatAnnotated || text.indexOf('.') != -1) {
                if (floatAnnotated) {
                    String withoutEnding = text.substring(0, text.length() - 3);
                    if (text.endsWith("f32"))
                        return new Token(loc, TokenType.FLOAT_LITERAL, Float.parseFloat(withoutEnding));
                    else if (text.endsWith("f64"))
                        return new Token(loc, TokenType.FLOAT_LITERAL, Double.parseDouble(withoutEnding));
                    else
                        throw new IllegalStateException("Bug in lexer - only f32 and f64 expected, but got numeric token " + text);
                } else {
                    return new Token(loc, TokenType.FLOAT_LITERAL, Fraction.parseFraction(text));
                }
            }

            //Integer
            int index = text.indexOf('i');
            boolean signed = index != -1;
            if (signed || (index = text.indexOf('u')) != -1) {
                int bits = Integer.parseInt(text.substring(index + 1));
                String actualContent = text.substring(0, index);
                return new Token(loc, TokenType.INT_LITERAL, new IntLiteralData(new BigInteger(actualContent), signed, bits));
            } else {
                return new Token(loc, TokenType.INT_LITERAL, new IntLiteralData(new BigInteger(text), false, 0));
            }
        }

        //Handle identifier
        if (WORD_REGEX.matcher(text).matches())
            return new Token(loc, TokenType.IDENTIFIER, text);

        //Char literal
        if (text.startsWith("'")) {
            int[] i = new int[]{1};
            char c = text.charAt(i[0]);
            if (c == '\\') {
                return new Token(loc, TokenType.CHAR_LITERAL, handleEscape(text, i, loc));
            } else {
                return new Token(loc, TokenType.CHAR_LITERAL, c);
            }
        }

        //String literal
        if (text.startsWith("\"")) {
            if (text.length() == 1 || !text.endsWith("\""))
                throw new LexingException("Encountered unmatched quote", loc);
            StringBuilder builder = new StringBuilder();
            for (int[] i = new int[]{1}; i[0] < text.length()-1; i[0]++) {
                char c = text.charAt(i[0]);
                if (c == '\\') {

                    builder.append(handleEscape(text, i, loc));
                } else {
                    builder.append(c);
                }
            }

            return new Token(loc, TokenType.STRING_LITERAL, builder.toString());
        }

        //If none of these, must be an error
        throw new LexingException("Encountered invalid token \"" + text + "\"", loc);
    }

    //i[0] currently points to a backslash in text
    private static char handleEscape(String text, int[] i, Loc loc) throws CompilationException {
        i[0]++;
        char next = text.charAt(i[0]);
        return switch (next) {
            case 'b' -> '\b';
            case 't' -> '\t';
            case 'n' -> '\n';
            case 'f' -> '\f';
            case 'r' -> '\r';
            case '\'' -> '\'';
            case '"' -> '"';
            case '\\' -> '\\';
            case 'u' -> {
                int num = 0;
                for (int j = 0; j < 4; j++) {
                    i[0]++;
                    num = num * 16 + Character.digit(text.charAt(i[0]), 16);
                }
                yield (char) num;
            }
            case '0', '1', '2', '3' -> {
                char digit2 = text.charAt(i[0] + 1);
                if ('0' <= digit2 && digit2 <= '7') {
                    char digit3 = text.charAt(i[0] + 2);
                    if ('0' <= digit3 && digit3 <= '7') {
                        i[0] += 2;
                        yield (char) (Character.digit(next, 8) * 64 + Character.digit(digit2, 8) * 8 + Character.digit(digit3, 8));
                    } else {
                        i[0] += 1;
                        yield (char) (Character.digit(next, 8) * 8 + Character.digit(digit2, 8));
                    }
                } else {
                    yield (char) (Character.digit(next, 8));
                }
            }
            case '4', '5', '6', '7' -> {
                char digit2 = text.charAt(i[0] + 1);
                if ('0' <= digit2 && digit2 <= '7') {
                    i[0] += 1;
                    yield (char) (Character.digit(next, 8) * 8 + Character.digit(digit2, 8));
                }
                yield (char) (Character.digit(next, 8));
            }
            default -> throw new LexingException("Illegal escape character \"\\" + next + "\"", loc);
        };
    }

    @Override
    public String toString() {
        String x = type.toString();
        if (value != null)
            x += "(" + value + ")";
        return x + " at " + loc().startLine() + ":" + loc().startColumn();
    }

    //Used for keywords and other tokens that are always the same string
    private static final Map<String, TokenGetter> BASIC_TOKENS = new HashMap<>();

    static {
        for (TokenType type : TokenType.values())
            for (String alias : type.exactStrings)
                BASIC_TOKENS.put(alias, (f, l, c) -> new Token(new Loc(f, l, c, l, c + alias.length()), type, null));
        BASIC_TOKENS.put("true", (f, l, c) -> new Token(new Loc(f, l, c, l, c + "true".length()), TokenType.BOOL_LITERAL, true));
        BASIC_TOKENS.put("false", (f, l, c) -> new Token(new Loc(f, l, c, l, c + "false".length()), TokenType.BOOL_LITERAL, false));
    }

    @FunctionalInterface
    private interface TokenGetter {
        Token get(String fileName, int line, int col);
    }

}