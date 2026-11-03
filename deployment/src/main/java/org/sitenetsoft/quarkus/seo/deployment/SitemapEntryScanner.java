package org.sitenetsoft.quarkus.seo.deployment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import org.sitenetsoft.quarkus.seo.ChangeFrequency;
import org.sitenetsoft.quarkus.seo.SitemapEntry;

/**
 * Find the JAX-RS methods annotated with {@code @SitemapEntry} and check their values.
 */
final class SitemapEntryScanner {

    private static final Logger LOG = Logger.getLogger(SitemapEntryScanner.class);
    private static final DotName SITEMAP_ENTRY = DotName.createSimple(SitemapEntry.class.getName());
    private static final DotName GET = DotName.createSimple("jakarta.ws.rs.GET");
    private static final DotName PATH = DotName.createSimple("jakarta.ws.rs.Path");
    private static final double UNSET_PRIORITY = -1;

    private SitemapEntryScanner() {
    }

    /**
     * Read every {@code @SitemapEntry} on a {@code GET} method of a class with a {@code @Path}.
     *
     * <p>
     * The scan ignores {@code @ApplicationPath} and {@code quarkus.rest.path}. It logs a warning for an annotated
     * method that is not a {@code GET} method or whose class has no {@code @Path}.
     *
     * @param index the index of the application classes
     * @return the entries sorted by path, one per path
     * @throws IllegalStateException when an entry has an unknown change frequency, a priority outside 0.0 to 1.0
     *         or a path with a template variable
     */
    static List<SitemapEntryBuildItem> scan(IndexView index) {
        Map<String, SitemapEntryBuildItem> entries = new LinkedHashMap<>();
        List<String> problems = new ArrayList<>();
        for (AnnotationInstance annotation : index.getAnnotations(SITEMAP_ENTRY)) {
            if (annotation.target().kind() != AnnotationTarget.Kind.METHOD) {
                continue;
            }
            MethodInfo method = annotation.target().asMethod();
            String name = method.declaringClass().name().toString() + "#" + method.name();
            AnnotationInstance classPath = method.declaringClass().declaredAnnotation(PATH);
            if (!method.hasDeclaredAnnotation(GET) || classPath == null) {
                LOG.warnf("@SitemapEntry on %s is ignored because the method is not a JAX-RS GET resource method",
                        name);
                continue;
            }
            AnnotationInstance methodPath = method.declaredAnnotation(PATH);
            String path = join(classPath.value().asString(), methodPath == null ? null : methodPath.value().asString());
            String changefreq = stringValue(annotation.value("changefreq"));
            double priority = annotation.value("priority") == null ? UNSET_PRIORITY
                    : annotation.value("priority").asDouble();
            int before = problems.size();
            if (path.indexOf('{') >= 0) {
                problems.add(name + " has the path " + path + " with a template variable");
            }
            if (!changefreq.isEmpty() && !isChangeFrequency(changefreq)) {
                problems.add(name + " has the unknown changefreq \"" + changefreq + "\"");
            }
            if (priority != UNSET_PRIORITY && !(priority >= 0.0 && priority <= 1.0)) {
                problems.add(name + " has the priority " + priority + " outside 0.0 to 1.0");
            }
            if (problems.size() == before) {
                entries.putIfAbsent(path, new SitemapEntryBuildItem(path, changefreq, priority));
            }
        }
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Invalid @SitemapEntry: " + String.join("; ", problems));
        }
        List<SitemapEntryBuildItem> sorted = new ArrayList<>(entries.values());
        sorted.sort(Comparator.comparing(SitemapEntryBuildItem::path));
        return sorted;
    }

    /**
     * Join a class path and a method path.
     *
     * @param classPath the value of the class {@code @Path}
     * @param methodPath the value of the method {@code @Path}, or null when the method has none
     * @return the path with one leading slash and no trailing slash, or {@code /} for the root
     */
    static String join(String classPath, String methodPath) {
        StringBuilder joined = new StringBuilder();
        append(joined, classPath);
        append(joined, methodPath);
        return joined.length() == 0 ? "/" : joined.toString();
    }

    private static void append(StringBuilder joined, String part) {
        if (part == null) {
            return;
        }
        int start = 0;
        int end = part.length();
        while (start < end && part.charAt(start) == '/') {
            start++;
        }
        while (end > start && part.charAt(end - 1) == '/') {
            end--;
        }
        if (start < end) {
            joined.append('/').append(part, start, end);
        }
    }

    private static String stringValue(AnnotationValue value) {
        return value == null ? "" : value.asString();
    }

    private static boolean isChangeFrequency(String value) {
        for (ChangeFrequency frequency : ChangeFrequency.values()) {
            if (frequency.name().toLowerCase(Locale.ROOT).equals(value)) {
                return true;
            }
        }
        return false;
    }
}
