package org.json.rpc.server;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

public class SupportedTypes {

    public static String getTypeName(Class<?> clazz) {
        if (clazz == null) {
            throw new NullPointerException("clazz");
        }

        if (clazz == void.class) {
            return void.class.getName();
        }

        if (clazz == boolean.class || Boolean.class == clazz) {
            return boolean.class.getName();
        }

        if (clazz == double.class || clazz == float.class
                || Double.class == clazz || Float.class == clazz) {
            return double.class.getName();
        }

        if (clazz == byte.class || clazz == char.class || clazz == int.class
                || clazz == long.class || Number.class.isAssignableFrom(clazz)) {
            return int.class.getName();
        }

        if (clazz == String.class) {
            return "string";
        }

        if (clazz.isArray()) {
            return "array";
        }

        return "struct";
    }

    public static void isAllowed(Method method) {
        if (method == null) {
            throw new NullPointerException("method");
        }

        if (!isAllowed(method.getReturnType())) {
            throw new IllegalArgumentException("unsupported return type '"
                    + method.getReturnType() + "' for method : " + method);
        }

        for (Class<?> parameterType : method.getParameterTypes()) {
            if (!isAllowed(method.getReturnType())) {
                throw new IllegalArgumentException(
                        "unsupported parameter type '" + parameterType
                                + "' for method : " + method);
            }
        }
    }

    public static boolean isAllowed(Class<?> clazz) {
        if (clazz == null) {
            throw new NullPointerException("clazz");
        }

        if (clazz.isPrimitive()) {
            return true;
        }

        if (clazz.isArray()) {
            return isAllowed(clazz.getComponentType());
        }

        if (Boolean.class == clazz) {
            return true;
        }

        if (Number.class.isAssignableFrom(clazz)) {
            return true;
        }

        if (String.class == clazz) {
            return true;
        }

        if (clazz.isAssignableFrom(Collection.class)
                || clazz.isAssignableFrom(Map.class)) {
            return false;
        }

        return true;
    }

}
