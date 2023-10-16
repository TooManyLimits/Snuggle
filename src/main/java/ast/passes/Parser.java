package ast.passes;

import ast.parsed.ParsedType;
import ast.parsed.def.method.SnuggleParsedMethodDef;
import ast.parsed.def.type.ParsedClassDef;
import ast.parsed.def.type.ParsedTypeDef;
import ast.parsed.expr.*;
import ast.parsed.prog.ParsedAST;
import ast.parsed.prog.ParsedFile;
import exceptions.CompilationException;
import exceptions.ParsingException;
import lexing.Lexer;
import lexing.Loc;
import lexing.Token;
import lexing.TokenType;
import runtime.Unit;
import util.ListUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static lexing.TokenType.*;

/**
 * The parse class is used to convert streams of tokens (Lexer) into parsed data.
 *
 * Note: Many things in this parser, as well as many of the implementations of
 * ParsedExpr, do not currently have any real code to them. They're only
 * there in preparation for the future.
 *
 */
public class Parser {

    /**
     * Parses all the given files and results in a ParsedAST
     */
    public static ParsedAST parse(Map<String, Lexer> files) throws CompilationException {
        //Verify that there's a main file
        if (!files.containsKey("main"))
            throw new IllegalArgumentException("Expected main file, but could not find any");
        //Parse files
        List<ParsedFile> parsedFiles = new ArrayList<>(files.size());
        for (var file : files.entrySet())
            parsedFiles.add(new Parser(file.getValue()).parseFile(file.getKey()));
        //Return
        return new ParsedAST(parsedFiles);
    }

    private final Lexer lexer;

    private Parser(Lexer lexer) {
        this.lexer = lexer;
    }

    /**
     *
     *
     * FILE AND PROGRAM PARSING
     *
     *
     */

    private ParsedFile parseFile(String fileName) throws CompilationException {
        //Find all the imports
        List<ParsedImport> imports = new ArrayList<>();
        while (lexer.consume(IMPORT))
            imports.add(parseImport());
        //Parse types
        List<ParsedTypeDef> parsedTypeDefs = new ArrayList<>();
        while (lexer.check(PUB, CLASS)) {
            boolean pub = lexer.consume(PUB);
            if (lexer.consume(CLASS))
                parsedTypeDefs.add(parseClass(pub));
            else
                throw new ParsingException("Expected annotatedType def, found " + lexer.peek().type(), lexer.peek().loc());
        }
        //Parse exprs
        ArrayList<ParsedExpr> code = new ArrayList<>();
        while (!lexer.check(EOF))
            code.add(parseExpr(List.of(), List.of(), true));
        code.trimToSize();
        //Return the file
        return new ParsedFile(fileName, imports, parsedTypeDefs, code);
    }

    //The "class" token was just consumed
    private ParsedTypeDef parseClass(boolean pub) throws CompilationException {
        //Get class methodName
        Token className = lexer.expect(IDENTIFIER, "Expected class methodName after \"class\", but got " + lexer.peek().type());
        //Parse generics and their bounds
        List<GenericDef> typeGenerics = parseGenerics();
        //Parse the supertype, if there is one (colon)
        ParsedType superType = lexer.consume(COLON) ? parseType(lexer.last(), typeGenerics, List.of()) : null;
        if (superType instanceof ParsedType.Generic) //Ensure supertype is not a generic (can't have class Funny<T>: T {...})
            throw new ParsingException("Cannot use generic annotatedType as supertype", className.loc());
        Loc leftCurlyLoc = lexer.expect(LEFT_CURLY, "Expected left curly brace to begin class definition", className.loc()).loc();
        ArrayList<SnuggleParsedMethodDef> methods = new ArrayList<>();
        while (!lexer.consume(RIGHT_CURLY)) {
            if (lexer.check(EOF))
                throw new ParsingException("Unmatched class definition curly brace {", leftCurlyLoc);
            boolean pubMember = lexer.consume(PUB);
            if (lexer.consume(FN))
                methods.add(parseMethod(pubMember, typeGenerics));
            else
                throw new ParsingException("Expected method def, found " + lexer.peek().type(), lexer.peek().loc());
        }
        return new ParsedClassDef(className.loc(), pub, className.string(), typeGenerics.size(), (ParsedType.Basic) superType, methods);
    }

