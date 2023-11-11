package ast.parsed.expr;

import ast.passes.TypeResolver;
import ast.type_resolved.ResolvedType;
import ast.type_resolved.expr.*;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import util.ListUtils;

import java.util.List;

//Ex. methodName is "addAssign", fallback is "add"
//if .addAssign() doesn't exist for "a = a.addAssign(b)", then
//move on to "a = a.add(b)"

//Lhs is one of:
// - A ParsedVariable
// - A ParsedFieldAccess
// - A ParsedSuper
// - A ParsedMethodCall where the name of the method was "get"
public record ParsedAugmentedAssignment(Loc loc, String methodName, String fallback, ParsedExpr lhs, ParsedExpr rhs) implements ParsedExpr {

    @Override
    public TypeResolvedExpr resolve(TypeResolver resolver) throws CompilationException {

        if (lhs instanceof ParsedSuper) {
            //super += rhs
            //Becomes super.addAssign(rhs). No reassignment to super.
            //Also, no fallback to add.
            return new TypeResolvedSuperMethodCall(loc, methodName, List.of(), List.of(rhs.resolve(resolver)));
        }
        if (lhs instanceof ParsedVariable var) {
            //SomeType += rhs
            //SomeType.addAssign(rhs). No reassignment to SomeClass.
            //Also, no fallback to add.
            ResolvedType maybeStaticReceiver = resolver.tryGetBasicType(var.name());
            if (maybeStaticReceiver != null) {
                //We have a static method call!
                return new TypeResolvedStaticMethodCall(
                        loc,
                        maybeStaticReceiver,
                        methodName,
                        List.of(),
                        List.of(rhs.resolve(resolver))
                );
            }
        }

        TypeResolvedExpr resolvedLhs = lhs.resolve(resolver);
        TypeResolvedExpr resolvedRhs = rhs.resolve(resolver);

        return new TypeResolvedMethodCall(loc, resolvedLhs, methodName, () -> {
            if (resolvedLhs instanceof TypeResolvedVariable v) {
                //If it's just a variable, there's no side effects, so emit a type resolved assignment
                return new TypeResolvedAssignment(loc, v, new TypeResolvedMethodCall(loc, v, fallback, null, List.of(), List.of(resolvedRhs)));
            }

            if (resolvedLhs instanceof TypeResolvedFieldAccess || resolvedLhs instanceof TypeResolvedStaticFieldAccess) {
                //The one time it needs to move forward
                return new TypeResolvedAugmentedFieldAssignment(loc, fallback, resolvedLhs, resolvedRhs);
            }

            //If LHS is any type of method call, with name "get", then replace it with a "set"
            //Temp variables are needed.
            //a[b] += c
            //becomes
            //{ var temp1 = a; var temp2 = b; temp1.set(temp2, temp1.get(temp2).addAssign(c)) }
            //In the case where a is super, or this is a static call, it's
            //{ var temp = b; a.set(temp, a.get(temp).addAssign(c)) }, because "a" cannot have side effects like that
            String tempReceiver = " $$ snuggle generated temp receiver :D $$ ";
            String tempIndexName = " $$ snuggle generated temp index :D $$ #";
            if (resolvedLhs instanceof TypeResolvedStaticMethodCall staticMethodCall && staticMethodCall.methodName().equals("get")) {
                return new TypeResolvedBlock(loc, ListUtils.join(
                        //Declare all the temp vars
                        ListUtils.mapIndexed(staticMethodCall.args(), (arg, i) ->
                                new TypeResolvedDeclaration(loc, tempIndexName + i, null, arg)
                        ),
                        //a.set(
                        List.of(new TypeResolvedStaticMethodCall(loc, staticMethodCall.type(), "set", staticMethodCall.genericArgs(), ListUtils.join(
                                //grab all temp vars
                                ListUtils.generate(staticMethodCall.args().size(), i ->
                                        new TypeResolvedVariable(loc, tempIndexName + i)
                                ),
                                //.add or addAssign
                                List.of(new TypeResolvedMethodCall(loc,
                                        //a.get(
                                        new TypeResolvedStaticMethodCall(loc, staticMethodCall.type(), "get", staticMethodCall.genericArgs(), ListUtils.generate(staticMethodCall.args().size(), i ->
                                                //grab all the temp vars
                                                new TypeResolvedVariable(loc, tempIndexName + i)
                                        )),
                                        fallback,
                                        null,
                                        List.of(),
                                        List.of(resolvedRhs)
                                ))
                        )))
                ));
            } else if (resolvedLhs instanceof TypeResolvedSuperMethodCall superMethodCall && superMethodCall.methodName().equals("get")) {
                return new TypeResolvedBlock(loc, ListUtils.join(
                        //Declare all the temp vars
                        ListUtils.mapIndexed(superMethodCall.args(), (arg, i) ->
                                new TypeResolvedDeclaration(loc, tempIndexName + i, null, arg)
                        ),
                        //a.set(
                        List.of(new TypeResolvedSuperMethodCall(loc, "set", superMethodCall.genericArgs(), ListUtils.join(
                                //grab all temp vars
                                ListUtils.generate(superMethodCall.args().size(), i ->
                                        new TypeResolvedVariable(loc, tempIndexName + i)
                                ),
                                //.add or addAssign
                                List.of(new TypeResolvedMethodCall(loc,
                                        //a.get(
                                        new TypeResolvedSuperMethodCall(loc, "get", superMethodCall.genericArgs(), ListUtils.generate(superMethodCall.args().size(), i ->
                                                //grab all the temp vars
                                                new TypeResolvedVariable(loc, tempIndexName + i)
                                        )),
                                        fallback,
                                        null,
                                        List.of(),
                                        List.of(resolvedRhs)
                                ))
                        )))
                ));
            } else if (resolvedLhs instanceof TypeResolvedMethodCall getMethodCall && getMethodCall.methodName().equals("get")) {
                return new TypeResolvedBlock(loc, ListUtils.join(
                        //temp receiver
                        List.of(new TypeResolvedDeclaration(loc, tempReceiver, null, getMethodCall.receiver())),
                        //declare the other temp vars
                        ListUtils.mapIndexed(getMethodCall.args(), (arg, i) ->
                                new TypeResolvedDeclaration(loc, tempIndexName + i, null, arg)
                        ),
                        //tempReceiver.set(
                        List.of(new TypeResolvedMethodCall(loc, new TypeResolvedVariable(loc, tempReceiver), "set", null, getMethodCall.genericArgs(), ListUtils.join(
                                //grab all temp vars
                                ListUtils.generate(getMethodCall.args().size(), i ->
                                        new TypeResolvedVariable(loc, tempIndexName + i)
                                ),
                                //.add or addAssign
                                List.of(new TypeResolvedMethodCall(loc,
                                        //tempReceiver.get()
                                        new TypeResolvedMethodCall(loc, new TypeResolvedVariable(loc, tempReceiver), "get", null, getMethodCall.genericArgs(), List.of(
                                                //grab all temp vars
                                                new TypeResolvedVariable(loc, tempIndexName)
                                        )),
                                        fallback,
                                        null,
                                        List.of(),
                                        List.of(resolvedRhs)
                                ))
                        )))
                ));
            } else {
                throw new IllegalStateException("Somehow augmented assignment didn't match any case? Bug in compiler, please report");
            }
        }, List.of(), List.of(resolvedRhs));


    }
}
