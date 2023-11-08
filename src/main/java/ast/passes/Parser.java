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
import runtime.Unit;
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
        //Find all the imports
        List<ParsedImport> imports = new ArrayList<>();
        while (lexer.consume(IMPORT))
            imports.add(parseImport());
        //Parse types
        List<ParsedTypeDef> parsedTypeDefs = new ArrayList<>();
        while (lexer.check(PUB, CLASS, STRUCT, ENUM)) {
            boolean pub = lexer.consume(PUB);
            if (lexer.consume(CLASS, STRUCT))
                parsedTypeDefs.add(parseClassOrStruct(lexer.last().type() == CLASS, pub));
            else if (lexer.consume(ENUM))
                parsedTypeDefs.add(parseEnum(pub));
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

    //The "class"/"struct" token was just consumed
    private ParsedTypeDef parseClassOrStruct(boolean isClass, boolean pub) throws CompilationException {
        //Get type name
        String typeTypeString = isClass ? "class" : "struct";
        Token typeName = lexer.expect(IDENTIFIER, "Expected name after \"" + typeTypeString + "\", but got " + lexer.peek().type(), lexer.last().loc());
        //Parse generics and their bounds
        List<GenericDef> typeGenerics = parseGenerics();
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
                    methods.add(parseMethod(isClass ? TypeType.CLASS : TypeType.STRUCT, true, typeName.string(), pubMember, typeGenerics));
                else if (lexer.consume(VAR))
                    fields.add(parseField(isClass, pubMember, true, typeGenerics));
                else if (lexer.consume(LEFT_CURLY))
                    methods.add(new SnuggleParsedMethodDef(lexer.last().loc(), false, true, "#init", 0, List.of(), List.of(), new ParsedType.Basic("unit", List.of()), parseBlock(typeGenerics, List.of())));
                else
                    throw new ParsingException("Expected \"fn\", \"var\", or initializer block after \"static\"", lexer.last().loc());
            }
            //Regular functions/fields (not static)
            else if (lexer.consume(FN))
                methods.add(parseMethod(isClass ? TypeType.CLASS : TypeType.STRUCT, false, typeName.string(), pubMember, typeGenerics));
            else if (lexer.consume(VAR))
                fields.add(parseField(isClass, pubMember, false, typeGenerics));
            else
                throw new ParsingException("Expected method or field definition for " + typeTypeString + " \"" + typeName.string() + "\", found " + lexer.peek().type(), lexer.peek().loc());
        }
        if (isClass)
            return new ParsedClassDef(typeName.loc(), pub, typeName.string(), typeGenerics.size(), (ParsedType.Basic) superType, methods, fields);
        else
            return new ParsedStructDef(typeName.loc(), pub, typeName.string(), typeGenerics.size(), methods, fields);
    }

    //"enum" was already consumed
    private ParsedTypeDef parseEnum(boolean pub) throws CompilationException {
        Loc loc = lexer.last().loc();
        //Get type name
        String typeName = lexer.expect(IDENTIFIER, "Expected name after \"enum\", but got " + lexer.peek().type(), lexer.last().loc()).string();
        //Get the enum properties
        List<ParsedEnumDef.ParsedEnumProperty> properties = parseEnumProperties(loc, typeName);
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
                        methods.add(parseMethod(TypeType.ENUM, true, typeName, true, List.of()));
                } else if (lexer.consume(FN)) {
                    methods.add(parseMethod(TypeType.ENUM, false, typeName, true, List.of()));
                } else {
                    throw new ParsingException("Expected function definition after \"pub\" inside enum \"" + typeName + "\"", loc);
                }
            } else if (lexer.consume(VAR))
                throw new ParsingException("Cannot add fields to enum definition", lexer.last().loc());
            else if (lexer.consume(STATIC)) {
                if (lexer.consume(VAR))
                    throw new ParsingException("Cannot add fields to enum definition", lexer.last().loc());
                else if (lexer.consume(FN))
                    methods.add(parseMethod(TypeType.ENUM, true, typeName, false, List.of()));
            }
            else if (lexer.consume(FN))
                methods.add(parseMethod(TypeType.ENUM, false, typeName, false, List.of()));
            else if (lexer.consume(IDENTIFIER)) {
                String name = lexer.last().string();
                Loc elemLoc = lexer.last().loc();

                //Parse the left paren (maybe), parse the arguments
                List<ParsedExpr> args;
                if (lexer.consume(LEFT_PAREN))
                    args = parseArguments(RIGHT_PAREN, List.of(), List.of());
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
        return new ParsedEnumDef(loc, pub, typeName, properties, elements, methods);
    }

    //
    private List<ParsedEnumDef.ParsedEnumProperty> parseEnumProperties(Loc loc, String enumName) throws CompilationException {
        //No parens -> no properties
        if (!lexer.consume(LEFT_PAREN))
            return List.of();
        //There was a paren, so let's start parsing
        ArrayList<ParsedEnumDef.ParsedEnumProperty> parsedProperties = new ArrayList<>();
        while (!lexer.consume(RIGHT_PAREN)) {
            boolean pub = lexer.consume(PUB);
            String name = lexer.expect(IDENTIFIER, "Expected name for enum property of \"" + enumName + "\", but got " + lexer.peek().type(), loc).string();
            Loc colonLoc = lexer.expect(COLON, "Enum property \"" + name + "\" in \"" + enumName + "\" is missing a annotatedType annotation").loc();
            ParsedType type = parseType(":", colonLoc, List.of(), List.of());
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
    private SnuggleParsedMethodDef parseMethod(TypeType typeType, boolean isStatic, String thisTypeName, boolean pub, List<GenericDef> typeGenerics) throws CompilationException {
        if (lexer.consume(NEW)) {
            if (isStatic)
                throw new ParsingException("Constructors cannot be static", lexer.last().loc());
            //Constructor mode. Similar, but slightly different.
            String methodName = "new";
            Loc methodLoc = lexer.last().loc();
            List<GenericDef> methodGenerics = parseGenerics();
            lexer.expect(LEFT_PAREN, "Expected ( to begin params list for method \"" + methodName + "\"", methodLoc);
            List<ParsedParam> params = parseParams(typeGenerics, methodGenerics, methodLoc, "method \"" + methodName + "\"");

            //TODO: Fix unit type
            //ParsedType returnType = ParsedType.Tuple.UNIT;
            AtomicInteger i = new AtomicInteger(); //cursed
            ParsedType returnType = switch (typeType) {
                case CLASS -> new ParsedType.Basic("unit", List.of()); //Class constructors return unit
                case STRUCT -> new ParsedType.Basic(thisTypeName, ListUtils.map(typeGenerics, g -> new ParsedType.Generic(i.getAndIncrement(), false))); //Struct constructors return the type itself
                case ENUM -> throw new ParsingException("Cannot create constructor for enum", methodLoc);
            };

            ParsedExpr body = parseExpr(typeGenerics, methodGenerics, false);
            List<String> paramNames = ListUtils.map(params, ParsedParam::name);
            List<ParsedType> paramTypes = ListUtils.map(params, ParsedParam::type);
            return new SnuggleParsedMethodDef(methodLoc, pub, false, methodName, methodGenerics.size(), paramNames, paramTypes, returnType, body);
        }
        Token methodName = lexer.expect(IDENTIFIER, "Expected method methodName after \"fn\", but got " + lexer.peek().type());
        List<GenericDef> methodGenerics = parseGenerics();
        lexer.expect(LEFT_PAREN, "Expected ( to begin params list for method \"" + methodName.string() + "\"", methodName.loc());
        List<ParsedParam> params = parseParams(typeGenerics, methodGenerics, methodName.loc(), "method \"" + methodName + "\"");

        //TODO: Fix unit type
        //ParsedType returnType = lexer.consume(COLON) ? parseType(lexer.last(), typeGenerics, methodGenerics) : null;
        ParsedType returnType = lexer.consume(COLON) ? parseType(":", lexer.last().loc(), typeGenerics, methodGenerics) : new ParsedType.Basic("unit", List.of());

        ParsedExpr body = parseExpr(typeGenerics, methodGenerics, false);
        List<String> paramNames = ListUtils.map(params, ParsedParam::name);
        List<ParsedType> paramTypes = ListUtils.map(params, ParsedParam::type);
        return new SnuggleParsedMethodDef(methodName.loc(), pub, isStatic, methodName.string(), methodGenerics.size(), paramNames, paramTypes, returnType, body);
    }

    // Parse a params list, not to be confused with an args list
    // The "(" was already consumed
    private List<ParsedParam> parseParams(List<GenericDef> typeGenerics, List<GenericDef> methodGenerics, Loc loc, String name) throws CompilationException {
        if (lexer.consume(RIGHT_PAREN))
            return List.of();
        ArrayList<ParsedParam> params = new ArrayList<>();

        //Parse one at least, then continue parsing while we have commas
        String paramName = lexer.expect(IDENTIFIER, "Expected ) to end params list for " + name, loc).string();
        Loc colonLoc = lexer.expect(COLON, "Parameter \"" + paramName + "\" in " + name + " is missing a annotatedType annotation", loc).loc();
        params.add(new ParsedParam(paramName, parseType(":", colonLoc, typeGenerics, methodGenerics)));
        while (lexer.consume(COMMA)) {
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
    private SnuggleParsedFieldDef parseField(boolean isClass, boolean pub, boolean isStatic, List<GenericDef> typeGenerics) throws CompilationException {
        Loc varLoc = lexer.last().loc();
        Token fieldName = lexer.expect(IDENTIFIER, "Expected field name after \"var\", but got " + lexer.peek().type());
        Loc colonLoc = lexer.expect(COLON, "Expected type annotation for field " + fieldName.string(), fieldName.loc()).loc();
        ParsedType annotatedType = parseType(":", colonLoc, typeGenerics, List.of());
        ParsedExpr initializer = null;
        if (lexer.consume(ASSIGN))
            if (isClass)
                initializer = parseExpr(typeGenerics, List.of(), false);
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
    private ParsedExpr parseExpr(List<GenericDef> classGenerics, List<GenericDef> methodGenerics, boolean canBeDeclaration) throws CompilationException {
        if (lexer.consume(SEMICOLON))
            return new ParsedLiteral(lexer.last().loc(), Unit.INSTANCE);
        return parseBinary(0, classGenerics, methodGenerics, canBeDeclaration);
    }

    private ParsedExpr parseBinary(int precedence, List<GenericDef> classGenerics, List<GenericDef> methodGenerics, boolean canBeDeclaration) throws CompilationException {
        if (precedence >= TOK_TYPES.size())
            return parseAs(classGenerics, methodGenerics, canBeDeclaration);
        ParsedExpr lhs = parseBinary(precedence + 1, classGenerics, methodGenerics, canBeDeclaration);
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

            ParsedExpr rhs = parseBinary(rhsPrecedence, classGenerics, methodGenerics, false);
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

    private ParsedExpr parseAs(List<GenericDef> classGenerics, List<GenericDef> methodGenerics, boolean canBeDeclaration) throws CompilationException {
        ParsedExpr lhs = parseUnary(classGenerics, methodGenerics, canBeDeclaration);
        while (lexer.consume(AS)) {
            //Since this is a while loop, you can technically do "1 as i32 as u32 as f32".
            //idk why you would, but you can, lol
            Token as = lexer.last();
            boolean isMaybe = lexer.consume(QUESTION_MARK);
            ParsedType type = parseType(isMaybe ? "as?" : "as", isMaybe ? as.loc().merge(lexer.last().loc()) : as.loc(), classGenerics, methodGenerics);
            lhs = new ParsedCast(lhs.loc().merge(lexer.last().loc()), as.loc().startLine(), lhs, isMaybe, type);
        }
        return lhs;
    }

    private ParsedExpr parseUnary(List<GenericDef> classGenerics, List<GenericDef> methodGenerics, boolean canBeDeclaration) throws CompilationException {
        if (lexer.consume(MINUS, NOT, BITWISE_NOT, HASHTAG)) {
            String methodName = switch (lexer.last().type()) {
                case MINUS -> "neg";
                case NOT -> "not";
                case BITWISE_NOT -> "bnot";
                case HASHTAG -> "size";
                default -> throw new IllegalStateException("parseUnary found invalid token \"" + lexer.last().type() + "\". Bug in compiler, please report!");
            };
            Loc operatorLoc = lexer.last().loc();
            ParsedExpr operand = parseUnary(classGenerics, methodGenerics, canBeDeclaration);
            return new ParsedMethodCall(operatorLoc, operand, methodName, List.of(), List.of());
        }

        return parseCallOrFieldOrAssignment(classGenerics, methodGenerics, canBeDeclaration);
    }

    private ParsedExpr parseCallOrFieldOrAssignment(List<GenericDef> classGenerics, List<GenericDef> methodGenerics, boolean canBeDeclaration) throws CompilationException {
        //Parse lhs first
        ParsedExpr lhs = parseCallOrField(classGenerics, methodGenerics, canBeDeclaration);

        //Once it's done, check for an assignment (or augmented assignment)
        if (lexer.consume(ASSIGN)) {
            Loc eqLoc = lexer.last().loc();
            //Regular assignment, = operator.
            //Parse the rhs:
            ParsedExpr rhs = parseExpr(classGenerics, methodGenerics, canBeDeclaration);
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
            ParsedExpr rhs = parseCallOrFieldOrAssignment(classGenerics, methodGenerics, canBeDeclaration);
            //Assert and return
            if (lhs instanceof ParsedVariable || lhs instanceof ParsedFieldAccess || lhs instanceof ParsedMethodCall parsedCall && parsedCall.methodName().equals("get")) {
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

    private ParsedExpr parseCallOrField(List<GenericDef> classGenerics, List<GenericDef> methodGenerics, boolean canBeDeclaration) throws CompilationException {
        //If we find STAR, then output is automatically recurse().get()
        //no, no, bad, do not, the syntax is not enjoyable
//        if (lexer.consume(STAR))
//            return new ParsedMethodCall(lexer.last().loc(), parseCallOrField(classGenerics, methodGenerics, canBeDeclaration), "get", List.of(), List.of());
        //Get the lhs
        ParsedExpr lhs = parseUnit(classGenerics, methodGenerics, canBeDeclaration);
        //Any of these equal-precedence "call" operations can happen left-to-right
        while (lexer.consume(LEFT_PAREN, DOT, LEFT_SQUARE)) {
            TokenType op = lexer.last().type();
            Loc loc = lexer.last().loc();
            switch (op) {
                case DOT -> {
                    Token name = lexer.expect(IDENTIFIER, "Expected methodName after \".\" at " + loc);
                    List<ParsedType> typeArgs = parseTypeArguments(classGenerics, methodGenerics);
                    if (lexer.consume(LEFT_PAREN)) {
                        List<ParsedExpr> args = parseArguments(RIGHT_PAREN, classGenerics, methodGenerics);
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
                    List<ParsedExpr> args = parseArguments(RIGHT_PAREN, classGenerics, methodGenerics);
                    Loc callLoc = (lhs instanceof ParsedSuper || lhs instanceof ParsedVariable) ? lhs.loc() : loc.merge(lexer.last().loc());
                    lhs = new ParsedMethodCall(callLoc, lhs, methodName, List.of(), args);
                }
                case LEFT_SQUARE -> {
                    List<ParsedExpr> args = parseArguments(RIGHT_SQUARE, classGenerics, methodGenerics);
                    Loc indexLoc = lhs.loc().merge(lexer.last().loc());
                    lhs = new ParsedMethodCall(indexLoc, lhs, "get", List.of(), args);
                }
            }
        }
        return lhs;
    }


    //If there's a ::, then parses annotatedType arguments list
    //If there isn't, returns an empty list
    private List<ParsedType> parseTypeArguments(List<GenericDef> classGenerics, List<GenericDef> methodGenerics) throws CompilationException {
        if (lexer.consume(DOUBLE_COLON)) {
            lexer.expect(LESS, "Expected < after :: to begin generic type arguments list", lexer.last().loc());
            if (lexer.consume(GREATER))
                return List.of(); //If it's immediately a >, then just return empty list. ::<>
            Token less = lexer.last();
            ArrayList<ParsedType> result = new ArrayList<>();
            result.add(parseType("<", less.loc(), classGenerics, methodGenerics));
            while (lexer.consume(COMMA))
                result.add(parseType("<", less.loc(), classGenerics, methodGenerics));
            lexer.expect(GREATER, "Expected > to end generic type arguments list that began at " + less.loc());
            result.trimToSize();
            return result;
        } else {
            return List.of();
        }
    }

    //The paren was just parsed
    private List<ParsedExpr> parseArguments(TokenType endingType, List<GenericDef> classGenerics, List<GenericDef> methodGenerics) throws CompilationException {
        if (lexer.consume(endingType))
            return List.of();
        Loc parenLoc = lexer.last().loc();
        ArrayList<ParsedExpr> arguments = new ArrayList<>();
        arguments.add(parseExpr(classGenerics, methodGenerics, false));
        while (lexer.consume(COMMA))
            arguments.add(parseExpr(classGenerics, methodGenerics, false));
        lexer.expect(endingType, "Expected " + endingType.exactStrings[0] + " to end args list", parenLoc);
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
                    BOOL_LITERAL,
                    STRING_LITERAL,
                    FLOAT_LITERAL -> new ParsedLiteral(lexer.last().loc(), lexer.last().value());
            case NEW -> parseConstructor(classGenerics, methodGenerics);

            //Other
            case IS -> { //is Type1 Type2
                Loc startLoc = lexer.last().loc();
                ParsedType type1 = parseType("is", startLoc, classGenerics, methodGenerics);
                ParsedType type2 = parseType("is", startLoc, classGenerics, methodGenerics);
                yield new ParsedIsSubtype(startLoc.merge(lexer.last().loc()), type1, type2);
            }

            //Identifiers
            case IDENTIFIER -> {
                ParsedVariable v = new ParsedVariable(lexer.last().loc(), lexer.last().string());
                //Double colon means it's a type reference
                if (lexer.check(DOUBLE_COLON)) {
                    List<ParsedType> typeArguments = parseTypeArguments(classGenerics, methodGenerics);
                    Loc fullLoc = v.loc().merge(lexer.last().loc());
                    int index = genericIndexOf(methodGenerics, v.name());
                    if (index != -1)
                        if (typeArguments.size() == 0)
                            yield new ParsedTypeExpr(fullLoc, new ParsedType.Generic(index, true));
                        else throw new ParsingException("Cannot put type parameters on a generic (" + v.name() + ")", fullLoc);
                    index = genericIndexOf(classGenerics, v.name());
                    if (index != -1)
                        if (typeArguments.size() == 0)
                            yield new ParsedTypeExpr(fullLoc, new ParsedType.Generic(index, false));
                        else throw new ParsingException("Cannot put type parameters on a generic (" + v.name() + ")", fullLoc);
                    yield new ParsedTypeExpr(fullLoc, new ParsedType.Basic(v.name(), typeArguments));
                } else {
                    //Otherwise just yield the variable
                    yield v;
                }
            }
            case THIS -> new ParsedVariable(lexer.last().loc(), "this");
            case SUPER -> new ParsedSuper(lexer.last().loc());

            //Control flow
            case IF -> parseIf(classGenerics, methodGenerics);
            case WHILE -> parseWhile(classGenerics, methodGenerics);
            case LEFT_CURLY -> parseBlock(classGenerics, methodGenerics);
            case RETURN -> new ParsedReturn(lexer.last().loc(), parseExpr(classGenerics, methodGenerics, false));

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
        ParsedExpr cond = wrapTruthy(parseExpr(classGenerics, methodGenerics, false));
        ParsedExpr ifTrue = parseExpr(classGenerics, methodGenerics, false);
        ParsedExpr ifFalse = lexer.consume(ELSE) ? parseExpr(classGenerics, methodGenerics, false) : null;

        Loc fullLoc = Loc.merge(ifLoc, (ifFalse == null ? ifTrue : ifFalse).loc());
        return new ParsedIf(fullLoc, cond, ifTrue, ifFalse);
    }

    private ParsedExpr parseWhile(List<GenericDef> classGenerics, List<GenericDef> methodGenerics) throws CompilationException {
        Loc whileLoc = lexer.last().loc();
        ParsedExpr cond = wrapTruthy(parseExpr(classGenerics, methodGenerics, false));
        ParsedExpr body = parseExpr(classGenerics, methodGenerics, false);
        Loc fullLoc = whileLoc.merge(body.loc());
        return new ParsedWhile(fullLoc, cond, body);
    }

    //"{" has already been parsed
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

        ParsedType annotatedType = lexer.consume(COLON) ? parseType(":", lexer.last().loc(), classGenerics, methodGenerics) : null;
        lexer.expect(ASSIGN, "Expected '=' after variable \"" + varName + "\" at " + varLoc);
        ParsedExpr rhs = parseExpr(classGenerics, methodGenerics, true);
        return new ParsedDeclaration(Loc.merge(varLoc, rhs.loc()), varName, annotatedType, rhs);
    }

    //Token "new" was just parsed
    private ParsedExpr parseConstructor(List<GenericDef> classGenerics, List<GenericDef> methodGenerics) throws CompilationException {
        Loc newLoc = lexer.last().loc();

        //First check for "new()" or "new {}", where we will later try to infer the type from context
        if (lexer.consume(LEFT_PAREN)) {
            List<ParsedExpr> args = parseArguments(RIGHT_PAREN, classGenerics, methodGenerics);
            return new ParsedConstructor(newLoc, null, args);
        } else if (lexer.consume(LEFT_CURLY)) {
            return parseStructConstructor(newLoc, null, classGenerics, methodGenerics);
        }

        //Wasn't those, so it must be explicitly typed
        ParsedType constructedType = parseType("new", lexer.last().loc(), classGenerics, methodGenerics);

        //Branch here - either a calling constructor new Type(), or a struct constructor new Type { }
        if (lexer.consume(LEFT_CURLY)) {
            return parseStructConstructor(newLoc, constructedType, classGenerics, methodGenerics);
        } else {
            lexer.expect(LEFT_PAREN, "Expected ( or { to start constructor of annotatedType " + constructedType, newLoc);
            List<ParsedExpr> args = parseArguments(RIGHT_PAREN, classGenerics, methodGenerics);
            return new ParsedConstructor(newLoc, constructedType, args);
        }
    }

    //Left curly { was already parsed
    private ParsedStructConstructor parseStructConstructor(Loc newLoc, ParsedType constructedType, List<GenericDef> classGenerics, List<GenericDef> methodGenerics) throws CompilationException {
        List<ParsedExpr> args = parseArguments(RIGHT_CURLY, classGenerics, methodGenerics);
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

        String head = lexer.expect(IDENTIFIER, "Expected type after " + startTokString, startTokLoc).string();

        //If the annotatedType is a generic, return a generic TypeString
        //Search method generics first, as they're more recently defined,
        //and can shadow class generics
        int index = genericIndexOf(methodGenerics, head);
        if (index != -1) {
            return addTypeModifiers(startTokLoc, new ParsedType.Generic(index, true));
        }
        index = genericIndexOf(typeGenerics, head);
        if (index != -1) {
            return addTypeModifiers(startTokLoc, new ParsedType.Generic(index, false));
        }

        //Otherwise, return a Basic annotatedType
        if (lexer.consume(LESS)) {
            Loc lessLoc = lexer.last().loc();
            ArrayList<ParsedType> generics = new ArrayList<>();
            generics.add(parseType(startTokString, startTokLoc, typeGenerics, methodGenerics));
            while (lexer.consume(COMMA))
                generics.add(parseType(startTokString, startTokLoc, typeGenerics, methodGenerics));
            generics.trimToSize();
            lexer.expect(GREATER, "Expected > to end generics list", lessLoc);
            return addTypeModifiers(startTokLoc, new ParsedType.Basic(head, generics));
        } else {
            return addTypeModifiers(startTokLoc, new ParsedType.Basic(head, List.of()));
        }
    }

    private ParsedType addTypeModifiers(Loc loc, ParsedType t) throws CompilationException {
        while (lexer.consume(QUESTION_MARK, LEFT_SQUARE)) {
            if (lexer.last().type() == QUESTION_MARK)
                t = new ParsedType.Basic("Option", List.of(t));
            else {
                //was a left square; expect a right square ] immediately after
                lexer.expect(RIGHT_SQUARE, "Expected ] after [ while parsing type", loc);
                t = new ParsedType.Basic("Array", List.of(t));
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
