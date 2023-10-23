package lexing;

public enum TokenType {

    //Different
    EOF,

    //Types with values
    BOOL_LITERAL, //Boolean
    STRING_LITERAL, //String
    INT_LITERAL, //BigInteger
    FLOAT_LITERAL, //Float, Double, or Fraction

    IDENTIFIER,

    //Symbols

    //Arithmetic Operators
    PLUS("+"),
    MINUS("-"),
    STAR("*"),
    SLASH("/"),
    PERCENT("%"),
    POWER("**"),

    //Bitwise operators
    BITWISE_AND("&"),
    BITWISE_OR("|"),
    BITWISE_XOR("^"),

    //Boolean operators
    AND("&&"),
    OR("||"),
    //Range operator
    RANGE(".."),

    //Assignments
    ASSIGN("="),
    PLUS_ASSIGN("+="),
    MINUS_ASSIGN("-="),
    TIMES_ASSIGN("*="),
    DIVIDE_ASSIGN("/="),
    MODULO_ASSIGN("%="),
    POWER_ASSIGN("**="),
    BITWISE_AND_ASSIGN("&="),
    BITWISE_OR_ASSIGN("|="),
    BITWISE_XOR_ASSIGN("^="),
    BITWISE_NOT_ASSIGN("~="),
    AND_ASSIGN("&&="),
    OR_ASSIGN("||="),

    //Comparisons
    GREATER(">"),
    LESS("<"),
    GREATER_EQUAL(">="),
    LESS_EQUAL("<="),
    EQUAL("=="),
    NOT_EQUAL("!="),

    //Other binary
    AS("as"),

    //Unary
    NOT("!"),
    BITWISE_NOT("~"),

    QUESTION_MARK("?"),

    //Brackets
    LEFT_PAREN("("),
    RIGHT_PAREN(")"),
    LEFT_SQUARE("["),
    RIGHT_SQUARE("]"),
    LEFT_CURLY("{"),
    RIGHT_CURLY("}"),

    //Punctuation
    DOT("."),
    COLON(":"),
    SEMICOLON(";"),
    COMMA(","),

    //in-class things
    THIS("this"),
    SUPER("super"),

    //Declarations and such
    VAR("var"),
    NEW("new"),

    PUB("pub"),
    IMPORT("import"),
    CLASS("class"),
    STRUCT("struct"), // structs :flushed:
    ENUM("enum"),

    //Function related thingis
    RETURN("return"),
    FN("fn"),

    //Control flow
    WHILE("while"),
    FOR("for"),
    IN("in"), //Just used in for loops
    BREAK("break"),
    IF("if"),
    ELSE("else"),

    ;
    /**
     * The exact string of this token. However,
     * if the token does not always follow the
     * same string, then it's null.
     */
    public final String[] exactStrings;

    TokenType(String... exactStrings) {
        this.exactStrings = exactStrings;
    }
}
