package com.relogiclabs.json.schema.internal.tree;

import com.relogiclabs.json.schema.exception.ClassInstantiationException;
import com.relogiclabs.json.schema.exception.CommonException;
import com.relogiclabs.json.schema.exception.DuplicateIncludeException;
import com.relogiclabs.json.schema.exception.FunctionNotFoundException;
import com.relogiclabs.json.schema.exception.InvalidFunctionException;
import com.relogiclabs.json.schema.exception.InvalidIncludeException;
import com.relogiclabs.json.schema.exception.JsonSchemaException;
import com.relogiclabs.json.schema.exception.NotFoundClassException;
import com.relogiclabs.json.schema.function.FunctionBase;
import com.relogiclabs.json.schema.message.ActualDetail;
import com.relogiclabs.json.schema.message.ErrorDetail;
import com.relogiclabs.json.schema.message.ExpectedDetail;
import com.relogiclabs.json.schema.message.MessageFormatter;
import com.relogiclabs.json.schema.tree.Context;
import com.relogiclabs.json.schema.tree.RuntimeContext;
import com.relogiclabs.json.schema.types.JFunction;
import com.relogiclabs.json.schema.types.JNode;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.relogiclabs.json.schema.internal.message.MessageHelper.getTypeName;
import static com.relogiclabs.json.schema.internal.tree.MethodPointer.getSignature;
import static com.relogiclabs.json.schema.internal.util.CollectionHelper.merge;
import static com.relogiclabs.json.schema.internal.util.StringHelper.concat;
import static com.relogiclabs.json.schema.message.ErrorCode.CLAS01;
import static com.relogiclabs.json.schema.message.ErrorCode.CLAS02;
import static com.relogiclabs.json.schema.message.ErrorCode.CLAS03;
import static com.relogiclabs.json.schema.message.ErrorCode.CLAS04;
import static com.relogiclabs.json.schema.message.ErrorCode.CLAS05;
import static com.relogiclabs.json.schema.message.ErrorCode.CLAS06;
import static com.relogiclabs.json.schema.message.ErrorCode.CLAS07;
import static com.relogiclabs.json.schema.message.ErrorCode.FUNC01;
import static com.relogiclabs.json.schema.message.ErrorCode.FUNC02;
import static com.relogiclabs.json.schema.message.ErrorCode.FUNC03;
import static com.relogiclabs.json.schema.message.ErrorCode.FUNC04;
import static com.relogiclabs.json.schema.message.ErrorCode.FUNC05;

public final class FunctionManager {
    private final Set<String> includes;
    private final Map<FunctionKey, List<MethodPointer>> functions;
    private final RuntimeContext runtime;

    public FunctionManager(RuntimeContext runtime) {
        this.runtime = runtime;
        this.includes = new HashSet<>();
        this.functions = new HashMap<>();
    }

    public void addClass(String className, Context context) {
        if(!includes.contains(className)) includes.add(className);
        else throw new DuplicateIncludeException(MessageFormatter.formatForSchema(
                CLAS01, "Class already included " + className, context));
        Class<?> subclass;
        try {
            subclass = Class.forName(className);
        } catch(ClassNotFoundException ex) {
            throw new NotFoundClassException(
                    MessageFormatter.formatForSchema(CLAS02, "Not found " + className, context));
        }
        var baseclass = FunctionBase.class;
        // if not FunctionBase's subclass
        if(!baseclass.isAssignableFrom(subclass))
            throw new InvalidIncludeException(MessageFormatter
                    .formatForSchema(CLAS03, subclass.getName() + " needs to inherit "
                            + baseclass.getName(), context));
        try {
            merge(functions, extractMethods(subclass, createInstance(subclass, context)));
        } catch(InvalidFunctionException ex) {
            throw new InvalidFunctionException(MessageFormatter
                .formatForSchema(ex.getCode(), ex.getMessage(), context));
        }
    }

    private Map<FunctionKey, List<MethodPointer>> extractMethods(Class<?> subclass, FunctionBase instance) {
        var baseclass = FunctionBase.class;
        Map<FunctionKey, List<MethodPointer>> functions = new HashMap<>();
        for(var m : subclass.getMethods()) {
            if(!baseclass.isAssignableFrom(m.getDeclaringClass())) continue;
            if(baseclass == m.getDeclaringClass()) continue;
            Parameter[] parameters = m.getParameters();
            if(m.getReturnType() != boolean.class && m.getReturnType() != Boolean.class)
                throw new InvalidFunctionException(FUNC01, concat("Function [", getSignature(m),
                        "] requires return type boolean"));
            if(parameters.length < 1) throw new InvalidFunctionException(FUNC02,
                    concat("Function [", getSignature(m), "] requires minimum one parameter"));
            var key = new FunctionKey(m, getParameterCount(parameters));
            var value = new MethodPointer(instance, m, parameters);
            var valueList = functions.get(key);
            if(valueList == null) valueList = new ArrayList<>();
            valueList.add(value);
            functions.put(key, valueList);
        }
        return functions;
    }

