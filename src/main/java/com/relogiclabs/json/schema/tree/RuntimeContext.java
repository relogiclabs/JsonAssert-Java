package com.relogiclabs.json.schema.tree;

import com.relogiclabs.json.schema.exception.DuplicateDefinitionException;
import com.relogiclabs.json.schema.internal.tree.FunctionManager;
import com.relogiclabs.json.schema.internal.tree.PragmaManager;
import com.relogiclabs.json.schema.message.MessageFormatter;
import com.relogiclabs.json.schema.types.JAlias;
import com.relogiclabs.json.schema.types.JDefinition;
import com.relogiclabs.json.schema.types.JFunction;
import com.relogiclabs.json.schema.types.JInclude;
import com.relogiclabs.json.schema.types.JNode;
import com.relogiclabs.json.schema.types.JPragma;
import com.relogiclabs.json.schema.types.JValidator;
import lombok.Getter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.function.Supplier;

import static com.relogiclabs.json.schema.internal.util.StringHelper.concat;
import static com.relogiclabs.json.schema.internal.util.StringHelper.quote;
import static com.relogiclabs.json.schema.message.ErrorCode.DEFI01;


public final class RuntimeContext {
    private final FunctionManager functionManager;
    private final PragmaManager pragmaManager;
    private int disableException = 0;

    @Getter private final Map<JAlias, JValidator> definitions;
    @Getter private final boolean throwException;
    @Getter private final Queue<Exception> exceptions;
    @Getter private final MessageFormatter messageFormatter;


    public RuntimeContext(MessageFormatter messageFormatter, boolean throwException) {
        this.messageFormatter = messageFormatter;
        this.throwException = throwException;
        this.definitions = new HashMap<>();
        this.functionManager = new FunctionManager(this);
        this.pragmaManager = new PragmaManager();
        this.exceptions = new LinkedList<>();
    }

    public JPragma addPragma(JPragma pragma) {
        return pragmaManager.addPragma(pragma);
    }

    public JInclude addClass(JInclude include) {
        addClass(include.getClassName(), include.getContext());
        return include;
    }

    public void addClass(String className, Context context) {
        functionManager.addClass(className, context);
    }

    public JDefinition addDefinition(JDefinition definition) {
        var previous = definitions.get(definition.getAlias());
        if(previous != null)
            throw new DuplicateDefinitionException(MessageFormatter.formatForSchema(
                DEFI01, concat("Duplicate definition of ", quote(definition.getAlias()),
                            " is found and already defined as ", previous.getOutline()),
                    definition.getContext()));
        definitions.put(definition.getAlias(), definition.getValidator());
        return definition;
    }

    public boolean getIgnoreUndefinedProperties() {
        return pragmaManager.getIgnoreUndefinedProperties();
    }

    public double getFloatingPointTolerance() {
        return pragmaManager.getFloatingPointTolerance();
    }

    public boolean getIgnoreObjectPropertyOrder() {
        return pragmaManager.getIgnoreObjectPropertyOrder();
    }

    public boolean invokeFunction(JFunction function, JNode target) {
        return functionManager.invokeFunction(function, target);
    }

    public boolean areEqual(double value1, double value2) {
        return Math.abs(value1 - value2) < getFloatingPointTolerance();
    }

    public <T> T tryMatch(Supplier<T> function) {
        try {
            disableException += 1;
            return function.get();
        } finally {
            disableException -= 1;
        }
    }

    public boolean failWith(RuntimeException exception) {
        if(throwException && disableException == 0) throw exception;
        if(disableException == 0) exceptions.add(exception);
        return false;
    }
}