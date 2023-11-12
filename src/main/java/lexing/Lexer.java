package lexing;

import exceptions.compile_time.CompilationException;
import exceptions.compile_time.LexingException;
import exceptions.compile_time.ParsingException;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * The Lexer can be separated out into its own
 * distinct class, because its only purpose is to provide
 * a steady stream of Tokens. The ast.passes.Parser and Compiler
 * are linked together because we don't want to deal
 * with a large AST data structure.
 */
public class Lexer {

    //If we declare that the longest text object allowed is some characters
    //Then we can use scanner horizon with the regex
    private static final Pattern REGEX = Pattern.compile(
            //Single line comments
            "//.*" + "|" +
            //Multi line comments
            "/\\*(\\*(?!/)|[^*])*\\*/" + "|" + //https://stackoverflow.com/questions/16160190/regular-expression-to-find-c-style-block-comments
            //Unique 2-character operators and ASSIGN variants, **, &&, ||, **=, &&=, ||=
            "(?:(\\*\\*)|&&|\\|\\|)=?" + "|" +
            //Assign variants for bit shifts, <<= and >>=
            ">>=|<<=" + "|" +
            //Other 2-character symbols, .. :: -> =>
            "\\.\\.|::|->|=>" + "|" +
            //1-character operators and versions with = after
            //ex. + and +=, < and <=, ! and !=, = and ==, even though these are very different situations
            "[-+*/%=&|^><!]=?" + "|" +
            //Other punctuation, () [] {} . : ; ? # ~
            "[()\\[\\]{}.:;,?#~]" + "|" +
            //Number literals
            "\\d+(?:(\\.\\d+(?:f32|f64)?)|(?:i8|u8|i16|u16|i32|u32|i64|u64|f32|f64)?)?" + "|" +
            //Identifiers
            "[a-zA-Z_]\\w*" + "|" +
            //String literals
            "\"(?:\\\\.|[^\\\\\"])*\"" + "|" +
            //Newlines
            "\n" + "|" +
            //Anything else
            "."
    );
    private static final int MAX_TOKEN_CHARS = 1024;

    //The reader and the scanner
    private final PushbackReader reader;
    private final Scanner scanner;
    private int line = 1; //"line 1" is the first line, generally speaking.
    private int col = 0; //column 0 is the first character
    public final String fileName; //used for error reporting, so we know which file a lexing error occurred in

    //The next token, if known.
    //When we peek(), if the next is unknown (null), we calculate it.
    //If it is known, then we just return it.
    //When we advance, we set to null.
    private Token next;
    //The token before next.
    //We set this to next when we advance.
    private Token last;

    //File methodName
    public Lexer(String fileName, Reader reader) {
        this.reader = new PushbackReader(reader);
        this.scanner = new Scanner(reader);
        this.fileName = fileName;
    }

    public Lexer(String fileName, String code) {
        this(fileName, new StringReader(code));
    }

    public boolean isDone() throws CompilationException {
        return peek().type() == TokenType.EOF;
    }

    public Token peek() throws CompilationException {
        while (next == null) {
            String res = scanner.findWithinHorizon(REGEX, 0);
            if (res == null) {
                try {
                    int nextChar = reader.read();
                    if (nextChar == -1)
                        return next = new Token(new Loc(fileName, line, 0, line, 0), TokenType.EOF, null);

                    throw new LexingException("Unexpected character \"" + nextChar + "\" on line " + line, new Loc(fileName, line, col, line, col + 1));
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read next token during lexing error", e);
                }
            }
            if (res.contains("\n")) {
                String[] substrings = res.split("\n", -1);
                line += substrings.length - 1;
                col = substrings[substrings.length - 1].length();
                continue;
            }
            next = Token.of(fileName, res, line, col);
            col += res.length();
        }
        return next;
    }

    public Token last() {
        return last;
    }

    public void advance() throws CompilationException {
        if (next == null) peek();
        last = next;
        next = null;
//        System.out.println(last);
    }

    public Token take() throws CompilationException {
        advance();
        return last;
    }

    public boolean check(TokenType type) throws CompilationException {
        if (peek() == null) return false;
        return peek().type() == type;
    }
    public boolean check(TokenType... types) throws CompilationException {
        if (peek() == null) return false;
        for (TokenType type : types)
            if (peek().type() == type)
                return true;
        return false;
    }
    public boolean checkBetween(TokenType first, TokenType last) throws CompilationException {
        if (first.ordinal() > last.ordinal()) throw new IllegalArgumentException("checkBetween args must have first <= last");
        if (peek() == null) return false;
        return peek().type().ordinal() >= first.ordinal() && peek().type().ordinal() <= last.ordinal();
    }
    public boolean consume(TokenType type) throws CompilationException {
        boolean x = check(type);
        if (x) advance();
        return x;
    }
    public boolean consume(TokenType... types) throws CompilationException {
        boolean x = check(types);
        if (x) advance();
        return x;
    }

    //Consume anything between these two token topLevelTypes in the enum, inclusive
    public boolean consumeBetween(TokenType first, TokenType last) throws CompilationException {
        if (first.ordinal() > last.ordinal()) throw new IllegalArgumentException("consumeBetween args must have first <= last");
        boolean x = checkBetween(first, last);
        if (x) advance();
        return x;
    }
    public Token expect(TokenType type, String message) throws CompilationException {
        if (!check(type))
            throw new ParsingException(message, peek().loc());
        return take();
    }

    public Token expect(TokenType type, String message, Loc startLoc) throws CompilationException {
        if (!check(type))
            throw new ParsingException(message, Loc.merge(startLoc, peek().loc()));
        return take();
    }

}