    private int getParameterCount(Parameter[] parameters) {
        for(var p : parameters) if(p.isVarArgs()) return -1;
        return parameters.length;
    }

    private FunctionBase createInstance(Class<?> type, Context context) {
        try {
            var constructor = type.getDeclaredConstructor(RuntimeContext.class);
            return (FunctionBase) constructor.newInstance(runtime);
        } catch (NoSuchMethodException e) {
            throw createException(CLAS04, e, type, context);
        } catch (InstantiationException e) {
            throw createException(CLAS05, e, type, context);
        } catch (InvocationTargetException e) {
            throw createException(CLAS06, e, type, context);
        } catch (IllegalAccessException e) {
            throw createException(CLAS07, e, type, context);
        }
    }

    private static CommonException createException(String code, Exception ex, Class<?> type, Context context) {
        return new ClassInstantiationException(MessageFormatter.formatForSchema(
                code, "Fail to create instance of " + type.getName(), context), ex);
    }

    public boolean invokeFunction(JFunction function, JNode target) {
        var methods = getMethods(function);
        Parameter mismatchParameter = null;

        for(var method : methods) {
            var parameters = method.getParameters();
            var arguments = function.getArguments();
            var schemaArgs = processArgs(parameters, arguments);
            if(schemaArgs == null) continue;
            if(isMatch(parameters.get(0), target))
                return method.invoke(function, addTarget(schemaArgs, target));
            mismatchParameter = parameters.get(0);
        }

        if(mismatchParameter != null)
            return failWith(new JsonSchemaException(new ErrorDetail(FUNC03,
                    "Function ", function.getOutline(), " is incompatible with the target data type"),
                    new ExpectedDetail(function, "applying to a supported data type such as ",
                            getTypeName(mismatchParameter.getType())),
                    new ActualDetail(target, "applied to an unsupported data type ",
                            getTypeName(target.getClass()), " of ", target)));

        return failWith(new FunctionNotFoundException(MessageFormatter
            .formatForSchema(FUNC04, function.getOutline(), function.getContext())));
    }

    private List<MethodPointer> getMethods(JFunction function) {
        var methodPointers = functions.get(new FunctionKey(function));
        if(methodPointers == null)
            methodPointers = functions.get(new FunctionKey(function.getName(), -1));
        if(methodPointers == null) throw new FunctionNotFoundException(MessageFormatter
                .formatForSchema(FUNC05, "Not found " + function.getOutline(), function.getContext()));
        return methodPointers;
    }

    private static List<Object> addTarget(List<Object> arguments, JNode target) {
        var args = new ArrayList<>(1 + arguments.size());
        args.add(target);
        args.addAll(arguments);
        return args;
    }

    private static List<Object> processArgs(List<Parameter> parameters, List<JNode> arguments) {
        var result = new ArrayList<>();
        for(int i = 1; i < parameters.size(); i++) {
            if(parameters.get(i).isVarArgs()) {
                List<JNode> rest = arguments.subList(i - 1, arguments.size() - i + 1);
                var varArgs = processVarArgs(parameters.get(i), rest);
                if(varArgs == null) return null;
                result.add(varArgs);
                break;
            }
            if(!isMatch(parameters.get(i), arguments.get(i - 1))) return null;
            result.add(arguments.get(i - 1));
        }
        return result;
    }

    private static boolean isMatch(Parameter parameter, JNode argument) {
        return parameter.getType().isInstance(argument);
    }

    private static Object processVarArgs(Parameter parameter, List<JNode> arguments) {
        var componentType = parameter.getType().getComponentType();
        if(componentType == null) throw new IllegalStateException();
        Object result = Array.newInstance(componentType, arguments.size());
        for(var i = 0; i < arguments.size(); i++) {
            var arg = arguments.get(i);
            if(!componentType.isInstance(arg)) return null;
            Array.set(result, i, arg);
        }
        return result;
    }

    private boolean failWith(RuntimeException exception) {
        return runtime.failWith(exception);
    }
}