    //"fn" was already consumed
    private SnuggleParsedMethodDef parseMethod(boolean pub, List<GenericDef> typeGenerics) throws CompilationException {
        if (lexer.consume(NEW)) {
            //Constructor mode. Similar, but slightly different.
            String methodName = "new";
            Loc methodLoc = lexer.last().loc();
            List<GenericDef> methodGenerics = parseGenerics();
            lexer.expect(LEFT_PAREN, "Expected ( to begin params list for method \"" + methodName + "\"", methodLoc);
            List<ParsedParam> params = parseParams(typeGenerics, methodGenerics, new Token(methodLoc, IDENTIFIER, methodName));

            //TODO: Fix unit type
            //ParsedType returnType = ParsedType.Tuple.UNIT;
            ParsedType returnType = new ParsedType.Basic("unit", List.of());

            ParsedExpr body = parseExpr(typeGenerics, methodGenerics, false);
            //TODO: Static methods, right now just always false
            List<String> paramNames = ListUtils.map(params, ParsedParam::name);
            List<ParsedType> paramTypes = ListUtils.map(params, ParsedParam::type);
            return new SnuggleParsedMethodDef(methodLoc, pub, false, methodName, methodGenerics.size(), paramNames, paramTypes, returnType, body);
        }
        Token methodName = lexer.expect(IDENTIFIER, "Expected method methodName after \"fn\", but got " + lexer.peek().type());
        List<GenericDef> methodGenerics = parseGenerics();
        lexer.expect(LEFT_PAREN, "Expected ( to begin params list for method \"" + methodName.string() + "\"", methodName.loc());
        List<ParsedParam> params = parseParams(typeGenerics, methodGenerics, methodName);

        //TODO: Fix unit type
        //ParsedType returnType = lexer.consume(COLON) ? parseType(lexer.last(), typeGenerics, methodGenerics) : null;
        ParsedType returnType = lexer.consume(COLON) ? parseType(lexer.last(), typeGenerics, methodGenerics) : new ParsedType.Basic("unit", List.of());

        ParsedExpr body = parseExpr(typeGenerics, methodGenerics, false);
        //TODO: Static methods, right now just always false
        List<String> paramNames = ListUtils.map(params, ParsedParam::name);
        List<ParsedType> paramTypes = ListUtils.map(params, ParsedParam::type);
        return new SnuggleParsedMethodDef(methodName.loc(), pub, false, methodName.string(), methodGenerics.size(), paramNames, paramTypes, returnType, body);
    }

    // Parse a params list, not to be confused with an args list
    // The "(" was already consumed
    private List<ParsedParam> parseParams(List<GenericDef> typeGenerics, List<GenericDef> methodGenerics, Token methodName) throws CompilationException {
        if (lexer.consume(RIGHT_PAREN))
            return List.of();
        ArrayList<ParsedParam> params = new ArrayList<>();

        //Parse one at least, then continue parsing while we have commas
        String paramName = lexer.expect(IDENTIFIER, "Expected ) to end params list for method \"" + methodName.string() + "\"", methodName.loc()).string();
        Token colonTok = lexer.expect(COLON, "Parameter \"" + paramName + "\" in method \"" + methodName.string() + "\" is missing a annotatedType annotation", methodName.loc());
        params.add(new ParsedParam(paramName, parseType(colonTok, typeGenerics, methodGenerics)));
        while (lexer.consume(COMMA)) {
            paramName = lexer.expect(IDENTIFIER, "Expected ) to end params list for method \"" + methodName.string() + "\"", methodName.loc()).string();
            colonTok = lexer.expect(COLON, "Parameter \"" + paramName + "\" in method \"" + methodName.string() + "\" is missing a annotatedType annotation", methodName.loc());
            params.add(new ParsedParam(paramName, parseType(colonTok, typeGenerics, methodGenerics)));
        }
        //Expect a closing paren
        lexer.expect(RIGHT_PAREN, "Expected ) to end params list for method \"" + methodName.string() + "\"", methodName.loc());
        //Return
        return params;
    }

    private record ParsedParam(String name, ParsedType type) {}

    /**
     *
     *
     * EXPRESSION PARSING
     *
     *
     */

