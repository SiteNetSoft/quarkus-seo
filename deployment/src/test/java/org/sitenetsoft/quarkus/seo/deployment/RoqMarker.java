package org.sitenetsoft.quarkus.seo.deployment;

import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

/**
 * Put an empty class with the name of a class of Roq's sitemap plugin into a test archive, so that the build sees the
 * plugin on the class path. The class is generated per archive, so other tests never see it.
 */
final class RoqMarker {

    static final String SITEMAP_KEYS = "io/quarkiverse/roq/plugin/sitemap/runtime/RoqSitemapKeys";

    private RoqMarker() {
    }

    static JavaArchive add(JavaArchive jar) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, SITEMAP_KEYS, null, "java/lang/Object", null);
        writer.visitEnd();
        return jar.add(new ByteArrayAsset(writer.toByteArray()), SITEMAP_KEYS + ".class");
    }
}
