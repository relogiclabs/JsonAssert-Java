package com.relogiclabs.jschema.internal.engine;

import com.relogiclabs.jschema.internal.antlr.SchemaParser;
import com.relogiclabs.jschema.internal.script.GDouble;
import com.relogiclabs.jschema.internal.script.GInteger;
import com.relogiclabs.jschema.internal.script.GLeftValue;
import com.relogiclabs.jschema.internal.script.GObject;
import com.relogiclabs.jschema.internal.script.GParameter;
import com.relogiclabs.jschema.internal.script.GRange;
import com.relogiclabs.jschema.tree.RuntimeContext;
import com.relogiclabs.jschema.type.EArray;
import com.relogiclabs.jschema.type.EBoolean;
import com.relogiclabs.jschema.type.EInteger;
import com.relogiclabs.jschema.type.ENull;
import com.relogiclabs.jschema.type.ENumber;
import com.relogiclabs.jschema.type.EObject;
import com.relogiclabs.jschema.type.EString;
import com.relogiclabs.jschema.type.EUndefined;
import com.relogiclabs.jschema.type.EValue;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.relogiclabs.jschema.internal.engine.ScriptErrorHelper.failOnDuplicateParameterName;
import static com.relogiclabs.jschema.internal.engine.ScriptErrorHelper.failOnFixedArity;
import static com.relogiclabs.jschema.internal.engine.ScriptErrorHelper.failOnVariadicArity;
import static com.relogiclabs.jschema.internal.loader.SchemaFunction.CONSTRAINT_PREFIX;
import static com.relogiclabs.jschema.internal.loader.SchemaFunction.VARIADIC_ARITY;
import static com.relogiclabs.jschema.internal.script.GFunction.CONSTRAINT_MODE;
import static com.relogiclabs.jschema.internal.script.GFunction.FUTURE_MODE;
import static com.relogiclabs.jschema.internal.script.GFunction.SUBROUTINE_MODE;
import static com.relogiclabs.jschema.internal.script.GParameter.hasVariadic;
import static com.relogiclabs.jschema.internal.util.CommonHelper.hasFlag;
import static com.relogiclabs.jschema.internal.util.StreamHelper.halt;
import static java.util.stream.Collectors.toMap;

public final class ScriptTreeHelper {
    private static final String TRYOF_VALUE = "value";
    private static final String TRYOF_ERROR = "error";

    private ScriptTreeHelper() {
        throw new UnsupportedOperationException("This class is not intended for instantiation");
    }

    public static boolean areEqual(EValue v1, EValue v2, RuntimeContext runtime) {
        v1 = dereference(v1);
        v2 = dereference(v2);
        if(v1 instanceof ENumber n1 && v2 instanceof ENumber n2)
            return runtime.areEqual(n1.toDouble(), n2.toDouble());
        if(v1 instanceof EBoolean b1 && v2 instanceof EBoolean b2)
            return b1.getValue() == b2.getValue();
        if(v1 instanceof EString s1 && v2 instanceof EString s2)
            return s1.getValue().equals(s2.getValue());
        if(v1 instanceof ENull && v2 instanceof ENull) return true;
        if(v1 instanceof EUndefined && v2 instanceof EUndefined) return true;
        if(v1 instanceof EArray a1 && v2 instanceof EArray a2) {
            if(a1.size() != a2.size()) return false;
            for(var i = 0; i < a1.size(); i++)
                if(!areEqual(a1.get(i), a2.get(i), runtime)) return false;
            return true;
        }
        if(v1 instanceof EObject o1 && v2 instanceof EObject o2) {
            if(o1.size() != o2.size()) return false;
            for(var k : o1.keySet())
                if(!areEqual(o1.get(k), o2.get(k), runtime)) return false;
            return true;
        }
        if(v1 instanceof GRange r1 && v2 instanceof GRange r2)
            return r1.equals(r2);
        return false;
    }

    public static EValue dereference(EValue value) {
        while(value instanceof GLeftValue lvalue)
            value = lvalue.getValue();
        return value;
    }

    static int getFunctionMode(TerminalNode constraint, TerminalNode future,
                TerminalNode subroutine) {
        return getFunctionMode(constraint)
            | getFunctionMode(future)
            | getFunctionMode(subroutine);
    }

    private static int getFunctionMode(TerminalNode node) {
        if(node == null) return 0;
        return switch(node.getSymbol().getType()) {
            case SchemaParser.G_CONSTRAINT -> CONSTRAINT_MODE;
            case SchemaParser.G_FUTURE -> FUTURE_MODE;
            case SchemaParser.G_SUBROUTINE -> SUBROUTINE_MODE;
            default -> 0;
        };
    }

    public static boolean isConstraint(int mode) {
        return hasFlag(mode, CONSTRAINT_MODE);
    }

    static ENumber increment(ENumber number) {
        if(number instanceof EInteger i) return GInteger.from(i.getValue() + 1);
        return GDouble.from(number.toDouble() + 1);
    }

    static ENumber decrement(ENumber number) {
        if(number instanceof EInteger i) return GInteger.from(i.getValue() - 1);
        return GDouble.from(number.toDouble() - 1);
    }

    static String generateKey(String baseName, GParameter[] parameters, boolean constraint) {
        var arity = hasVariadic(parameters) ? VARIADIC_ARITY : parameters.length;
        var baseKey = baseName + "#" + arity;
        return constraint ? CONSTRAINT_PREFIX + baseKey : baseKey;
    }

    static GParameter[] toParameters(List<TerminalNode> identifiers,
                TerminalNode ellipsis) {
        identifiers.stream().collect(toMap(ParseTree::getText, Function.identity(),
                (i1, i2) -> halt(failOnDuplicateParameterName(i2))
        ));
        var parameters = identifiers.stream().map(ParseTree::getText)
                .collect(Collectors.toCollection(ArrayList::new));
        if(ellipsis != null) updateLast(parameters, ellipsis.getText());
        return parameters.stream().map(GParameter::new).toArray(GParameter[]::new);
    }

    public static void areCompatible(GParameter[] parameters, List<EValue> arguments, String code) {
        if(hasVariadic(parameters)) {
            if(arguments.size() < parameters.length - 1) throw failOnVariadicArity(code);
            return;
        }
        if(arguments.size() != parameters.length) throw failOnFixedArity(code);
    }

    public static String stringify(Object object) {
        if(object instanceof EString s) return s.getValue();
        return object.toString();
    }

    static GObject createTryofMonad(EValue value, EValue error) {
        var object = new GObject(2);
        object.put(TRYOF_VALUE, value);
        object.put(TRYOF_ERROR, error);
        return object;
    }

    private static void updateLast(List<String> list, String suffix) {
        var last = list.size() - 1;
        list.set(last, list.get(last).concat(suffix));
    }
}