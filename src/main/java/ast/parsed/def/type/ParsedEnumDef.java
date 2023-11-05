package ast.parsed.def.type;

import ast.parsed.ParsedType;
import ast.parsed.def.method.SnuggleParsedMethodDef;
import ast.parsed.expr.ParsedExpr;
import ast.passes.TypeResolver;
import ast.type_resolved.def.type.TypeResolvedEnumDef;
import ast.type_resolved.def.type.TypeResolvedTypeDef;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import util.ListUtils;

import java.util.List;

/*

pub enum Day(pub isWeekend: bool) {
    SUNDAY(true)
    MONDAY(false)
    TUESDAY(false)
    WEDNESDAY(false)
    THURSDAY(false)
    FRIDAY(false)
    SATURDAY(true)

    //Get the day of the week on a given date
    static fn dayOf(date: Date): Day {...}

}

var x = Day.dayOf(Date.now())
if x.isWeekend System.print("yippee!!!!")

 */

public record ParsedEnumDef(Loc loc, boolean pub, String name, List<ParsedEnumProperty> properties, List<ParsedEnumElement> elements, List<SnuggleParsedMethodDef> methods) implements ParsedTypeDef {

    @Override
    public TypeResolvedTypeDef resolve(TypeResolver resolver) throws CompilationException {
        return new TypeResolvedEnumDef(
                loc,
                name,
                ListUtils.map(properties, p -> new TypeResolvedEnumDef.TypeResolvedEnumProperty(p.name, p.type.resolve(loc, resolver))),
                ListUtils.map(elements, e -> new TypeResolvedEnumDef.TypeResolvedEnumElement(e.name, ListUtils.map(e.args, arg -> arg.resolve(resolver)))),
                ListUtils.map(methods, m -> m.resolve(resolver))
        );
    }

    public record ParsedEnumProperty(boolean pub, String name, ParsedType type) {}
    public record ParsedEnumElement(String name, List<ParsedExpr> args) {}

}