    /**
     * canBeDeclaration notes:
     * - Generally, declarations are allowed, but they are disallowed inside any of the following contexts:
     * - - Inside an argument to a method/constructor call. This includes being the rhs of a binary operator.
     * - - Inside the condition or branches of an if-expression.
     * - - Inside the condition or body of a while loop.
     * - - Inside the body of a method definition.
     * - However, declarations are made possible again as top-level expressions inside of blocks, except the last.
     * - - So, for example, if a block is an argument to a method, then declarations are still legal inside that block.
     * - - If the context of the block cannot contain a declaration, the last expression in the block cannot either.
     */
    private ParsedExpr parseExpr(List<GenericDef> classGenerics, List<GenericDef> methodGenerics, boolean canBeDeclaration) throws CompilationException {
        if (lexer.consume(SEMICOLON))
            return new ParsedLiteral(lexer.last().loc(), Unit.INSTANCE);
        return parseBinary(0, classGenerics, methodGenerics, canBeDeclaration);
    }

    private ParsedExpr parseBinary(int precedence, List<GenericDef> classGenerics, List<GenericDef> methodGenerics, boolean canBeDeclaration) throws CompilationException {
        if (precedence >= TOK_TYPES.size())
            return parseUnary(classGenerics, methodGenerics, canBeDeclaration);
        ParsedExpr lhs = parseBinary(precedence + 1, classGenerics, methodGenerics, canBeDeclaration);
        int rhsPrecedence = RIGHT_ASSOCIATIVE.get(precedence) ? precedence : precedence + 1;
        while (lexer.consume(TOK_TYPES.get(precedence))) {
            TokenType op = lexer.last().type();

            String methodName = switch (op) {
                case PLUS -> "add";
                case MINUS -> "sub";
                case STAR -> "mul";
                case SLASH -> "div";
                case PERCENT -> "rem";
                case POWER -> "pow";

                case BITWISE_AND -> "band";
                case BITWISE_OR -> "bor";
                case BITWISE_XOR -> "bxor";

                //Special
                case AND, OR -> "SPECIAL_IGNORE";

                case EQUAL, NOT_EQUAL -> "eq";
                case GREATER -> "gt";
                case LESS -> "lt";
                case GREATER_EQUAL -> "ge";
                case LESS_EQUAL -> "le";
                default -> throw new IllegalStateException("parseBinary found invalid token \"" + op.exactStrings[0] + "\". Bug in compiler, please report!");
            };

            ParsedExpr rhs = parseBinary(rhsPrecedence, classGenerics, methodGenerics, false);
            Loc fullLoc = Loc.merge(lhs.loc(), rhs.loc());

            //Special handling for and/or operations. They can't be method calls
            //because of short-circuiting.
            if (op == AND || op == OR) {
                String tempVarName = "$desugarShortCircuit";
                ParsedExpr ifTrue = new ParsedVariable(fullLoc, tempVarName);
                ParsedExpr ifFalse = rhs;
                if (op == AND) {ParsedExpr temp = ifTrue; ifTrue = ifFalse; ifFalse = temp; } //swap branches if AND
                lhs = new ParsedBlock(fullLoc, List.of(
                        new ParsedDeclaration(fullLoc, tempVarName, new ParsedType.Basic("bool", List.of()), lhs),
                        new ParsedIf(fullLoc,
                                new ParsedVariable(fullLoc, tempVarName),
                                ifTrue,
                                ifFalse
                        )
                ));
                continue;
            }

            //Otherwise, operators are method calls
            lhs = new ParsedMethodCall(fullLoc, lhs, methodName, List.of(), List.of(rhs));

            //Special !=
            if (op == NOT_EQUAL)
                lhs = new ParsedMethodCall(lhs.loc(), lhs, "not", List.of(), List.of());

        }

        return lhs;
    }

    private ParsedExpr parseUnary(List<GenericDef> classGenerics, List<GenericDef> methodGenerics, boolean canBeDeclaration) throws CompilationException {
        if (lexer.consume(MINUS, NOT, BITWISE_NOT)) {
            String methodName = switch (lexer.last().type()) {
                case MINUS -> "neg";
                case NOT -> "not";
                case BITWISE_NOT -> "bnot";
                default -> throw new IllegalStateException("parseUnary found invalid token \"" + lexer.last().type() + "\". Bug in compiler, please report!");
            };
            Loc operatorLoc = lexer.last().loc();
            ParsedExpr operand = parseUnary(classGenerics, methodGenerics, canBeDeclaration);
            Loc fullLoc = Loc.merge(operatorLoc, operand.loc());
            return new ParsedMethodCall(fullLoc, operand, methodName, List.of(), List.of());
        }

        return parseCall(classGenerics, methodGenerics, canBeDeclaration);
    }

