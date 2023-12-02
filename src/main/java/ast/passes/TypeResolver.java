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
import lexing.Lexer;
import lexing.Loc;
import util.ListUtils;
import util.MapStack;

import java.util.*;

/**
 * Bundles together objects needed to resolve topLevelTypes.
 * Holds a static method, resolve(), which is responsible for
 * passing a ParsedAST on to the next stage, the TypeResolvedAST.
 */
public class TypeResolver {

    //All topLevelTypes defined throughout the entire program, as a list, as well as the inverse of this mapping
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
    private final Map<String, Map<String, Integer>> snuggleTopLevelTypeDefs;

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

        Set<String> fileNames = new HashSet<>(ListUtils.map(parsedAST.files(), ParsedFile::name));

        //Insert the snuggle-defined files into the ParsedAST
        for (Map.Entry<String, String> snuggleFile : builtinTypes.getSnuggleFiles().entrySet()) {
            String name = snuggleFile.getKey();
            //Add it to the AST, if there isn't already a file with that name
            if (!fileNames.contains(name)) {
                String source = snuggleFile.getValue();
                ParsedFile parsed = Parser.parseFile(name, new Lexer(name, source));
                parsedAST.files().add(parsed);
            }
        }

        //Globally defined builtin topLevelTypes
        builtins = new IdentityHashMap<>();
        reflectedBuiltins = new IdentityHashMap<>();
        for (BuiltinType builtin : builtinTypes.getBuiltins()) {
            int mapping = register(new BuiltinParsedTypeDef(builtin));
            if (builtin.nameable()) {
                //Require that all nameable builtin topLevelTypes have distinct names
                if (currentMappings.putIfAbsent(builtin.name(), mapping) != null)
                    throw new IllegalStateException("Bug by environment implementor: multiple builtin topLevelTypes named \"" + builtin.name() + "\"?");
            }
            builtins.put(builtin, mapping);
            //If it's a reflected builtin, add it into the reflected map
            if (builtin instanceof ReflectedBuiltin reflected) {
                if (reflectedBuiltins.containsKey(reflected.getJavaClass()))
                    throw new IllegalStateException("Multiple reflections on the same class? Likely bug by environment designer, please report if not!");
                reflectedBuiltins.put(reflected.getJavaClass(), reflected);
            }
        }

        //Get the topLevelTypes defined by the program
        snuggleTopLevelTypeDefs = new HashMap<>();
        for (ParsedFile f : parsedAST.files()) {
            if (snuggleTopLevelTypeDefs.containsKey(f.name()))
                throw new DuplicateNamesException("Multiple files with same name \"" + f.name() + "\": error.", new Loc(f.name(), 0, 0, 0, 0));
            snuggleTopLevelTypeDefs.put(f.name(), ListUtils.indexBy(ListUtils.map(f.topLevelTypeDefs(), this::register), i -> allTypeDefs.get(i).name()));
        }

        //Resolve all the files/topLevelTypes
        finalTypeDefList = new ArrayList<>();

        //Resolve the builtin topLevelTypes
        for (Integer builtinIndex : builtins.values()) {
            ParsedTypeDef typeDef = allTypeDefs.get(builtinIndex);
            ListUtils.setExpand(finalTypeDefList, allTypeDefsInverse.get(typeDef), typeDef.resolve(this));
        }

        //Also resolve the file code
        Map<String, TypeResolvedFile> codeByFile = new HashMap<>();

        for (ParsedFile f : parsedAST.files()) {
            //For each file:
            push();
            //Import itself, including non-pub members
            doImport(new ParsedImport(new Loc(f.name(), 0,0,0,0), f.name()), true);
            //Import all the auto-imports of the BuiltinTypes
            for (String autoImport : builtinTypes.getAutoImports())
                doImport(new ParsedImport(new Loc(f.name(), 0,0,0,0), autoImport), false);
            //Import all of its top-level imports
            for (ParsedImport parsedImport : f.topLevelImports())
                doImport(parsedImport, false);

            //For each top level type def in the file:
            for (ParsedTypeDef typeDef : f.topLevelTypeDefs()) {
                //Resolve the typedef and store
                ListUtils.setExpand(finalTypeDefList, allTypeDefsInverse.get(typeDef), typeDef.resolve(this));
            }
            //Also resolve the imports and code in the file:
            codeByFile.put(f.name(), new TypeResolvedFile(
                    f.name(),
//                    ListUtils.map(f.imports(), i -> new TypeResolvedImport(i.loc(), i.fileName())),
                    ListUtils.map(f.topLevelTypeDefs(), t -> new ResolvedType.Basic(allTypeDefsInverse.get(t), List.of())),
                    ListUtils.map(f.topLevelExtensionMethods(), m -> m.resolve(this)),
                    ListUtils.map(f.code(), e -> e.resolve(this))
            ));
            //Pop scope
            pop();
        }

        //Save the final results
        for (var x : finalTypeDefList)
            if (x == null)
                throw new IllegalStateException("Resolved type defs not fully initialized? Bug in compiler, please report");
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
        Map<String, Integer> fileMapping = snuggleTopLevelTypeDefs.get(parsedImport.fileName());
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

    //Add a typedef to the current mappings :3
    //and return the mapping of the type
    //Should only be run for nested types!
    public int addType(Loc loc, ParsedTypeDef typeDef) throws CompilationException {
        if (!typeDef.nested())
            throw new IllegalStateException("Attempt to call addType() with a non-nested type? Bug in compiler, please report!");
        int mapping = register(typeDef);
        ListUtils.setExpand(finalTypeDefList, mapping, typeDef.resolve(this));
        if (currentMappings.get(typeDef.name()) != null)
            throw new ImportException("Type \"" + typeDef.name() + "\" already exists in this scope", loc);
        currentMappings.put(typeDef.name(), mapping);
        return mapping;
    }

    public Integer lookup(String name) {
        return currentMappings.get(name);
    }

    public Integer lookup(ParsedTypeDef typeDef) {
        return allTypeDefsInverse.get(typeDef);
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
