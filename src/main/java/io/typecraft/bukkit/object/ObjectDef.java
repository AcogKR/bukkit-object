package io.typecraft.bukkit.object;

import lombok.Value;
import lombok.With;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

@Value(staticConstructor = "of")
@With
public class ObjectDef {
    Set<FieldDef> fields;
    TypeDef objectType;
    Class<?> builderClass;
    public static final ObjectDef empty = new ObjectDef(Collections.emptySet(), TypeDef.empty, Void.class);

    public boolean isEmpty() {
        return builderClass == Void.class;
    }

    public static ObjectDef from(Class<?> clazz) {
        return clazz.getName().endsWith("kt")
                ? kotlin(clazz)
                : java(clazz);
    }

    public static ObjectDef java(Class<?> clazz) {
        // check lombok @Builder
        Class<?> builderclass = Reflections.findClass(String.format("%s$%sBuilder", clazz.getName(), clazz.getSimpleName())).orElse(null);
        TypeDef objectType = TypeDef.from(clazz).orElse(null);
        if (builderclass == null || objectType == null) {
            return ObjectDef.empty;
        }
        Set<FieldDef> fields = new HashSet<>();
        for (Field field : clazz.getDeclaredFields()) {
            String fieldName = field.getName();
            String methodName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            Method method = null;
            try {
                method = clazz.getMethod(methodName);
            } catch (NoSuchMethodException e) {
                // Ignore
            }
            TypeDef fieldType = method != null ? TypeDef.from(method.getGenericReturnType()).orElse(null) : null;
            if (fieldType != null) {
                fields.add(FieldDef.of(fieldName, fieldType, methodName));
            }
        }
        return new ObjectDef(fields, objectType, builderclass);
    }

    public static ObjectDef kotlin(Class<?> clazz) {
        Method[] methods = clazz.getDeclaredMethods();
        Method copyMethod = null;
        for (Method method : methods) {
            if (method.getName().equals("copy") && Function.class.isAssignableFrom(method.getReturnType())) {
                copyMethod = method;
                break;
            }
        }
        TypeDef objectType = TypeDef.from(clazz).orElse(null);
        if (copyMethod == null || objectType == null) {
            return ObjectDef.empty;
        }
        Set<FieldDef> fields = new HashSet<>();
        for (Field field : clazz.getDeclaredFields()) {
            TypeDef fieldType = TypeDef.from(field.getGenericType()).orElse(null);
            if (fieldType != null) {
                fields.add(FieldDef.of(field.getName(), fieldType));
            }
        }
        return new ObjectDef(fields, objectType, clazz);
    }

}
