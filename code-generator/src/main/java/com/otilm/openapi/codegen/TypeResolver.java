package com.otilm.openapi.codegen;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Handles type resolution and import management for generated code. Detects naming conflicts when multiple classes
 * share the same simple name and determines when to use fully qualified names.
 */
public class TypeResolver {

    private final Set<String> conflictingNames;
    private final Set<String> imports;

    public TypeResolver(Class<?> interfaceClass) {
        Map<String, Set<Class<?>>> simpleNameToClasses = buildSimpleNameMap(interfaceClass);
        this.conflictingNames = detectConflicts(simpleNameToClasses);
        this.imports = collectImports(interfaceClass);
    }

    /**
     * Returns the set of import statements (fully qualified names) needed for the generated class. Excludes imports for
     * conflicting names (which will use FQN inline).
     */
    public Set<String> getNonConflictingImports() {
        Set<String> result = new LinkedHashSet<>();
        for (String imp : imports) {
            String simpleName = getSimpleName(imp);
            if (!conflictingNames.contains(simpleName)) {
                result.add(imp);
            }
        }
        return result;
    }

    /**
     * Gets the appropriate type name to use in generated code. Returns a simple name for non-conflicting types, fully
     * qualified name for conflicting types.
     */
    public String getTypeName(Class<?> type) {
        return getTypeName((Type) type);
    }

    /**
     * Gets the appropriate type name to use in generated code for generic types.
     */
    public String getTypeName(Type type) {
        if (type instanceof Class<?> clazz) {
            return getClassTypeName(clazz);
        }

        if (type instanceof ParameterizedType parameterizedType) {
            return getParameterizedTypeName(parameterizedType);
        }

        if (type instanceof WildcardType wildcardType) {
            return getWildcardTypeName(wildcardType);
        }

        if (type instanceof TypeVariable<?> typeVariable) {
            return typeVariable.getName();
        }

        if (type instanceof GenericArrayType arrayType) {
            return getGenericArrayTypeName(arrayType);
        }

        return type.getTypeName();
    }

    private String getClassTypeName(Class<?> clazz) {
        if (clazz.isArray()) {
            return getTypeName(clazz.getComponentType()) + "[]";
        }

        String simpleName = clazz.getSimpleName();
        if (conflictingNames.contains(simpleName)) {
            return clazz.getName().replace('$', '.');
        }
        return simpleName;
    }

    private String getParameterizedTypeName(ParameterizedType parameterizedType) {
        Type rawType = parameterizedType.getRawType();
        Type[] typeArguments = parameterizedType.getActualTypeArguments();

        StringBuilder builder = new StringBuilder();
        builder.append(getTypeName(rawType));
        builder.append("<");
        for (int i = 0; i < typeArguments.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(getTypeName(typeArguments[i]));
        }
        builder.append(">");
        return builder.toString();
    }

    private String getWildcardTypeName(WildcardType wildcardType) {
        Type[] lowerBounds = wildcardType.getLowerBounds();
        if (lowerBounds.length > 0) {
            return "? super " + joinBounds(lowerBounds);
        }
        Type[] upperBounds = wildcardType.getUpperBounds();
        if (upperBounds.length == 0 || isObjectOnly(upperBounds)) {
            return "?";
        }
        return "? extends " + joinBounds(upperBounds);
    }

    private String getGenericArrayTypeName(GenericArrayType arrayType) {
        return getTypeName(arrayType.getGenericComponentType()) + "[]";
    }

    /**
     * Builds a map from simple class names to their fully qualified Class objects for all types used in the interface
     * methods.
     */
    private Map<String, Set<Class<?>>> buildSimpleNameMap(Class<?> interfaceClass) {
        Map<String, Set<Class<?>>> map = new HashMap<>();

        for (Method method : interfaceClass.getMethods()) {
            if (isObjectMethod(method)) {
                continue;
            }

            addTypeToMap(map, method.getGenericReturnType());

            for (Type paramType : method.getGenericParameterTypes()) {
                addTypeToMap(map, paramType);
            }

            for (Type exception : method.getGenericExceptionTypes()) {
                addTypeToMap(map, exception);
            }
        }

        return map;
    }

    /**
     * Adds a type and any nested generic component types to the simple name map.
     */
    private void addTypeToMap(Map<String, Set<Class<?>>> map, Type type) {
        if (type instanceof Class<?>) {
            addClassToMap(map, (Class<?>) type);
            return;
        }

        if (type instanceof ParameterizedType parameterizedType) {
            addTypeToMap(map, parameterizedType.getRawType());
            for (Type argument : parameterizedType.getActualTypeArguments()) {
                addTypeToMap(map, argument);
            }
            return;
        }

        if (type instanceof WildcardType wildcardType) {
            for (Type bound : wildcardType.getUpperBounds()) {
                addTypeToMap(map, bound);
            }
            for (Type bound : wildcardType.getLowerBounds()) {
                addTypeToMap(map, bound);
            }
            return;
        }

        if (type instanceof TypeVariable<?> typeVariable) {
            for (Type bound : typeVariable.getBounds()) {
                addTypeToMap(map, bound);
            }
            return;
        }

        if (type instanceof GenericArrayType arrayType) {
            addTypeToMap(map, arrayType.getGenericComponentType());
        }
    }

