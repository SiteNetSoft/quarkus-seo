package org.sitenetsoft.quarkus.seo.deployment;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import org.sitenetsoft.quarkus.seo.runtime.SeoPaths;

/**
 * Find the prefix that Quarkus REST or RESTEasy Classic puts in front of every JAX-RS {@code @Path}.
 *
 * <p>
 * The class reads the annotation by name, so the deployment module needs no REST dependency.
 */
final class RestPaths {

    private static final DotName APPLICATION_PATH = DotName.createSimple("jakarta.ws.rs.ApplicationPath");

    private RestPaths() {
    }

    /**
     * Return the configuration key that holds the REST path of the REST stack in use.
     *
     * @param resteasyClassic true when RESTEasy Classic is the REST stack
     * @return {@code quarkus.resteasy.path} for RESTEasy Classic, {@code quarkus.rest.path} otherwise
     */
    static String restPathKey(boolean resteasyClassic) {
        return resteasyClassic ? "quarkus.resteasy.path" : "quarkus.rest.path";
    }

    /**
     * Return the JAX-RS prefix of the application.
     *
     * <p>
     * An {@code @ApplicationPath} on an {@code Application} subclass wins, as in Quarkus REST. Without one, the
     * value of the REST path key applies. See {@link #restPathKey(boolean)}. When several classes carry the annotation, the class whose name
     * sorts first wins.
     *
     * @param index the combined index of the application
     * @param restPath the value of the REST path key, empty when the key is not set
     * @return the prefix with one leading slash and no trailing slash, or an empty string for the root
     */
    static String prefix(IndexView index, Optional<String> restPath) {
        List<AnnotationInstance> annotations = new ArrayList<>();
        for (AnnotationInstance annotation : index.getAnnotations(APPLICATION_PATH)) {
            if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                ClassInfo type = annotation.target().asClass();
                if (!type.isInterface() && !Modifier.isAbstract(type.flags())) {
                    annotations.add(annotation);
                }
            }
        }
        String value;
        if (annotations.isEmpty()) {
            value = restPath.orElse("/");
        } else {
            annotations.sort(Comparator.comparing(annotation -> annotation.target().asClass().name().toString()));
            AnnotationInstance first = annotations.get(0);
            value = first.value() == null ? "/" : first.value().asString();
        }
        String normalized = SeoPaths.normalize(value);
        return "/".equals(normalized) ? "" : normalized;
    }
}
