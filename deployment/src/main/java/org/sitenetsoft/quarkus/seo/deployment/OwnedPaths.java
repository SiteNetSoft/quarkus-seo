package org.sitenetsoft.quarkus.seo.deployment;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.vertx.http.deployment.spi.StaticResourcesBuildItem;

/**
 * Find the paths that the application serves itself.
 *
 * <p>
 * The class reads JAX-RS and Reactive Routes annotations by name, so the deployment module needs no REST or
 * Reactive Routes dependency. It does not see the {@code @ApplicationPath} prefix, the {@code quarkus.rest.path}
 * prefix, a {@code @Route} method with no {@code path} or a {@code regex} route.
 */
final class OwnedPaths {

    private static final DotName PATH = DotName.createSimple("jakarta.ws.rs.Path");
    private static final DotName ROUTE = DotName.createSimple("io.quarkus.vertx.web.Route");
    private static final DotName ROUTES = DotName.createSimple("io.quarkus.vertx.web.Route$Routes");
    private static final DotName ROUTE_BASE = DotName.createSimple("io.quarkus.vertx.web.RouteBase");

    private OwnedPaths() {
    }

    /**
     * Read the JAX-RS resource paths and the Reactive Routes paths of the application.
     *
     * <p>
     * A method {@code @Path} joins the {@code @Path} of its class. A {@code @Route} path joins the {@code @RouteBase}
     * path of its class. Interfaces are skipped because REST client interfaces also carry {@code @Path}. A method
     * {@code @Path} in a class with no {@code @Path} is skipped because a sub-resource has no fixed path.
     *
     * @param index the combined index of the application
     * @return the paths with one leading slash and no trailing slash
     */
    static Set<String> fromIndex(IndexView index) {
        Set<String> paths = new LinkedHashSet<>();
        for (AnnotationInstance annotation : index.getAnnotations(PATH)) {
            AnnotationTarget target = annotation.target();
            if (target.kind() == AnnotationTarget.Kind.CLASS) {
                ClassInfo type = target.asClass();
                if (!type.isInterface()) {
                    paths.add(SitemapEntryScanner.join(annotation.value().asString(), null));
                }
            } else if (target.kind() == AnnotationTarget.Kind.METHOD) {
                ClassInfo type = target.asMethod().declaringClass();
                AnnotationInstance classPath = type.declaredAnnotation(PATH);
                if (!type.isInterface() && classPath != null) {
                    paths.add(SitemapEntryScanner.join(classPath.value().asString(), annotation.value().asString()));
                }
            }
        }
        for (AnnotationInstance annotation : index.getAnnotations(ROUTE)) {
            addRoute(paths, annotation, annotation.target());
        }
        for (AnnotationInstance container : index.getAnnotations(ROUTES)) {
            for (AnnotationInstance annotation : container.value().asNestedArray()) {
                addRoute(paths, annotation, container.target());
            }
        }
        return paths;
    }

    private static void addRoute(Set<String> paths, AnnotationInstance route, AnnotationTarget target) {
        AnnotationValue path = route.value("path");
        if (target.kind() != AnnotationTarget.Kind.METHOD || path == null) {
            return;
        }
        ClassInfo type = target.asMethod().declaringClass();
        if (type.isInterface()) {
            return;
        }
        AnnotationInstance base = type.declaredAnnotation(ROUTE_BASE);
        AnnotationValue basePath = base == null ? null : base.value("path");
        paths.add(SitemapEntryScanner.join(basePath == null ? null : basePath.asString(), path.asString()));
    }

    /**
     * Tell whether a static file of the application or of a jar sits on a path.
     *
     * @param statics the static resources, absent when the application has none
     * @param path the path relative to {@code META-INF/resources}, with a leading slash
     * @return true when a static file sits on the path
     */
    static boolean ownedStatic(Optional<StaticResourcesBuildItem> statics, String path) {
        return statics.isPresent() && statics.get().getPaths().contains(path);
    }
}
