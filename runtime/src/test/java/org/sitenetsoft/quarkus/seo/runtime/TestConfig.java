package org.sitenetsoft.quarkus.seo.runtime;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Fakes the two config interfaces. A call returns the value stored under its dotted path,
 * or an empty default for the return type.
 */
final class TestConfig {

    private final Map<String, Object> values = new HashMap<>();

    TestConfig with(String path, Object value) {
        values.put(path, value);
        return this;
    }

    SeoConfig config() {
        return proxy(SeoConfig.class, "");
    }

    SeoBuildConfig buildConfig() {
        return proxy(SeoBuildConfig.class, "");
    }

    private <T> T proxy(Class<T> type, String prefix) {
        InvocationHandler handler = (self, method, args) -> answer(method, prefix);
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, handler));
    }

    private Object answer(Method method, String prefix) {
        String name = method.getName();
        if ("defaultTitle".equals(name)) {
            name = "default";
        }
        String path = prefix.isEmpty() ? name : prefix + "." + name;
        Class<?> type = method.getReturnType();
        if (values.containsKey(path)) {
            Object value = values.get(path);
            return type == Optional.class ? Optional.of(value) : value;
        }
        if (type == Optional.class) {
            return Optional.empty();
        }
        if (type == boolean.class) {
            return true;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == Map.class) {
            return Map.of();
        }
        if (type == List.class) {
            return List.of();
        }
        if (type.isEnum()) {
            return type.getEnumConstants()[0];
        }
        if (type.isInterface()) {
            return proxy(type, path);
        }
        throw new IllegalStateException("No default for " + path);
    }
}