    private ParsedExpr parseCall(List<GenericDef> classGenerics, List<GenericDef> methodGenerics, boolean canBeDeclaration) throws CompilationException {
        ParsedExpr lhs = parseUnit(classGenerics, methodGenerics, canBeDeclaration);
        while (lexer.consume(LEFT_PAREN, DOT, LEFT_SQUARE)) {
            TokenType op = lexer.last().type();
            Loc loc = lexer.last().loc();
            switch (op) {
                case DOT -> {
                    Token name = lexer.expect(IDENTIFIER, "Expected methodName after \".\" at " + loc);
                    List<ParsedType> typeArgs = parseTypeArguments(classGenerics, methodGenerics);
                    if (lexer.consume(LEFT_PAREN)) {
                        List<ParsedExpr> args = parseArguments(classGenerics, methodGenerics);
                        Loc callLoc = Loc.merge(lhs.loc(), lexer.last().loc());
                        lhs = new ParsedMethodCall(callLoc, lhs, name.string(), typeArgs, args);
                    } else {
                        if (typeArgs.size() > 0)
                            throw new ParsingException("Fields cannot accept generic parameters: field \"" + name.string() + "\"", name.loc());
                        lhs = new ParsedFieldAccess(Loc.merge(lhs.loc(), name.loc()), lhs, name.string());
                        throw new IllegalStateException("Fields are not yet implemented");
                    }
                }
                case LEFT_PAREN -> lhs = new ParsedMethodCall(loc, lhs, "invoke", List.of(), parseArguments(classGenerics, methodGenerics));
                case LEFT_SQUARE -> {
                    throw new IllegalStateException("Indexing is not yet implemented");
                }
            }
        }

        //Check for assignment after, of any annotatedType (augmented or not)
        if (lexer.consumeBetween(ASSIGN, OR_ASSIGN)) {
            TokenType operator = lexer.last().type();
            Loc operatorLoc = lexer.last().loc();
            if ((lhs instanceof ParsedVariable || lhs instanceof ParsedFieldAccess)) {
                ParsedExpr rhs = parseExpr(classGenerics, methodGenerics, canBeDeclaration);
                Loc fullLoc = Loc.merge(lhs.loc(), rhs.loc());
                //Potentially modify expression, if this is augmented
                if (operator != ASSIGN) {
                    String methodName = switch (operator) {
                        case PLUS_ASSIGN -> "addAssign";
                        case MINUS_ASSIGN -> "subAssign";
                        case TIMES_ASSIGN -> "mulAssign";
                        case DIVIDE_ASSIGN -> "divAssign";
                        case MODULO_ASSIGN -> "remAssign";
                        case POWER_ASSIGN -> "powAssign";

                        //Special handling, short-circuiting
                        case OR_ASSIGN, AND_ASSIGN -> "IGNORE_SPECIAL";

                        case BITWISE_AND_ASSIGN -> "bandAssign";
                        case BITWISE_OR_ASSIGN -> "borAssign";
                        case BITWISE_XOR_ASSIGN -> "bxorAssign";
                        case BITWISE_NOT_ASSIGN -> "bnotAssign";

                        default -> throw new IllegalStateException("Parsing augmented assignment found invalid token \"" + operator.exactStrings[0] + "\". Bug in compiler, please report!");
                    };

                    //If the left was a field access, then we need to do some variable binding first.
                    if (lhs instanceof ParsedFieldAccess fieldAccess) {
                        //a.b += 5
                        //becomes
                        //{ var temp = a; temp.b = temp.b.plusEquals(5) }
                        //Important that the temp variable is not possible to be a regular identifier, otherwise this could mess with things
                        String tempVarName = "$desugarAugmentedAssignment";

                        //Special case for short-circuiting
                        if (operator == OR_ASSIGN || operator == AND_ASSIGN) {
                            ParsedExpr ifTrue = new ParsedFieldAccess(fullLoc, new ParsedVariable(fullLoc, tempVarName), fieldAccess.name());
                            ParsedExpr ifFalse = new ParsedAssignment(fullLoc, new ParsedFieldAccess(fullLoc, new ParsedVariable(fullLoc, tempVarName), fieldAccess.name()), rhs);
                            return new ParsedBlock(fullLoc, List.of(
                                    new ParsedDeclaration(fullLoc, tempVarName, null, fieldAccess.lhs()),
                                    new ParsedIf(fullLoc,
                                            new ParsedFieldAccess(fullLoc, new ParsedVariable(fullLoc, tempVarName), fieldAccess.name()),
                                            ifTrue,
                                            ifFalse
                                    )
                            ));
                        }

                        return new ParsedBlock(fullLoc, List.of(
                                //Declare the temp variable
                                new ParsedDeclaration(fullLoc, tempVarName, null, fieldAccess.lhs()),
                                //Assignment
                                new ParsedAssignment(fullLoc,
                                        //Target of assignment is temp.fieldName
                                        new ParsedFieldAccess(fullLoc, new ParsedVariable(fullLoc, tempVarName), fieldAccess.name()),
                                        //rhs is the method call
                                        new ParsedMethodCall(fullLoc,
                                                new ParsedFieldAccess(fullLoc, new ParsedVariable(fullLoc, tempVarName), fieldAccess.name()),
                                                methodName,
                                                List.of(),
                                                List.of(rhs)
                                        )
                                )
                        ));
                    } else {
                        //Special short-circuiting handling
                        if (operator == OR_ASSIGN || operator == AND_ASSIGN)  {
                            ParsedExpr ifTrue = lhs;
                            ParsedExpr ifFalse = new ParsedAssignment(fullLoc, lhs, rhs);
                            if (operator == AND_ASSIGN) {ParsedExpr temp = ifTrue; ifTrue = ifFalse; ifFalse = temp; } //swap branches if AND
                            //Just comepletely return here
                            return new ParsedIf(fullLoc,
                                    lhs,
                                    ifTrue,
                                    ifFalse
                            );
                        }
                        //It was a variable, so we can just call the method, and assign.
                        rhs = new ParsedMethodCall(fullLoc, lhs, methodName, List.of(), List.of(rhs));
                    }
                }
                //Merge the locations and create assignment
                lhs = new ParsedAssignment(fullLoc, lhs, rhs);
            } else {
                throw new ParsingException("Invalid assignment (" + operator.exactStrings[0] +  "). Can only assign to variables and fields.", operatorLoc);
            }
        }

        return lhs;
    }

