package org.sitenetsoft.quarkus.seo;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class PublicApiTest {

    private static final String PACKAGE = "org.sitenetsoft.quarkus.seo";
    private static final String INTERNAL = PACKAGE + ".runtime.";

    @Test
    void publicSignaturesExposeNoInternalType() throws Exception {
        List<String> leaks = new ArrayList<>();
        List<Class<?>> types = publicTypes();
        assertThat(types).contains(Seo.class, SeoCaches.class, ResolvedSeo.Meta.class, IndexNow.class);
        for (Class<?> type : types) {
            for (Method method : type.getDeclaredMethods()) {
                if (visible(method.getModifiers()) && !method.isSynthetic()) {
                    check(leaks, type.getName() + "#" + method.getName(), method.getGenericReturnType());
                    for (Type parameter : method.getGenericParameterTypes()) {
                        check(leaks, type.getName() + "#" + method.getName(), parameter);
                    }
                }
            }
            for (Constructor<?> constructor : type.getDeclaredConstructors()) {
                if (visible(constructor.getModifiers())) {
                    for (Type parameter : constructor.getGenericParameterTypes()) {
                        check(leaks, type.getName() + "#<init>", parameter);
                    }
                }
            }
            for (Field field : type.getDeclaredFields()) {
                if (visible(field.getModifiers())) {
                    check(leaks, type.getName() + "." + field.getName(), field.getGenericType());
                }
            }
        }
        assertThat(leaks).isEmpty();
    }

    private static boolean visible(int modifiers) {
        return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers);
    }

    private static void check(List<String> leaks, String member, Type type) {
        if (type instanceof Class<?> raw) {
            Class<?> component = raw;
            while (component.isArray()) {
                component = component.getComponentType();
            }
            if (component.getName().startsWith(INTERNAL)) {
                leaks.add(member + " uses " + component.getName());
            }
        } else if (type instanceof ParameterizedType parameterized) {
            check(leaks, member, parameterized.getRawType());
            for (Type argument : parameterized.getActualTypeArguments()) {
                check(leaks, member, argument);
            }
        } else if (type instanceof GenericArrayType array) {
            check(leaks, member, array.getGenericComponentType());
        } else if (type instanceof WildcardType wildcard) {
            for (Type bound : wildcard.getUpperBounds()) {
                check(leaks, member, bound);
            }
            for (Type bound : wildcard.getLowerBounds()) {
                check(leaks, member, bound);
            }
        }
    }

    private static List<Class<?>> publicTypes() throws IOException, URISyntaxException, ClassNotFoundException {
        Path directory = Path.of(Seo.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                .resolve(PACKAGE.replace('.', '/'));
        List<Class<?>> types = new ArrayList<>();
        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.toList()) {
                String name = file.getFileName().toString();
                if (!name.endsWith(".class") || name.equals("package-info.class")) {
                    continue;
                }
                Class<?> type = Class.forName(PACKAGE + "." + name.substring(0, name.length() - ".class".length()),
                        false, PublicApiTest.class.getClassLoader());
                if (Modifier.isPublic(type.getModifiers()) && !type.isAnonymousClass()) {
                    types.add(type);
                }
            }
        }
        return types;
    }
}
