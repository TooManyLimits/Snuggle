package ast.passes;

import ast.parsed.ParsedType;
import ast.parsed.def.field.SnuggleParsedFieldDef;
import ast.parsed.def.method.SnuggleParsedMethodDef;
import ast.parsed.def.type.ParsedClassDef;
import ast.parsed.def.type.ParsedEnumDef;
import ast.parsed.def.type.ParsedStructDef;
import ast.parsed.def.type.ParsedTypeDef;
import ast.parsed.expr.*;
import ast.parsed.prog.ParsedAST;
import ast.parsed.prog.ParsedFile;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.ParsingException;
import lexing.Lexer;
import lexing.Loc;
import lexing.Token;
import lexing.TokenType;
import util.ListUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
        ArrayList<ParsedFile> parsedFiles = new ArrayList<>(files.size());
        for (var file : files.entrySet())
            parsedFiles.add(new Parser(file.getValue()).parseFile(file.getKey()));
        //Return
        return new ParsedAST(parsedFiles);
    }

    public static ParsedFile parseFile(String name, Lexer lexer) throws CompilationException {
        return new Parser(lexer).parseFile(name);
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
        ArrayList<ParsedExpr> code = new ArrayList<>();
        ArrayList<ParsedTypeDef> topLevelTypes = new ArrayList<>();
        ArrayList<ParsedExtensionMethod> topLevelExtensionMethods = new ArrayList<>();
        while (!lexer.check(EOF)) {
            ParsedExpr e = parseExpr(List.of(), List.of(), true, false);
            if (e instanceof ParsedTypeDefExpr topLevelTypeDef)
                topLevelTypes.add(topLevelTypeDef.typeDef());
            else if (e instanceof ParsedExtensionMethod topLevelExtensionMethod)
                topLevelExtensionMethods.add(topLevelExtensionMethod);
            code.add(e);
        }
        code.trimToSize();
        topLevelTypes.trimToSize();
        topLevelExtensionMethods.trimToSize();
        return new ParsedFile(fileName, topLevelTypes, topLevelExtensionMethods, code);
    }

    //The "class"/"struct" token was just consumed
    private ParsedExpr parseClassOrStruct(boolean isClass, boolean pub, List<GenericDef> prevTypeGenerics, List<GenericDef> methodGenerics, boolean isNested) throws CompilationException {
        //Get type name
        String typeTypeString = isClass ? "class" : "struct";
        Token typeName = lexer.expect(IDENTIFIER, "Expected name after \"" + typeTypeString + "\", but got " + lexer.peek().type(), lexer.last().loc());
        //Parse generics
        List<GenericDef> typeGenerics = new ArrayList<>(prevTypeGenerics);
        typeGenerics.addAll(parseGenerics());
        //Parse the supertype, if there is one (colon)
        ParsedType superType = null;
        if (lexer.consume(COLON)) {
            if (isClass)
                superType = parseType(":", lexer.last().loc(), typeGenerics, List.of());
            else
                throw new ParsingException("Unexpected token COLON - structs cannot have a supertype!", lexer.last().loc());
        }
        if (superType instanceof ParsedType.Generic) //Ensure supertype is not a generic (can't have class Funny<T>: T {...})
            throw new ParsingException("Cannot use generic annotatedType as supertype", typeName.loc());
        Loc leftCurlyLoc = lexer.expect(LEFT_CURLY, "Expected left curly brace to begin class definition", typeName.loc()).loc();
        ArrayList<SnuggleParsedMethodDef> methods = new ArrayList<>();
        ArrayList<SnuggleParsedFieldDef> fields = new ArrayList<>();
        while (!lexer.consume(RIGHT_CURLY)) {
            if (lexer.check(EOF))
                throw new ParsingException("Unmatched " + typeTypeString + " definition curly brace {", leftCurlyLoc);
            boolean pubMember = lexer.consume(PUB);
            //"static fn" or "static var"
            if (lexer.consume(STATIC)) {
                if (lexer.consume(FN))
                    methods.add(parseMethod(isClass ? TypeType.CLASS : TypeType.STRUCT, true, typeName.string(), pubMember, typeGenerics, methodGenerics));
                else if (lexer.consume(VAR))
                    fields.add(parseField(isClass, pubMember, true, typeGenerics, methodGenerics));
                else if (lexer.consume(LEFT_CURLY))
                    methods.add(new SnuggleParsedMethodDef(lexer.last().loc(), false, true, "#init", 0, List.of(), List.of(), ParsedType.Tuple.UNIT, parseBlock(typeGenerics, methodGenerics)));
                else
                    throw new ParsingException("Expected \"fn\", \"var\", or initializer block after \"static\"", lexer.last().loc());
            }
            //Regular functions/fields (not static)
            else if (lexer.consume(FN))
                methods.add(parseMethod(isClass ? TypeType.CLASS : TypeType.STRUCT, false, typeName.string(), pubMember, typeGenerics, methodGenerics));
            else if (lexer.consume(VAR))
                fields.add(parseField(isClass, pubMember, false, typeGenerics, methodGenerics));
            else
                throw new ParsingException("Expected method or field definition for " + typeTypeString + " \"" + typeName.string() + "\", found " + lexer.peek().type(), lexer.peek().loc());
        }
        int numGenerics = typeGenerics.size() - prevTypeGenerics.size();
        return new ParsedTypeDefExpr(typeName.loc(), isClass ?
                new ParsedClassDef(typeName.loc(), pub, typeName.string(), numGenerics, isNested, (ParsedType.Basic) superType, methods, fields) :
                new ParsedStructDef(typeName.loc(), pub, typeName.string(), numGenerics, isNested, methods, fields)
        );
    }

    //"enum" was already consumed
    private ParsedExpr parseEnum(boolean pub, List<GenericDef> typeGenerics, List<GenericDef> methodGenerics, boolean isNested) throws CompilationException {
        Loc loc = lexer.last().loc();
        //Get type name
        String typeName = lexer.expect(IDENTIFIER, "Expected name after \"enum\", but got " + lexer.peek().type(), lexer.last().loc()).string();
        //Get the enum properties
        List<ParsedEnumDef.ParsedEnumProperty> properties = parseEnumProperties(loc, typeName, typeGenerics, methodGenerics);
        //Set up more lists
        ArrayList<ParsedEnumDef.ParsedEnumElement> elements = new ArrayList<>();
        ArrayList<SnuggleParsedMethodDef> methods = new ArrayList<>();
        //Loop through the body
        Loc leftCurlyLoc = lexer.expect(LEFT_CURLY, "Expected { to begin body for enum " + typeName).loc();
        while (!lexer.consume(RIGHT_CURLY)) {
            if (lexer.check(EOF))
                throw new ParsingException("Unmatched enum definition curly brace {", leftCurlyLoc);

            if (lexer.consume(PUB)) {
                if (lexer.consume(VAR))
                    throw new ParsingException("Cannot add fields to enum definition", lexer.last().loc()); //Nice error message in case someone tries that
                else if (lexer.consume(STATIC)) {
                    if (lexer.consume(VAR))
                        throw new ParsingException("Cannot add fields to enum definition", lexer.last().loc());
                    else if (lexer.consume(FN))
                        methods.add(parseMethod(TypeType.ENUM, true, typeName, true, typeGenerics, methodGenerics));
                } else if (lexer.consume(FN)) {
                    methods.add(parseMethod(TypeType.ENUM, false, typeName, true, typeGenerics, methodGenerics));
                } else {
                    throw new ParsingException("Expected function definition after \"pub\" inside enum \"" + typeName + "\"", loc);
                }
            } else if (lexer.consume(VAR))
                throw new ParsingException("Cannot add fields to enum definition", lexer.last().loc());
            else if (lexer.consume(STATIC)) {
                if (lexer.consume(VAR))
                    throw new ParsingException("Cannot add fields to enum definition", lexer.last().loc());
                else if (lexer.consume(FN))
                    methods.add(parseMethod(TypeType.ENUM, true, typeName, false, typeGenerics, methodGenerics));
            }
            else if (lexer.consume(FN))
                methods.add(parseMethod(TypeType.ENUM, false, typeName, false, typeGenerics, methodGenerics));
            else if (lexer.consume(IDENTIFIER)) {
                String name = lexer.last().string();
                Loc elemLoc = lexer.last().loc();

                //Parse the left paren (maybe), parse the arguments
                List<ParsedExpr> args;
                if (lexer.consume(LEFT_PAREN))
                    args = parseArguments(RIGHT_PAREN, typeGenerics, methodGenerics);
                else
                    args = List.of();

                //If it doesn't match up, then error
                if (args.size() != properties.size())
                    throw new ParsingException("Enum element \"" + name + "\" has incorrect number of args - the enum has " + properties.size() + " properties, but only " + args.size() + " args were provided.", elemLoc);
                elements.add(new ParsedEnumDef.ParsedEnumElement(name, args));
            } else {
                throw new ParsingException("Expected enum element or method definition, found " + lexer.peek().type(), lexer.peek().loc());
            }
        }
        elements.trimToSize();
        methods.trimToSize();
        return new ParsedTypeDefExpr(loc, new ParsedEnumDef(loc, pub, typeName, isNested, properties, elements, methods));
    }

    //
    private List<ParsedEnumDef.ParsedEnumProperty> parseEnumProperties(Loc loc, String enumName, List<GenericDef> typeGenerics, List<GenericDef> methodGenerics) throws CompilationException {
        //No parens -> no properties
        if (!lexer.consume(LEFT_PAREN))
            return List.of();
        //There was a paren, so let's start parsing
        ArrayList<ParsedEnumDef.ParsedEnumProperty> parsedProperties = new ArrayList<>();
        while (!lexer.consume(RIGHT_PAREN)) {
            boolean pub = lexer.consume(PUB);
            String name = lexer.expect(IDENTIFIER, "Expected name for enum property of \"" + enumName + "\", but got " + lexer.peek().type(), loc).string();
            Loc colonLoc = lexer.expect(COLON, "Enum property \"" + name + "\" in \"" + enumName + "\" is missing a annotatedType annotation").loc();
            ParsedType type = parseType(":", colonLoc, typeGenerics, methodGenerics);
            parsedProperties.add(new ParsedEnumDef.ParsedEnumProperty(pub, name, type));
            if (!lexer.consume(COMMA)) {
                //If no comma, break
                lexer.expect(RIGHT_PAREN, "Expected ) to end enum properties list for enum \"" + enumName + "\", but got " + lexer.peek().type(), loc);
                break;
            }
        }
        parsedProperties.trimToSize();
        return parsedProperties;
    }

    enum TypeType {
        CLASS, STRUCT, ENUM
    }

    //"fn" was already consumed
    private SnuggleParsedMethodDef parseMethod(TypeType typeType, boolean isStatic, String thisTypeName, boolean pub, List<GenericDef> typeGenerics, List<GenericDef> prevMethodGenerics) throws CompilationException {
        if (lexer.consume(NEW)) {
            //Constructor
            if (isStatic)
                throw new ParsingException("Constructors cannot be static", lexer.last().loc());
            String methodName = "new";
            Loc methodLoc = lexer.last().loc();
            List<GenericDef> methodGenerics = new ArrayList<>(prevMethodGenerics);
            methodGenerics.addAll(parseGenerics());
            lexer.expect(LEFT_PAREN, "Expected ( to begin params list for method \"" + methodName + "\"", methodLoc);
            List<ParsedParam> params = parseParams(typeGenerics, methodGenerics, methodLoc, "method \"" + methodName + "\"", false);

            //TODO: Fix unit type
            //ParsedType returnTypeGetter = ParsedType.Tuple.UNIT;
            AtomicInteger i = new AtomicInteger(); //cursed
            ParsedType returnType = switch (typeType) {
                case CLASS -> ParsedType.Tuple.UNIT; //Class constructors return unit
                case STRUCT -> new ParsedType.Basic(thisTypeName, ListUtils.map(typeGenerics, g -> new ParsedType.Generic(i.getAndIncrement(), false))); //Struct constructors return the type itself
                case ENUM -> throw new ParsingException("Cannot create constructor for enum", methodLoc);
            };

            ParsedExpr body = parseExpr(typeGenerics, methodGenerics, false, true);
            List<String> paramNames = ListUtils.map(params, ParsedParam::name);
            List<ParsedType> paramTypes = ListUtils.map(params, ParsedParam::type);
            return new SnuggleParsedMethodDef(methodLoc, pub, false, methodName, methodGenerics.size(), paramNames, paramTypes, returnType, body);
        } else {
            //Not constructor

            Token methodName = lexer.expect(IDENTIFIER, "Expected method methodName after \"fn\", but got " + lexer.peek().type());
            List<GenericDef> methodGenerics = parseGenerics();
            lexer.expect(LEFT_PAREN, "Expected ( to begin params list for method \"" + methodName.string() + "\"", methodName.loc());
            List<ParsedParam> params = parseParams(typeGenerics, methodGenerics, methodName.loc(), "method \"" + methodName + "\"", false);

            //ParsedType returnTypeGetter = lexer.consume(COLON) ? parseType(lexer.last(), typeGenerics, methodGenerics) : null;
            ParsedType returnType = lexer.consume(COLON) ? parseType(":", lexer.last().loc(), typeGenerics, methodGenerics) : ParsedType.Tuple.UNIT;

            ParsedExpr body = parseExpr(typeGenerics, methodGenerics, false, true);
            List<String> paramNames = ListUtils.map(params, ParsedParam::name);
            List<ParsedType> paramTypes = ListUtils.map(params, ParsedParam::type);
            return new SnuggleParsedMethodDef(methodName.loc(), pub, isStatic, methodName.string(), methodGenerics.size(), paramNames, paramTypes, returnType, body);
        }
    }

    // Parse a params list, not to be confused with an args list
    // The "(" was already consumed
    // Boolean arg is whether "this" should be allowed as a name of the first parameter
    private List<ParsedParam> parseParams(List<GenericDef> typeGenerics, List<GenericDef> methodGenerics, Loc loc, String name, boolean acceptThisAsFirstParameter) throws CompilationException {
        if (lexer.consume(RIGHT_PAREN))
            return List.of();
        ArrayList<ParsedParam> params = new ArrayList<>();

        //Parse one at least, then continue parsing while we have commas
        String paramName;
        if (acceptThisAsFirstParameter && lexer.consume(THIS))
            paramName = "this";
        else
            paramName = lexer.expect(IDENTIFIER, "Expected ) to end params list for " + name, loc).string();
        Loc colonLoc = lexer.expect(COLON, "Parameter \"" + paramName + "\" in " + name + " is missing a annotatedType annotation", loc).loc();
        params.add(new ParsedParam(paramName, parseType(":", colonLoc, typeGenerics, methodGenerics)));
        while (lexer.consume(COMMA)) {
            if (lexer.check(RIGHT_PAREN))
                break;
            paramName = lexer.expect(IDENTIFIER, "Expected ) to end params list for " + name, loc).string();
            colonLoc = lexer.expect(COLON, "Parameter \"" + paramName + "\" in " + name + " is missing a annotatedType annotation", loc).loc();
            params.add(new ParsedParam(paramName, parseType(":", colonLoc, typeGenerics, methodGenerics)));
        }
        //Expect a closing paren
        lexer.expect(RIGHT_PAREN, "Expected ) to end params list for " + name, loc);
        //Return
        return params;
    }

    private record ParsedParam(String name, ParsedType type) {}

    //"var" was already consumed
    private SnuggleParsedFieldDef parseField(boolean isClass, boolean pub, boolean isStatic, List<GenericDef> typeGenerics, List<GenericDef> methodGenerics) throws CompilationException {
        Loc varLoc = lexer.last().loc();
        Token fieldName = lexer.expect(IDENTIFIER, "Expected field name after \"var\", but got " + lexer.peek().type());
        Loc colonLoc = lexer.expect(COLON, "Expected type annotation for field " + fieldName.string(), fieldName.loc()).loc();
        ParsedType annotatedType = parseType(":", colonLoc, typeGenerics, List.of());
        ParsedExpr initializer = null;
        if (lexer.consume(ASSIGN))
            if (isClass)
                initializer = parseExpr(typeGenerics, methodGenerics, false, true);
            else
                throw new ParsingException("Unexpected \"=\" : Struct fields cannot cannot have initializers!", lexer.last().loc());
        return new SnuggleParsedFieldDef(varLoc.merge(lexer.last().loc()), pub, isStatic, fieldName.string(), annotatedType, initializer);
    }

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
    private ParsedExpr parseExpr(List<GenericDef> typeGenerics, List<GenericDef> methodGenerics, boolean canBeDeclaration, boolean isNested) throws CompilationException {
        if (lexer.consume(SEMICOLON))
            return new ParsedTuple(lexer.last().loc(), List.of());
        return parseBinary(0, typeGenerics, methodGenerics, canBeDeclaration, isNested);
    }

    private ParsedExpr parseBinary(int precedence, List<GenericDef> typeGenerics, List<GenericDef> methodGenerics, boolean canBeDeclaration, boolean isNested) throws CompilationException {
        if (precedence >= TOK_TYPES.size())
            return parseAs(typeGenerics, methodGenerics, canBeDeclaration, isNested);
        ParsedExpr lhs = parseBinary(precedence + 1, typeGenerics, methodGenerics, canBeDeclaration, isNested);
        int rhsPrecedence = RIGHT_ASSOCIATIVE.get(precedence) ? precedence : precedence + 1;
        while (lexer.consume(TOK_TYPES.get(precedence))) {
            TokenType op = lexer.last().type();
            Loc opLoc = lexer.last().loc();

            String methodName = switch (op) {
                case PLUS -> "add";
                case MINUS -> "sub";
                case STAR -> "mul";
                case SLASH -> "div";
                case PERCENT -> "rem";
                case POWER -> "pow";

                case AMPERSAND -> "band";
                case PIPE -> "bor";
                case CARAT -> "bxor";

                //Special
                case AND, OR -> "SPECIAL_IGNORE_SHOULD_NEVER_SEE";

                case EQUAL, NOT_EQUAL -> "eq";
                case GREATER -> {
                    if (lexer.consume(GREATER)) //Bit shift
                        yield "shr";
                    else
                        yield "gt";
                }
                case LESS -> {
                    if (lexer.consume(LESS)) //Bit shift
                        yield "shl";
                    else
                        yield "lt";
                }
                case GREATER_EQUAL -> "ge";
                case LESS_EQUAL -> "le";
                default -> throw new IllegalStateException("parseBinary found invalid token \"" + op.exactStrings[0] + "\". Bug in compiler, please report!");
            };

            ParsedExpr rhs = parseBinary(rhsPrecedence, typeGenerics, methodGenerics, false, true);
            Loc fullLoc = Loc.merge(lhs.loc(), rhs.loc());

            //Special handling for and/or operations. They can't be method calls
            //because of short-circuiting.
            if (op == AND || op == OR) {
                lhs = new ParsedLogicalBinOp(fullLoc, op == AND,
                        wrapTruthy(lhs),
                        wrapTruthy(rhs)
                );
                continue;
            }

            //Otherwise, operators are method calls
            lhs = new ParsedMethodCall(opLoc, lhs, methodName, List.of(), List.of(rhs));

            //Special !=
            if (op == NOT_EQUAL)
                lhs = new ParsedMethodCall(opLoc, lhs, "not", List.of(), List.of());
        }

        return lhs;
    }

    private ParsedExpr parseAs(List<GenericDef> typeGenerics, List<GenericDef> methodGenerics, boolean canBeDeclaration, boolean isNested) throws CompilationException {
        ParsedExpr lhs = parseUnary(typeGenerics, methodGenerics, canBeDeclaration, isNested);
        while (lexer.consume(AS)) {
            //Since this is a while loop, you can technically do "1 as i32 as u32 as f32".
            //idk why you would, but you can, lol
            Token as = lexer.last();
            boolean isMaybe = lexer.consume(QUESTION_MARK);
            ParsedType type = parseType(isMaybe ? "as?" : "as", isMaybe ? as.loc().merge(lexer.last().loc()) : as.loc(), typeGenerics, methodGenerics);
            lhs = new ParsedCast(lhs.loc().merge(lexer.last().loc()), as.loc().startLine(), lhs, isMaybe, type);
        }
        return lhs;
    }

    private ParsedExpr parseUnary(List<GenericDef> typeGenerics, List<GenericDef> methodGenerics, boolean canBeDeclaration, boolean isNested) throws CompilationException {
        if (lexer.consume(MINUS, NOT, BITWISE_NOT, HASHTAG)) {
            String methodName = switch (lexer.last().type()) {
                case MINUS -> "neg";
                case NOT -> "not";
                case BITWISE_NOT -> "bnot";
                case HASHTAG -> "size";
                default -> throw new IllegalStateException("parseUnary found invalid token \"" + lexer.last().type() + "\". Bug in compiler, please report!");
            };
            Loc operatorLoc = lexer.last().loc();
            ParsedExpr operand = parseUnary(typeGenerics, methodGenerics, canBeDeclaration, true);
            return new ParsedMethodCall(operatorLoc, operand, methodName, List.of(), List.of());
        }

        return parseCallOrFieldOrAssignment(typeGenerics, methodGenerics, canBeDeclaration, isNested);
    }

    private ParsedExpr parseCallOrFieldOrAssignment(List<GenericDef> typeGenerics, List<GenericDef> methodGenerics, boolean canBeDeclaration, boolean isNested) throws CompilationException {
        //Parse lhs first
        ParsedExpr lhs = parseCallOrField(typeGenerics, methodGenerics, canBeDeclaration, isNested);

        //Once it's done, check for an assignment (or augmented assignment)
        if (lexer.consume(ASSIGN)) {
            Loc eqLoc = lexer.last().loc();
            //Regular assignment, = operator.
            //Parse the rhs:
            ParsedExpr rhs = parseExpr(typeGenerics, methodGenerics, canBeDeclaration, isNested);
            //Assert the lhs to be a Variable, FieldAccess, or MethodCall("get")
            if (lhs instanceof ParsedVariable || lhs instanceof ParsedFieldAccess) {
                return new ParsedAssignment(eqLoc, lhs, rhs);
            } else if (lhs instanceof ParsedMethodCall parsedCall && parsedCall.methodName().equals("get")) {
                //If lhs was a "get", transform to a "set"
                //a[0] = 1
                //a.get(0) = 1
                //a.set(0, 1)
                return new ParsedMethodCall(
                        eqLoc,
                        parsedCall.receiver(),
                        "set",
                        parsedCall.genericArgs(), //Usually empty
                        ListUtils.join(parsedCall.args(), List.of(rhs))
                );
            }
            //If it wasn't any case, error
            throw new ParsingException("Invalid assignment (=). Can only assign to variables, fields, and [] results.", eqLoc);
        } else if (lexer.consumeBetween(PLUS_ASSIGN, RIGHT_SHIFT_ASSIGN)) {
            //Don't count ||= or &&= in this case, since they use special short circuit handling
            TokenType op = lexer.last().type(); //Get the operator
            Loc opLoc = lexer.last().loc(); //And its loc
            //Choose method names via switch statement
            String fallback = switch (op) {
                case PLUS_ASSIGN -> "add";
                case MINUS_ASSIGN -> "sub";
                case TIMES_ASSIGN -> "mul";
                case DIVIDE_ASSIGN -> "div";
                case MODULO_ASSIGN -> "rem";
                case POWER_ASSIGN -> "pow";

                case BITWISE_AND_ASSIGN -> "band";
                case BITWISE_OR_ASSIGN -> "bor";
                case BITWISE_XOR_ASSIGN -> "bxor";

                case LEFT_SHIFT_ASSIGN -> "shl";
                case RIGHT_SHIFT_ASSIGN -> "shr";
                default -> throw new IllegalStateException("Parsing augmented assignment found invalid token \"" + op.exactStrings[0] + "\". Bug in compiler, please report!");
            };
            String methodName = fallback + "Assign"; //Assignment versions are just the same as regular, but with "Assign" appended

            //Parse the rhs:
            ParsedExpr rhs = parseCallOrFieldOrAssignment(typeGenerics, methodGenerics, canBeDeclaration, isNested);
            //Assert and return
            if (lhs instanceof ParsedVariable || lhs instanceof ParsedSuper || lhs instanceof ParsedFieldAccess || lhs instanceof ParsedMethodCall parsedCall && parsedCall.methodName().equals("get")) {
                return new ParsedAugmentedAssignment(opLoc, methodName, fallback, lhs, rhs);
            }
            //If it wasn't any case, error
            throw new ParsingException("Invalid assignment (" + op.exactStrings[0] + "). Can only assign to variables, fields, and [] results.", opLoc);
        } else if (lexer.consumeBetween(AND_ASSIGN, OR_ASSIGN)) {
            //Special case short circuiting &&=, ||=
            throw new ParsingException("||= and &&= operators not yet implemented", lexer.last().loc());
        } else {
            return lhs;
        }
    }

    private ParsedExpr parseCallOrField(List<GenericDef> typeGenerics, List<GenericDef> methodGenerics, boolean canBeDeclaration, boolean isNested) throws CompilationException {
        //If we find STAR, then output is automatically recurse().get()
        //no, no, bad, do not, the syntax is not enjoyable
        if (lexer.consume(STAR))
            return new ParsedMethodCall(lexer.last().loc(), parseCallOrField(typeGenerics, methodGenerics, canBeDeclaration, true), "get", List.of(), List.of());
        //Get the lhs
        ParsedExpr lhs = parseUnit(typeGenerics, methodGenerics, canBeDeclaration, isNested);
        //Any of these equal-precedence "call" operations can happen left-to-right
        while (lexer.consume(LEFT_PAREN, DOT, LEFT_SQUARE)) {
            TokenType op = lexer.last().type();
            Loc loc = lexer.last().loc();
            switch (op) {
                case DOT -> {
                    Token name = lexer.expect(IDENTIFIER, "Expected methodName after \".\" at " + loc);
                    List<ParsedType> typeArgs = parseTypeArguments(typeGenerics, methodGenerics);
                    if (lexer.consume(LEFT_PAREN)) {
                        List<ParsedExpr> args = parseArguments(RIGHT_PAREN, typeGenerics, methodGenerics);
                        lhs = new ParsedMethodCall(name.loc(), lhs, name.string(), typeArgs, args);
                    } else {
                        if (typeArgs.size() > 0)
                            throw new ParsingException("Field accesses cannot accept generic parameters: field \"" + name.string() + "\"", name.loc());
                        lhs = new ParsedFieldAccess(Loc.merge(lhs.loc(), name.loc()), lhs, name.string());
                    }
                }
                case LEFT_PAREN -> {
                    //super() is super.new(), while anythingElse() is anythingElse.invoke().
                    String methodName = (lhs instanceof ParsedSuper) ? "new" : "invoke";
                    List<ParsedExpr> args = parseArguments(RIGHT_PAREN, typeGenerics, methodGenerics);
                    Loc callLoc = (lhs instanceof ParsedSuper || lhs instanceof ParsedVariable) ? lhs.loc() : loc.merge(lexer.last().loc());
                    lhs = new ParsedMethodCall(callLoc, lhs, methodName, List.of(), args);
                }
                case LEFT_SQUARE -> {
                    List<ParsedExpr> args = parseArguments(RIGHT_SQUARE, typeGenerics, methodGenerics);
                    Loc indexLoc = lhs.loc().merge(lexer.last().loc());
                    lhs = new ParsedMethodCall(indexLoc, lhs, "get", List.of(), args);
                }
            }
        }
        return lhs;
    }


    //If there's a ::, then parses annotatedType arguments list
    //If there isn't, returns an empty list
    private List<ParsedType> parseTypeArguments(List<GenericDef> typeGenerics, List<GenericDef> methodGenerics) throws CompilationException {
        if (lexer.consume(DOUBLE_COLON)) {
            lexer.expect(LESS, "Expected < after :: to begin generic type arguments list", lexer.last().loc());
            if (lexer.consume(GREATER))
                return List.of(); //If it's immediately a >, then just return empty list. ::<>
            Token less = lexer.last();
            ArrayList<ParsedType> result = new ArrayList<>();
            result.add(parseType("<", less.loc(), typeGenerics, methodGenerics));
            while (lexer.consume(COMMA)) {
                if (lexer.check(GREATER))
                    break;
                result.add(parseType("<", less.loc(), typeGenerics, methodGenerics));
            }
            lexer.expect(GREATER, "Expected > to end generic type arguments list that began at " + less.loc());
            result.trimToSize();
            return result;
        } else {
            return List.of();
        }
    }

    //The paren was just parsed
    private List<ParsedExpr> parseArguments(TokenType endingType, List<GenericDef> typeGenerics, List<GenericDef> methodGenerics) throws CompilationException {
        if (lexer.consume(endingType))
            return List.of();
        Loc parenLoc = lexer.last().loc();
        ArrayList<ParsedExpr> arguments = new ArrayList<>();
        arguments.add(parseExpr(typeGenerics, methodGenerics, false, true)); //all arguments are nested in something
        while (lexer.consume(COMMA)) {
            if (lexer.check(endingType))
                break;
            arguments.add(parseExpr(typeGenerics, methodGenerics, false, true)); //^
        }
        lexer.expect(endingType, "Expected " + endingType.exactStrings[0] + " to end args list", parenLoc);
        arguments.trimToSize();
        return arguments;
    }

    //Parse a "unit" expression, the smallest and tightest-grouped expressions
    private ParsedExpr parseUnit(List<GenericDef> typeGenerics, List<GenericDef> methodGenerics, boolean canBeDeclaration, boolean isNested) throws CompilationException {
        return switch (lexer.take().type()) {
            //Eof
            case EOF -> throw new ParsingException("Unexpected end of file, expected expression", lexer.last().loc());

            //Things
            case    INT_LITERAL,
                    BOOL_LITERAL,
                    STRING_LITERAL,
                    FLOAT_LITERAL -> new ParsedLiteral(lexer.last().loc(), lexer.last().value());
            case NEW -> parseConstructor(typeGenerics, methodGenerics);
            case IDENTIFIER -> parseIdentOrTypeExpr(typeGenerics, methodGenerics);
            case THIS -> new ParsedVariable(lexer.last().loc(), "this");
            case SUPER -> new ParsedSuper(lexer.last().loc());
            case LEFT_PAREN -> parseParenExpr(typeGenerics, methodGenerics, canBeDeclaration);

            //Declarations
            case    CLASS,
                    STRUCT -> parseClassOrStruct(lexer.last().type() == CLASS, false, typeGenerics, methodGenerics, isNested);
            case ENUM -> parseEnum(false, typeGenerics, methodGenerics, isNested);
            case FN -> parseFn(false, typeGenerics, methodGenerics, isNested);
            case PUB -> {
                if (isNested)
                    throw new ParsingException("Types/functions nested in other expressions cannot be \"pub\"", lexer.last().loc());

                yield switch (lexer.take().type()) {
                    case    CLASS,
                            STRUCT -> parseClassOrStruct(lexer.last().type() == CLASS, true, typeGenerics, methodGenerics, false);
                    case ENUM -> parseEnum(true, typeGenerics, methodGenerics, false);
                    case FN -> parseFn(true, typeGenerics, methodGenerics, false);
                    default -> throw new ParsingException("Expected class, struct, enum, or fn after \"pub\"", lexer.last().loc());
                };
            }
            case VAR -> parseDeclaration(typeGenerics, methodGenerics, canBeDeclaration);
            case IMPORT -> parseImport();

            //Control flow
            case IF -> parseIf(typeGenerics, methodGenerics);
            case WHILE -> parseWhile(typeGenerics, methodGenerics);
            case FOR -> parseFor(typeGenerics, methodGenerics);
            case LEFT_CURLY -> parseBlock(typeGenerics, methodGenerics);
            case RETURN -> new ParsedReturn(lexer.last().loc(), parseExpr(typeGenerics, methodGenerics, false, true));

            //Other
            case IS -> { //is Type1 Type2
                Loc startLoc = lexer.last().loc();
                ParsedType type1 = parseType("is", startLoc, typeGenerics, methodGenerics);
                ParsedType type2 = parseType("is", startLoc, typeGenerics, methodGenerics);
                yield new ParsedIsSubtype(startLoc.merge(lexer.last().loc()), type1, type2);
            }

            default -> throw new ParsingException("Unexpected token " + lexer.last().type() + ". Expected expression", lexer.last().loc());
        };
    }

    //The "import" token was already consumed
    private ParsedImport parseImport() throws CompilationException {
        Loc loc = lexer.last().loc();
        Token str = lexer.expect(STRING_LITERAL, "Expected string literal after import");
        return new ParsedImport(loc, str.string());
    }

    //The identifier token was already consumed
    private ParsedExpr parseIdentOrTypeExpr(List<GenericDef> typeGenerics, List<GenericDef> methodGenerics) throws CompilationException {
        ParsedVariable v = new ParsedVariable(lexer.last().loc(), lexer.last().string());
        //Double colon means it's a type reference
        if (lexer.check(DOUBLE_COLON)) {
            List<ParsedType> typeArguments = parseTypeArguments(typeGenerics, methodGenerics);
            Loc fullLoc = v.loc().merge(lexer.last().loc());
            int index = genericIndexOf(methodGenerics, v.name());
            if (index != -1)
                if (typeArguments.size() == 0)
                    return new ParsedTypeExpr(fullLoc, new ParsedType.Generic(index, true));
                else throw new ParsingException("Cannot put type parameters on a generic (" + v.name() + ")", fullLoc);
            index = genericIndexOf(typeGenerics, v.name());
            if (index != -1)
                if (typeArguments.size() == 0)
                    return new ParsedTypeExpr(fullLoc, new ParsedType.Generic(index, false));
                else throw new ParsingException("Cannot put type parameters on a generic (" + v.name() + ")", fullLoc);
            return new ParsedTypeExpr(fullLoc, new ParsedType.Basic(v.name(), typeArguments));
        } else {
            //Otherwise just return the variable, maybe converted to a lambda.
            return maybeMakeLambda(v, typeGenerics, methodGenerics);
        }
    }

    //The left paren was just parsed
    private ParsedExpr parseParenExpr(List<GenericDef> typeGenerics, List<GenericDef> methodGenerics, boolean canBeDeclaration) throws CompilationException {
        Loc parenLoc = lexer.last().loc();
        //If we immediately find a right paren, then it's a tuple of 0 values (aka unit)
        if (lexer.consume(RIGHT_PAREN))
            return maybeMakeLambda(new ParsedTuple(parenLoc.merge(lexer.last().loc()), List.of()), typeGenerics, methodGenerics);
        ParsedExpr expr = parseExpr(typeGenerics, methodGenerics, canBeDeclaration, true);
        boolean allVariables = (expr instanceof ParsedVariable); //used later, for lambdas
        //If we find a right paren immediately after an expr, then we can return a parsed paren expr
        if (lexer.consume(RIGHT_PAREN)) {
            if (allVariables)
                return maybeMakeLambda(expr, typeGenerics, methodGenerics);
            return new ParsedParenExpr(parenLoc.merge(lexer.last().loc()), expr);
        }
        //We didn't find a right paren, so let's keep going
        ArrayList<ParsedExpr> elems = new ArrayList<>();
        elems.add(expr);
        while (lexer.consume(COMMA)) {
            if (lexer.check(RIGHT_PAREN))
                break;
            expr = parseExpr(typeGenerics, methodGenerics, canBeDeclaration, true);
            elems.add(expr);
            allVariables &= (expr instanceof ParsedVariable);
        }
        lexer.expect(RIGHT_PAREN, "Expected ) to end tuple that began at " + parenLoc, parenLoc);

        elems.trimToSize();
        if (allVariables)
            return maybeMakeLambda(new ParsedTuple(parenLoc.merge(lexer.last().loc()), elems), typeGenerics, methodGenerics);
        return new ParsedTuple(parenLoc.merge(lexer.last().loc()), elems);
    }

    //Lhs is one of:
    //- a ParsedVariable
    //- a ParsedTuple containing only ParsedVariables
    private ParsedExpr maybeMakeLambda(ParsedExpr lhs, List<GenericDef> typeGenerics, List<GenericDef> methodGenerics) throws CompilationException {
        if (!lexer.consume(ARROW)) //No arrow, just return lhs
            return lhs;
        //ParsedVariable:
        if (lhs instanceof ParsedVariable v) {
            return new ParsedLambda(lexer.last().loc(), List.of(v.name()), parseExpr(typeGenerics, methodGenerics, false, true));
        } else if (lhs instanceof ParsedTuple tuple) {
            //Tuple:
            List<String> names = ListUtils.map(tuple.elements(), e -> {
                if (e instanceof ParsedVariable v)
                    return v.name();
                throw new IllegalStateException("Bug in parser - should never have tried to make lambda out of tuple containing non-variables? Please report!");
            });
            return new ParsedLambda(lexer.last().loc(), names, parseExpr(typeGenerics, methodGenerics, false, true));
        }
        throw new IllegalStateException("Tried to make lambda out of something other than variable or tuple? Bug in compiler, please report!");
    }

    private ParsedExpr parseIf(List<GenericDef> typeGenerics, List<GenericDef> methodGenerics) throws CompilationException {
        Loc ifLoc = lexer.last().loc();
        ParsedExpr cond = wrapTruthy(parseExpr(typeGenerics, methodGenerics, false, true)); //things inside an if-expression are always nested
        ParsedExpr ifTrue = parseExpr(typeGenerics, methodGenerics, false, true);
        ParsedExpr ifFalse = lexer.consume(ELSE) ? parseExpr(typeGenerics, methodGenerics, false, true) : null;

        Loc fullLoc = Loc.merge(ifLoc, (ifFalse == null ? ifTrue : ifFalse).loc());
        return new ParsedIf(fullLoc, cond, ifTrue, ifFalse);
    }

    private ParsedExpr parseWhile(List<GenericDef> typeGenerics, List<GenericDef> methodGenerics) throws CompilationException {
        Loc whileLoc = lexer.last().loc();
        ParsedExpr cond = wrapTruthy(parseExpr(typeGenerics, methodGenerics, false, true));
        ParsedExpr body = parseExpr(typeGenerics, methodGenerics, false, true);
        Loc fullLoc = whileLoc.merge(body.loc());
        return new ParsedWhile(fullLoc, cond, body);
    }

    /**
     * Syntax sugar!
     * for a: T in b body
     * //becomes:
     * {
     *     var temp$iter = b.iter()
     *     var temp$value: T? = new()
     *     while temp$value = temp$iter() {
     *         var a: T = *temp$value
     *         body
     *     }
     * }
     */
    private ParsedExpr parseFor(List<GenericDef> typeGenerics, List<GenericDef> methodGenerics) throws CompilationException {
        Loc forLoc = lexer.last().loc();
        String varName = lexer.expect(IDENTIFIER, "Expected identifier after \"for\"", forLoc).string();
        lexer.expect(COLON, "Expected type annotation on loop variable \"" + varName + "\"", forLoc);
        ParsedType varType = parseType(":", lexer.last().loc(), typeGenerics, methodGenerics);
        lexer.expect(IN, "Expected \"in\" after variable in \"for\" loop", forLoc);
        ParsedExpr iteratee = parseExpr(typeGenerics, methodGenerics, false, true);
        ParsedExpr body = parseExpr(typeGenerics, methodGenerics, false, true);
        //Now emit result
        String iterName = "temp$iter";
        String iterValue = "temp$value";
        //Overall block
        return new ParsedBlock(forLoc, List.of(
                //var temp$iter = b.iter()
                new ParsedDeclaration(forLoc, iterName, null,
                        new ParsedMethodCall(forLoc, iteratee, "iter", List.of(), List.of())
                ),
                //var temp$value: T? = new()
                new ParsedDeclaration(forLoc, iterValue,
                        new ParsedType.Basic("Option", List.of(varType)),
                        new ParsedConstructor(forLoc, null, List.of())
                ),
                //while loop
                new ParsedWhile(forLoc,
                        wrapTruthy(
                                new ParsedAssignment(forLoc,
                                        new ParsedVariable(forLoc, iterValue),
                                        new ParsedMethodCall(forLoc,
                                                new ParsedVariable(forLoc, iterName),
                                                "invoke",
                                                List.of(),
                                                List.of()
                                        )
                                )
                        ),
                        //Body of while, a block
                        new ParsedBlock(forLoc, List.of(
                                new ParsedDeclaration(forLoc, varName, varType,
                                        new ParsedMethodCall(forLoc,
                                                new ParsedVariable(forLoc, iterValue),
                                                "get",
                                                List.of(),
                                                List.of()
                                        )
                                ),
                                body
                        ))
                )
        ));
    }

    //"{" has already been parsed
    private ParsedExpr parseBlock(List<GenericDef> typeGenerics, List<GenericDef> methodGenerics) throws CompilationException {
        Loc loc = lexer.last().loc();
        //Empty block, return with List.of()
        if (lexer.consume(RIGHT_CURLY))
            return new ParsedBlock(Loc.merge(loc, lexer.last().loc()), List.of());

        //Non-empty block
        ArrayList<ParsedExpr> exprs = new ArrayList<>();
        while (!lexer.consume(RIGHT_CURLY)) {
            exprs.add(parseExpr(typeGenerics, methodGenerics, true, true));
            if (lexer.isDone())
                throw new ParsingException("Unmatched {", loc);
        }
        return new ParsedBlock(Loc.merge(loc, lexer.last().loc()), exprs);
    }

    // "var" was just consumed
    private ParsedExpr parseDeclaration(List<GenericDef> typeGenerics, List<GenericDef> methodGenerics, boolean canBeDeclaration) throws CompilationException {
        Loc varLoc = lexer.last().loc();
        String varName = lexer.expect(IDENTIFIER, "Expected methodName after \"var\" at " + varLoc).string();

        if (!canBeDeclaration)
            throw new ParsingException("Invalid declaration location for variable \"" + varName + "\"", varLoc);

        ParsedType annotatedType = lexer.consume(COLON) ? parseType(":", lexer.last().loc(), typeGenerics, methodGenerics) : null;
        lexer.expect(ASSIGN, "Expected '=' after variable \"" + varName + "\" at " + varLoc);
        ParsedExpr rhs = parseExpr(typeGenerics, methodGenerics, true, true);
        return new ParsedDeclaration(Loc.merge(varLoc, rhs.loc()), varName, annotatedType, rhs);
    }

    private ParsedExpr parseFn(boolean pub, List<GenericDef> typeGenerics, List<GenericDef> methodGenerics, boolean isNested) throws CompilationException {
        Loc fnLoc = lexer.last().loc();
        Token nameTok = lexer.expect(IDENTIFIER, "Expected function name after \"fn\"", fnLoc);
        List<GenericDef> newGenerics = parseGenerics();
        methodGenerics = ListUtils.join(methodGenerics, newGenerics); //Append method generics
        //Parse parameters. We want to accept "this" as a possible first parameter.
        lexer.expect(LEFT_PAREN, "Expected ( to begin arguments list for function \"" + nameTok.string() + "\"", fnLoc);
        List<ParsedParam> params = parseParams(typeGenerics, methodGenerics, fnLoc, nameTok.string(), true);
        ParsedType resultType = lexer.consume(COLON) ? parseType(":", lexer.last().loc(), typeGenerics, methodGenerics) : ParsedType.Tuple.UNIT;
        //Generate the method def
        SnuggleParsedMethodDef parsedMethodDef = new SnuggleParsedMethodDef(
                fnLoc, pub, true,
                //Name depends on whether this was an extension function or not
                params.size() > 0 && params.get(0).name.equals("this") ? nameTok.string() : "invoke",
                newGenerics.size(),
                ListUtils.map(params, ParsedParam::name),
                ListUtils.map(params, ParsedParam::type),
                resultType,
                parseExpr(typeGenerics, methodGenerics, false, true)
        );
        //Decide what to do with the method def, based on whether this is an extension method or not.
        if (params.size() > 0 && params.get(0).name.equals("this")) {
            //It's an extension method
            return new ParsedExtensionMethod(nameTok.loc(), parsedMethodDef);
        } else {
            //Not an extension method
            return new ParsedTypeDefExpr(nameTok.loc(), new ParsedClassDef(
                    nameTok.loc(), pub, nameTok.string(), 0, isNested, null, List.of(parsedMethodDef), List.of()
            ));
        }
    }

    //Token "new" was just parsed
    private ParsedExpr parseConstructor(List<GenericDef> typeGenerics, List<GenericDef> methodGenerics) throws CompilationException {
        Loc newLoc = lexer.last().loc();

        //First check for "new()" or "new {}", where we will later try to infer the type from context
        if (lexer.consume(LEFT_PAREN)) {
            List<ParsedExpr> args = parseArguments(RIGHT_PAREN, typeGenerics, methodGenerics);
            return new ParsedConstructor(newLoc, null, args);
        } else if (lexer.consume(LEFT_CURLY)) {
            return parseStructConstructor(newLoc, null, typeGenerics, methodGenerics);
        }

        //Wasn't those, so it must be explicitly typed
        ParsedType constructedType = parseType("new", lexer.last().loc(), typeGenerics, methodGenerics);

        //Branch here - either a calling constructor new Type(), or a struct constructor new Type { }
        if (lexer.consume(LEFT_CURLY)) {
            return parseStructConstructor(newLoc, constructedType, typeGenerics, methodGenerics);
        } else {
            lexer.expect(LEFT_PAREN, "Expected ( or { to start constructor of annotatedType " + constructedType, newLoc);
            List<ParsedExpr> args = parseArguments(RIGHT_PAREN, typeGenerics, methodGenerics);
            return new ParsedConstructor(newLoc, constructedType, args);
        }
    }

    //Left curly { was already parsed
    private ParsedStructConstructor parseStructConstructor(Loc newLoc, ParsedType constructedType, List<GenericDef> typeGenerics, List<GenericDef> methodGenerics) throws CompilationException {
        List<ParsedExpr> args = parseArguments(RIGHT_CURLY, typeGenerics, methodGenerics);
        if (args.stream().allMatch(a -> a instanceof ParsedAssignment parsedAssignment && parsedAssignment.lhs() instanceof ParsedVariable)) {
            //Using the named format
            List<String> argNames = new ArrayList<>(args.size());
            for (int i = 0; i < args.size(); i++) {
                argNames.add(((ParsedVariable) ((ParsedAssignment) args.get(i)).lhs()).name());
                args.set(i, ((ParsedAssignment) args.get(i)).rhs());
            }
            return new ParsedStructConstructor(newLoc, constructedType, argNames, args);
        } else {
            //Unnamed format
            return new ParsedStructConstructor(newLoc, constructedType, null, args);
        }
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
        register(false, PLUS, MINUS, PIPE, CARAT); //a + b + c == (a + b) + c
        register(false, STAR, SLASH, PERCENT, AMPERSAND);
        register(true, POWER); //a ** b ** c == a ** (b ** c)
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

    private ParsedType parseType(String startTokString, Loc startTokLoc, List<GenericDef> typeGenerics, List<GenericDef> methodGenerics) throws CompilationException {
        //bad bad bad
        //go away
//        if (lexer.consume(AMPERSAND))
//            return new ParsedType.Basic("Box", List.of(parseType(startTokString, startTokLoc, typeGenerics, methodGenerics)));

        if (lexer.consume(LEFT_PAREN)) {
            //Tuple time
            //If it ends right away, then just return unit
            if (lexer.consume(RIGHT_PAREN))
                return addTypeModifiers(startTokLoc, ParsedType.Tuple.UNIT, typeGenerics, methodGenerics);
            startTokString = "(";
            startTokLoc = lexer.last().loc();

            //Parse elements of the tuple
            ArrayList<ParsedType> elements = new ArrayList<>();
            elements.add(parseType(startTokString, startTokLoc, typeGenerics, methodGenerics));
            while (lexer.consume(COMMA)) {
                if (lexer.check(RIGHT_PAREN))
                    break;
                elements.add(parseType(startTokString, startTokLoc, typeGenerics, methodGenerics));
            }
            elements.trimToSize();
            lexer.expect(RIGHT_PAREN, "Expected ) to end tuple type", startTokLoc);

            //Return the tuple
            return addTypeModifiers(startTokLoc, new ParsedType.Tuple(elements), typeGenerics, methodGenerics);
        } else {
            //Not a tuple
            String head = lexer.expect(IDENTIFIER, "Expected type after " + startTokString, startTokLoc).string();

            //If the annotatedType is a generic, return a generic TypeString
            //Search method generics first, as they're more recently defined,
            //and can shadow class generics
            int index = genericIndexOf(methodGenerics, head);
            if (index != -1) {
                return addTypeModifiers(startTokLoc, new ParsedType.Generic(index, true), typeGenerics, methodGenerics);
            }
            index = genericIndexOf(typeGenerics, head);
            if (index != -1) {
                return addTypeModifiers(startTokLoc, new ParsedType.Generic(index, false), typeGenerics, methodGenerics);
            }

            //Otherwise, return a Basic annotatedType
            if (lexer.consume(LESS)) {
                Loc lessLoc = lexer.last().loc();
                ArrayList<ParsedType> generics = new ArrayList<>();
                generics.add(parseType(startTokString, startTokLoc, typeGenerics, methodGenerics));
                while (lexer.consume(COMMA)) {
                    if (lexer.check(GREATER))
                        break;
                    generics.add(parseType(startTokString, startTokLoc, typeGenerics, methodGenerics));
                }
                generics.trimToSize();
                lexer.expect(GREATER, "Expected > to end generics list", lessLoc);
                return addTypeModifiers(startTokLoc, new ParsedType.Basic(head, generics), typeGenerics, methodGenerics);
            } else {
                return addTypeModifiers(startTokLoc, new ParsedType.Basic(head, List.of()), typeGenerics, methodGenerics);
            }
        }
    }

    private ParsedType addTypeModifiers(Loc loc, ParsedType t, List<GenericDef> typeGenerics, List<GenericDef> methodGenerics) throws CompilationException {
        while (lexer.consume(QUESTION_MARK, LEFT_SQUARE)) {
            if (lexer.last().type() == QUESTION_MARK)
                t = new ParsedType.Basic("Option", List.of(t));
            else {
                //was a left square; expect a right square ] immediately after
                lexer.expect(RIGHT_SQUARE, "Expected ] after [ while parsing type", loc);
                t = new ParsedType.Basic("Array", List.of(t));
            }
        }
        if (lexer.consume(ARROW)) {
            Loc arrowLoc = lexer.last().loc();
            if (t instanceof ParsedType.Tuple parsedTuple) {
                return new ParsedType.Func(parsedTuple.elements(), parseType("ARROW", arrowLoc, typeGenerics, methodGenerics));
            } else {
                return new ParsedType.Func(List.of(t), parseType("ARROW", arrowLoc, typeGenerics, methodGenerics));
            }
        }
        return t;
    }

    private static ParsedMethodCall wrapTruthy(ParsedExpr expr) {
        return new ParsedMethodCall(expr.loc(), expr, "bool", List.of(), List.of()); //truthy
    }

    //Generic parsing stuff

    private record GenericDef(String name) {}

    private GenericDef parseGenericDef(Loc listLoc) throws CompilationException {
        String name = lexer.expect(IDENTIFIER, "Expected > to end generics list", listLoc).string();
        return new GenericDef(name);
    }

    //Allow indexOf() with a string on a generic def list
    private static int genericIndexOf(List<GenericDef> genericDefs, String name) {
        for (int i = genericDefs.size() - 1; i >= 0; i--)
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
                while (lexer.consume(COMMA)) {
                    if (lexer.check(GREATER))
                        break;
                    generics.add(parseGenericDef(loc));
                }
                lexer.expect(GREATER, "Expected > to end generics list", loc);
                generics.trimToSize();
                return generics;
            }
        }
        return List.of();
    }

}