    //If there's a <, then parses annotatedType arguments list
    //If there isn't, returns an empty list
    private List<ParsedType> parseTypeArguments(List<GenericDef> classGenerics, List<GenericDef> methodGenerics) throws CompilationException {
        if (lexer.consume(LESS)) {
            Token less = lexer.last();
            ArrayList<ParsedType> result = new ArrayList<>();
            result.add(parseType(less, classGenerics, methodGenerics));
            while (lexer.consume(COMMA))
                result.add(parseType(less, classGenerics, methodGenerics));
            lexer.expect(GREATER, "Expected > to end generic annotatedType arguments list that began at " + less.loc());
            result.trimToSize();
            return result;
        } else {
            return List.of();
        }
    }

    //The paren was just parsed
    private List<ParsedExpr> parseArguments(List<GenericDef> classGenerics, List<GenericDef> methodGenerics) throws CompilationException {
        if (lexer.consume(RIGHT_PAREN))
            return List.of();
        Loc parenLoc = lexer.last().loc();
        ArrayList<ParsedExpr> arguments = new ArrayList<>();
        arguments.add(parseExpr(classGenerics, methodGenerics, false));
        while (lexer.consume(COMMA))
            arguments.add(parseExpr(classGenerics, methodGenerics, false));
        lexer.expect(RIGHT_PAREN, "Expected ) to end args list", parenLoc);
        arguments.trimToSize();
        return arguments;
    }

