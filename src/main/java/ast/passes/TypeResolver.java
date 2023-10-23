package ast.passes;

import ast.parsed.def.type.BuiltinParsedTypeDef;
import ast.parsed.def.type.ParsedTypeDef;
import ast.parsed.expr.ParsedImport;
import ast.parsed.prog.ParsedAST;
import ast.parsed.prog.ParsedFile;
import ast.type_resolved.ResolvedType;
import ast.type_resolved.def.type.TypeResolvedTypeDef;
import ast.type_resolved.prog.TypeResolvedAST;
import ast.type_resolved.prog.TypeResolvedFile;
import builtin_types.BuiltinType;
import builtin_types.BuiltinTypes;
import builtin_types.reflect.ReflectedBuiltin;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.DuplicateNamesException;
import exceptions.compile_time.ImportException;
import lexing.Loc;
import util.ListUtils;
import util.MapStack;

import java.util.*;

/**
 * Bundles together objects needed to resolve types.
 * Holds a static method, resolve(), which is responsible for
 * passing a ParsedAST on to the next stage, the TypeResolvedAST.
 */
public class TypeResolver {

    //All types defined throughout the entire program, as a list, as well as the inverse of this mapping
    private final List<ParsedTypeDef> allTypeDefs = new ArrayList<>();
    private final Map<ParsedTypeDef, Integer> allTypeDefsInverse = new IdentityHashMap<>();

    //Storage for outputs of the constructor
    private final List<TypeResolvedTypeDef> finalTypeDefList;
    private final Map<String, TypeResolvedFile> resolvedCodeByFile;
    private final IdentityHashMap<BuiltinType, Integer> builtins;
    private final IdentityHashMap<Class<?>, ReflectedBuiltin> reflectedBuiltins;

    //The current mappings from string -> int, which updates over time as we
    //push scope, pop scope, and import.
    private final MapStack<String, Integer> currentMappings = new MapStack<>();

    //Types defined in the code, must be imported. Used to update currentMappings
    //when we come across an import expression.
    private final Map<String, Map<String, Integer>> snuggleTypeDefs;

    /**
     * The big method that does it all - converts a ParsedAST into a TypeResolvedAST.
     */
    public static TypeResolvedAST resolve(BuiltinTypes builtinTypes, ParsedAST ast) throws CompilationException {
        //Create an instance of TypeResolver, which does its work in the constructor.
        TypeResolver resolver = new TypeResolver(builtinTypes, ast);
        //Use said work to construct a TypeResolved AST.
        return new TypeResolvedAST(resolver.finalTypeDefList, resolver.resolvedCodeByFile, resolver.builtins, resolver.reflectedBuiltins);
    }