    /**
     * Adds a class and its component type (if array) to the simple name map.
     */
    private void addClassToMap(Map<String, Set<Class<?>>> map, Class<?> type) {
        if (type.isPrimitive()) {
            return;
        }

        if (type.isArray()) {
            addClassToMap(map, type.getComponentType());
            return;
        }

        if (shouldImportType(type)) {
            String simpleName = type.getSimpleName();
            map.computeIfAbsent(simpleName, k -> new HashSet<>()).add(type);
        }
    }

    /**
     * Detects class names that have multiple different fully qualified names. Returns a set of simple names that have
     * conflicts.
     */
    private Set<String> detectConflicts(Map<String, Set<Class<?>>> simpleNameToClass) {
        Set<String> conflicts = new HashSet<>();

        for (Map.Entry<String, Set<Class<?>>> entry : simpleNameToClass.entrySet()) {
            if (entry.getValue().size() > 1) {
                conflicts.add(entry.getKey());
            }
        }

        return conflicts;
    }

    /**
     * Collects all imports needed for the interface implementation.
     */
    private Set<String> collectImports(Class<?> interfaceClass) {
        Set<String> result = new LinkedHashSet<>();

        for (Method method : interfaceClass.getMethods()) {
            if (isObjectMethod(method)) {
                continue;
            }

            addImportForType(result, method.getGenericReturnType());

            for (Type paramType : method.getGenericParameterTypes()) {
                addImportForType(result, paramType);
            }

            for (Type exception : method.getGenericExceptionTypes()) {
                addImportForType(result, exception);
            }
        }

        return result;
    }

    /**
     * Adds import for a type if it's not a primitive or java.lang type.
     */
    private void addImportForType(Set<String> imports, Type type) {
        if (type instanceof Class<?>) {
            addImportForType(imports, (Class<?>) type);
            return;
        }

        if (type instanceof ParameterizedType parameterizedType) {
            addImportForType(imports, parameterizedType.getRawType());
            for (Type argument : parameterizedType.getActualTypeArguments()) {
                addImportForType(imports, argument);
            }
            return;
        }

        if (type instanceof WildcardType wildcardType) {
            for (Type bound : wildcardType.getUpperBounds()) {
                addImportForType(imports, bound);
            }
            for (Type bound : wildcardType.getLowerBounds()) {
                addImportForType(imports, bound);
            }
            return;
        }

        if (type instanceof TypeVariable<?> typeVariable) {
            for (Type bound : typeVariable.getBounds()) {
                addImportForType(imports, bound);
            }
            return;
        }

        if (type instanceof GenericArrayType arrayType) {
            addImportForType(imports, arrayType.getGenericComponentType());
        }
    }

    private void addImportForType(Set<String> imports, Class<?> type) {
        if (type.isPrimitive()) {
            return;
        }

        if (type.isArray()) {
            addImportForType(imports, type.getComponentType());
            return;
        }

        if (shouldImportType(type)) {
            imports.add(type.getName().replace('$', '.'));
        }
    }

    /**
     * Determines if a type should be imported (not primitive, array, or java.lang).
     */
    private boolean shouldImportType(Class<?> type) {
        if (type.isPrimitive() || type.isArray()) {
            return false;
        }

        String packageName = type.getPackage() != null ? type.getPackage().getName() : "";
        return !packageName.isEmpty() && !packageName.equals("java.lang");
    }

    /**
     * Checks if a method is declared in the Object class.
     */
    private boolean isObjectMethod(Method method) {
        return method.getDeclaringClass().equals(Object.class);
    }

    /**
     * Extracts a simple name from a fully qualified name.
     */
    private String getSimpleName(String fullyQualifiedName) {
        return fullyQualifiedName.substring(fullyQualifiedName.lastIndexOf('.') + 1);
    }

    private boolean isObjectOnly(Type[] bounds) {
        if (bounds.length != 1) {
            return false;
        }
        Type bound = bounds[0];
        return bound instanceof Class<?> && Object.class.equals(bound);
    }

    private String joinBounds(Type[] bounds) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < bounds.length; i++) {
            if (i > 0) {
                builder.append(" & ");
            }
            builder.append(getTypeName(bounds[i]));
        }
        return builder.toString();
    }
}