    //Parse a "unit" expression, the smallest and tightest-grouped expressions
    private ParsedExpr parseUnit(List<GenericDef> classGenerics, List<GenericDef> methodGenerics, boolean canBeDeclaration) throws CompilationException {
        return switch (lexer.take().type()) {
            //Eof
            case EOF -> throw new ParsingException("Unexpected end of file, expected expression", lexer.last().loc());

            //Literals and objects
            case    INT_LITERAL,
                    BOOL_LITERAL -> new ParsedLiteral(lexer.last().loc(), lexer.last().value());
            case NEW -> parseConstructor(classGenerics, methodGenerics);

            //Identifiers
            case IDENTIFIER -> new ParsedVariable(lexer.last().loc(), lexer.last().string());
            case THIS -> new ParsedVariable(lexer.last().loc(), "this");

            //Control flow
            case IF -> parseIf(classGenerics, methodGenerics);
            case WHILE -> parseWhile(classGenerics, methodGenerics);
            case LEFT_CURLY -> parseBlock(classGenerics, methodGenerics);

            //Variable declaration
            case VAR -> parseDeclaration(classGenerics, methodGenerics, canBeDeclaration);

            //Imports
            case IMPORT -> parseImport();

            default -> throw new ParsingException("Unexpected token " + lexer.last().type() + ". Expected expression", lexer.last().loc());
        };
    }

    //The "import" token was already consumed
    private ParsedImport parseImport() throws CompilationException {
        Loc loc = lexer.last().loc();
        Token str = lexer.expect(STRING_LITERAL, "Expected string literal after import");
        return new ParsedImport(loc, str.string());
    }

    private ParsedExpr parseIf(List<GenericDef> classGenerics, List<GenericDef> methodGenerics) throws CompilationException {
        Loc ifLoc = lexer.last().loc();
        ParsedExpr cond = parseExpr(classGenerics, methodGenerics, false);
        ParsedExpr ifTrue = parseExpr(classGenerics, methodGenerics, false);
        ParsedExpr ifFalse = lexer.consume(ELSE) ? parseExpr(classGenerics, methodGenerics, false) : null;

        Loc fullLoc = Loc.merge(ifLoc, (ifFalse == null ? ifTrue : ifFalse).loc());
        return new ParsedIf(fullLoc, cond, ifTrue, ifFalse);
    }

    private ParsedExpr parseWhile(List<GenericDef> classGenerics, List<GenericDef> methodGenerics) throws CompilationException {
        Loc whileLoc = lexer.last().loc();
        ParsedExpr cond = parseExpr(classGenerics, methodGenerics, false);
        ParsedExpr body = parseExpr(classGenerics, methodGenerics, false);
        Loc fullLoc = whileLoc.merge(body.loc());
        return new ParsedWhile(fullLoc, cond, body);
    }

    private ParsedExpr parseBlock(List<GenericDef> classGenerics, List<GenericDef> methodGenerics) throws CompilationException {
        Loc loc = lexer.last().loc();
        //Empty block, return with List.of()
        if (lexer.consume(RIGHT_CURLY))
            return new ParsedBlock(Loc.merge(loc, lexer.last().loc()), List.of());

        //Non-empty block
        ArrayList<ParsedExpr> exprs = new ArrayList<>();
        while (!lexer.consume(RIGHT_CURLY)) {
            exprs.add(parseExpr(classGenerics, methodGenerics, true));
            if (lexer.isDone())
                throw new ParsingException("Unmatched {", loc);
        }
        return new ParsedBlock(Loc.merge(loc, lexer.last().loc()), exprs);
    }

    // "var" was just consumed
    private ParsedExpr parseDeclaration(List<GenericDef> classGenerics, List<GenericDef> methodGenerics, boolean canBeDeclaration) throws CompilationException {
        Loc varLoc = lexer.last().loc();
        String varName = lexer.expect(IDENTIFIER, "Expected methodName after \"var\" at " + varLoc).string();

        if (!canBeDeclaration)
            throw new ParsingException("Invalid declaration location for variable \"" + varName + "\"", varLoc);

        ParsedType annotatedType = lexer.consume(COLON) ? parseType(lexer.last(), classGenerics, methodGenerics) : null;
        lexer.expect(ASSIGN, "Expected '=' after variable \"" + varName + "\" at " + varLoc);
        ParsedExpr rhs = parseExpr(classGenerics, methodGenerics, true);
        return new ParsedDeclaration(Loc.merge(varLoc, rhs.loc()), varName, annotatedType, rhs);
    }

