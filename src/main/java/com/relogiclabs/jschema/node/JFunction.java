package com.relogiclabs.jschema.node;

import com.relogiclabs.jschema.exception.FunctionValidationException;
import com.relogiclabs.jschema.internal.builder.JFunctionBuilder;
import com.relogiclabs.jschema.internal.message.ActualHelper;
import com.relogiclabs.jschema.internal.message.ExpectedHelper;
import com.relogiclabs.jschema.internal.tree.FunctionCache;
import com.relogiclabs.jschema.message.ErrorDetail;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;

import static com.relogiclabs.jschema.internal.message.MessageHelper.NestedFunctionFailed;
import static com.relogiclabs.jschema.internal.util.StreamHelper.forEachTrue;
import static com.relogiclabs.jschema.internal.util.StringHelper.join;
import static com.relogiclabs.jschema.message.ErrorCode.FNCFAL01;
import static java.util.Objects.requireNonNull;

@Getter
@EqualsAndHashCode
public final class JFunction extends JBranch implements NestedMode {
    static final String NESTED_MARKER = "*";
    private final String name;
    private final boolean nested;
    private final List<JNode> arguments;
    private final FunctionCache cache;

    private JFunction(JFunctionBuilder builder) {
        super(builder);
        name = requireNonNull(builder.name());
        nested = requireNonNull(builder.nested());
        arguments = requireNonNull(builder.arguments());
        children = arguments;
        cache = new FunctionCache();
    }

    public static JFunction from(JFunctionBuilder builder) {
        return new JFunction(builder).initialize();
    }

    @Override
    public boolean match(JNode node) {
        if(!nested) return invokeFunction(node);
        if(!(node instanceof JComposite composite)) return fail(new FunctionValidationException(
            new ErrorDetail(FNCFAL01, NestedFunctionFailed),
            ExpectedHelper.asNestedFunctionFailed(this),
            ActualHelper.asNestedFunctionFailed(node)));
        return forEachTrue(composite.components().stream().map(this::invokeFunction));
    }

    private boolean invokeFunction(JNode node) {
        return getRuntime().getConstraints().invoke(this, node);
    }

    @Override
    public boolean isNested() {
        return nested;
    }

    boolean isApplicable(JNode node) {
        return !nested || node instanceof JComposite;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(name);
        if(nested) builder.append(NESTED_MARKER);
        builder.append(join(arguments, ", ", "(", ")"));
        return builder.toString();
    }
}