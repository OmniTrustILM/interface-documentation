package com.otilm.openapi.config.loader;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves top-level extension values that are wrapped in backticks as static field references.
 */
public class ExtensionReferenceResolver {

    /**
     * Resolves a single top-level extension value.
     * <p>
     * Rules: - only whole-string backtick values are treated as references - non-reference values are returned
     * unchanged - references must point to public static fields with Map/List values
     */
    public Object resolveTopLevelValue(Object value, String extensionKey, String contextLabel) {
        if (!(value instanceof String stringValue)) {
            return value;
        }

        if (!stringValue.contains("`")) {
            return value;
        }

        if (!isWholeBacktickReference(stringValue)) {
            throw new IllegalStateException(String
                    .format("Invalid extension value for key '%s' in %s. "
                            + "Backticks are only allowed for whole-string references in format `fully.qualified.Class.FIELD`.",
                            extensionKey, contextLabel));
        }

        String reference = stringValue.substring(1, stringValue.length() - 1);
        ResolvedReference resolvedReference = parseReference(reference, extensionKey, contextLabel);
        Object resolvedValue = resolveFieldValue(resolvedReference, extensionKey, contextLabel);

        if (!(resolvedValue instanceof Map<?, ?>) && !(resolvedValue instanceof List<?>)) {
            throw new IllegalStateException(String
                    .format("Invalid extension reference for key '%s' in %s. Field '%s.%s' resolves to unsupported top-level type '%s'. "
                            + "Only Map and List are supported.", extensionKey, contextLabel,
                            resolvedReference.className(), resolvedReference.fieldName(),
                            resolvedValue == null ? "null" : resolvedValue.getClass().getName()));
        }

        return sanitizeYamlSafeValue(resolvedValue, extensionKey, contextLabel, "value");
    }

    private boolean isWholeBacktickReference(String value) {
        if (value.length() < 2 || value.charAt(0) != '`' || value.charAt(value.length() - 1) != '`') {
            return false;
        }
        return value.substring(1, value.length() - 1).indexOf('`') < 0;
    }

    private ResolvedReference parseReference(String reference, String extensionKey, String contextLabel) {
        int lastDot = reference.lastIndexOf('.');
        if (lastDot <= 0 || lastDot >= reference.length() - 1) {
            throw new IllegalStateException(String
                    .format("Invalid extension reference for key '%s' in %s. Value '%s' does not match required format 'fully.qualified.Class.FIELD'.",
                            extensionKey, contextLabel, reference));
        }

        String className = reference.substring(0, lastDot);
        String fieldName = reference.substring(lastDot + 1);

        if (!className.contains(".")) {
            throw new IllegalStateException(String
                    .format("Invalid extension reference for key '%s' in %s. Class name '%s' must be fully qualified.",
                            extensionKey, contextLabel, className));
        }

        return new ResolvedReference(className, fieldName);
    }

    private Object resolveFieldValue(ResolvedReference reference, String extensionKey, String contextLabel) {
        try {
            Class<?> clazz = Class.forName(reference.className());
            Field field = clazz.getDeclaredField(reference.fieldName());
            int modifiers = field.getModifiers();

            if (!Modifier.isPublic(modifiers) || !Modifier.isStatic(modifiers)) {
                throw new IllegalStateException(String
                        .format("Invalid extension reference for key '%s' in %s. Field '%s.%s' must be public static.",
                                extensionKey, contextLabel, reference.className(), reference.fieldName()));
            }

            return field.get(null);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(String
                    .format("Invalid extension reference for key '%s' in %s. Class '%s' was not found.", extensionKey,
                            contextLabel, reference.className()),
                    e);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(String
                    .format("Invalid extension reference for key '%s' in %s. Field '%s' was not found in class '%s'.",
                            extensionKey, contextLabel, reference.fieldName(), reference.className()),
                    e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(String
                    .format("Invalid extension reference for key '%s' in %s. Cannot access field '%s.%s'.",
                            extensionKey, contextLabel, reference.className(), reference.fieldName()),
                    e);
        }
    }

    private Object sanitizeYamlSafeValue(Object value, String extensionKey, String contextLabel, String path) {
        if (isYamlSafeScalar(value)) {
            return value;
        }

        if (value instanceof Map<?, ?> mapValue) {
            Map<Object, Object> sanitized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
                if (!isYamlSafeScalar(entry.getKey())) {
                    throw new IllegalStateException(String
                            .format("Invalid extension reference for key '%s' in %s. Map key at path '%s' has unsupported type '%s'.",
                                    extensionKey, contextLabel, path, entry.getKey().getClass().getName()));
                }
                Object nested = sanitizeYamlSafeValue(entry.getValue(), extensionKey, contextLabel,
                        path + "." + entry.getKey());
                sanitized.put(entry.getKey(), nested);
            }
            return sanitized;
        }

        if (value instanceof List<?> listValue) {
            List<Object> sanitized = new ArrayList<>(listValue.size());
            for (int i = 0; i < listValue.size(); i++) {
                sanitized
                        .add(sanitizeYamlSafeValue(listValue.get(i), extensionKey, contextLabel, path + "[" + i + "]"));
            }
            return sanitized;
        }

        throw new IllegalStateException(String
                .format("Invalid extension reference for key '%s' in %s. Value at path '%s' has unsupported type '%s'. "
                        + "Only YAML-safe scalars/maps/lists are supported.", extensionKey, contextLabel, path,
                        value.getClass().getName()));
    }

    private boolean isYamlSafeScalar(Object value) {
        return value == null || value instanceof String || value instanceof Number || value instanceof Boolean;
    }

    private record ResolvedReference(String className, String fieldName) {
    }
}