    private ParsedExpr parseConstructor(List<GenericDef> classGenerics, List<GenericDef> methodGenerics) throws CompilationException {
        Loc newLoc = lexer.last().loc();
        ParsedType constructedType = parseType(lexer.last(), classGenerics, methodGenerics);
        lexer.expect(LEFT_PAREN, "Expected args for constructor of annotatedType " + constructedType, newLoc);
        List<ParsedExpr> args = parseArguments(classGenerics, methodGenerics);
        Loc fullLoc = newLoc.merge(lexer.last().loc());
        return new ParsedConstructor(fullLoc, constructedType, args);
    }

    /**
     * Binary operator precedence and associativity
     */

    private static final ArrayList<TokenType[]> TOK_TYPES = new ArrayList<>();
    private static final ArrayList<Boolean> RIGHT_ASSOCIATIVE = new ArrayList<>();

    static {
        register(false, OR);
        register(false, AND);
        register(false, EQUAL, NOT_EQUAL);
        register(false, GREATER, LESS, GREATER_EQUAL, LESS_EQUAL);
        register(false, PLUS, MINUS, BITWISE_OR); //a + b + c == (a + b) + c
        register(false, STAR, SLASH, PERCENT, BITWISE_AND, BITWISE_XOR);
        register(true, POWER); //a ^ b ^ c == a ^ (b ^ c)
    }

    private static void register(boolean rightAssociative, TokenType... tokenTypes) {
        TOK_TYPES.add(tokenTypes);
        RIGHT_ASSOCIATIVE.add(rightAssociative);
    }


    /**
     *
     *
     * OTHER PARSE UTILS BELOW
     *
     *
     */

    private ParsedType parseType(Token startToken, List<GenericDef> typeGenerics, List<GenericDef> methodGenerics) throws CompilationException {
        String head = lexer.expect(IDENTIFIER, "Expected annotatedType after " + startToken.type().exactStrings[0], startToken.loc()).string();

        //If the annotatedType is a generic, return a generic TypeString
        //Search method generics first, as they're more recently defined,
        //and can shadow class generics
        int index = genericIndexOf(methodGenerics, head);
        if (index != -1)
            return new ParsedType.Generic(index, true);
        index = genericIndexOf(typeGenerics, head);
        if (index != -1)
            return new ParsedType.Generic(index, false);

        //Otherwise, return a Basic annotatedType
        if (lexer.consume(LESS)) {
            Loc lessLoc = lexer.last().loc();
            ArrayList<ParsedType> generics = new ArrayList<>();
            generics.add(parseType(startToken, typeGenerics, methodGenerics));
            while (lexer.consume(COMMA))
                generics.add(parseType(startToken, typeGenerics, methodGenerics));
            generics.trimToSize();
            lexer.expect(GREATER, "Expected > to end generics list", lessLoc);
            return new ParsedType.Basic(head, generics);
        } else {
            return new ParsedType.Basic(head, List.of());
        }
    }

    //Generic parsing stuff

    private record GenericDef(String name) {}

    private GenericDef parseGenericDef(Loc listLoc) throws CompilationException {
        String name = lexer.expect(IDENTIFIER, "Expected > to end generics list", listLoc).string();
        return new GenericDef(name);
    }

    //Allow indexOf() with a string on a generic def list
    private static int genericIndexOf(List<GenericDef> genericDefs, String name) {
        for (int i = 0; i < genericDefs.size(); i++)
            if (genericDefs.get(i).name().equals(name))
                return i;
        return -1;
    }

    private List<GenericDef> parseGenerics() throws CompilationException {
        if (lexer.consume(LESS)) {
            Loc loc = lexer.last().loc();
            if (lexer.consume(GREATER)) {
                throw new ParsingException("Expected generics list after <", loc);
            } else {
                ArrayList<GenericDef> generics = new ArrayList<>();
                generics.add(parseGenericDef(loc));
                while (lexer.consume(COMMA))
                    generics.add(parseGenericDef(loc));
                lexer.expect(GREATER, "Expected > to end generics list", loc);
                generics.trimToSize();
                return generics;
            }
        }
        return List.of();
    }

}