    //Use the static method above.
    private TypeResolver(BuiltinTypes builtinTypes, ParsedAST parsedAST) throws CompilationException {
        //Globally defined types
        builtins = new IdentityHashMap<>();
        reflectedBuiltins = new IdentityHashMap<>();
        for (BuiltinType builtin : builtinTypes.getAll()) {
            int mapping = register(new BuiltinParsedTypeDef(builtin));
            if (builtin.nameable()) {
                //Require that all nameable builtin types have distinct names
                if (currentMappings.putIfAbsent(builtin.name(), mapping) != null)
                    throw new IllegalStateException("Bug by environment implementor: multiple builtin types named \"" + builtin.name() + "\"?");
            }
            builtins.put(builtin, mapping);
            //If it's a reflected builtin, add it into the reflected map
            if (builtin instanceof ReflectedBuiltin reflected) {
                if (reflectedBuiltins.containsKey(reflected.reflectedClass))
                    throw new IllegalStateException("Multiple reflections on the same class? Likely bug by environment designer, please report if not!");
                reflectedBuiltins.put(reflected.reflectedClass, reflected);
            }
        }

        //Get the types defined by the program
        snuggleTypeDefs = new HashMap<>();
        for (ParsedFile f : parsedAST.files()) {
            if (snuggleTypeDefs.containsKey(f.name()))
                throw new DuplicateNamesException("Multiple files with same methodName \"" + f.name() + "\": error.", new Loc(f.name(), 0, 0, 0, 0));
            snuggleTypeDefs.put(f.name(), ListUtils.indexBy(ListUtils.map(f.typeDefs(), this::register), i -> allTypeDefs.get(i).name()));
        }

        //Resolve all the files/types
        TypeResolvedTypeDef[] result = new TypeResolvedTypeDef[allTypeDefs.size()];

        //Resolve the builtin types
        for (Integer builtinIndex : builtins.values()) {
            ParsedTypeDef typeDef = allTypeDefs.get(builtinIndex);
            result[allTypeDefsInverse.get(typeDef)] = typeDef.resolve(this);
        }

        //Also resolve the file code
        Map<String, TypeResolvedFile> codeByFile = new HashMap<>();

        for (ParsedFile f : parsedAST.files()) {
            //For each file:
            push();
            //Import itself, including non-pub members
            doImport(new ParsedImport(new Loc(f.name(), 0,0,0,0), f.name()), true); //Import itself
            for (ParsedImport parsedImport : f.imports()) //Import everything it imports, without non-pub members
                doImport(parsedImport, false);
            //For each annotatedType in the file:
            for (ParsedTypeDef typeDef : f.typeDefs()) {
                //Resolve the typedef and store
                result[allTypeDefsInverse.get(typeDef)] = typeDef.resolve(this);
            }
            //Also resolve the imports and code in the file:
            codeByFile.put(f.name(), new TypeResolvedFile(
                    f.name(),
                    ListUtils.map(f.imports(), i -> i.resolve(this)),
                    ListUtils.map(f.code(), e -> e.resolve(this))
            ));
            //Pop scope
            pop();
        }

        //Save the final results
        finalTypeDefList = List.of(result);
        resolvedCodeByFile = codeByFile;
    }

    //Adds this typeDef to the list and the inverse
    private Integer register(ParsedTypeDef typeDef) {
        allTypeDefsInverse.put(typeDef, allTypeDefs.size());
        allTypeDefs.add(typeDef);
        return allTypeDefs.size() - 1;
    }

    public void push() {
        currentMappings.push();
    }

    public void pop() {
        currentMappings.pop();
    }

    /**
     * Handle the given import statement, and update the current typedef mappings appropriately.
     * Throws an exception if the imported file does not exist.
     *
     * If getNonPub is true, then this will also import non-pub members.
     * This only happens when a file implicitly imports itself.
     */
    public void doImport(ParsedImport parsedImport, boolean getNonPub) throws CompilationException {
        Map<String, Integer> fileMapping = snuggleTypeDefs.get(parsedImport.fileName());
        if (fileMapping == null)
            throw new ImportException("File \"" + parsedImport.fileName() + "\" could not be found.", parsedImport.loc());
        for (Map.Entry<String, Integer> e : fileMapping.entrySet()) {
            ParsedTypeDef typeDef = allTypeDefs.get(e.getValue());
            //Only add the mapping if it's pub, or if we're getting non-pub members
            if (typeDef.pub() || getNonPub) {
                //Do not allow duplicate mappings
                if (currentMappings.get(e.getKey()) != null)
                    throw new ImportException("Type \"" + e.getKey() + "\" already exists in this scope", parsedImport.loc());
                currentMappings.put(e.getKey(), e.getValue());
            }
        }
    }

    public Integer lookup(String name) {
        return currentMappings.get(name);
    }

    //Return a ResolvedType for the type if it exists
    //otherwise, return null
    public ResolvedType tryGetBasicType(String typeName) {
        Integer resolvedName = currentMappings.get(typeName);
        return resolvedName == null ? null : new ResolvedType.Basic(resolvedName, List.of());
    }

    public ResolvedType resolveBasicBuiltin(BuiltinType builtinType) {
        return new ResolvedType.Basic(builtins.get(builtinType), List.of());
    }

}
