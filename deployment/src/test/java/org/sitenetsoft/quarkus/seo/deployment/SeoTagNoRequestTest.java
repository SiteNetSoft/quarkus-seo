package org.sitenetsoft.quarkus.seo.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusUnitTest;

class SeoTagNoRequestTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.title.template", "%s")
            .withApplicationRoot(jar -> jar.addClasses(StartupRenderer.class)
                    .addAsResource(new StringAsset(
                            "<head>{#seo title=\"About\" description=\"Tag\" /}</head><h1>{seo:title}</h1>"),
                            "templates/about.html"));

    static final AtomicReference<String> STARTUP_THREAD = new AtomicReference<>();
    static final AtomicReference<String> PLAIN_THREAD = new AtomicReference<>();
    static final AtomicReference<Throwable> FAILURE = new AtomicReference<>();

    @Test
    void rendersOutsideARequest() {
        assertThat(FAILURE.get()).isNull();
        assertThat(STARTUP_THREAD.get()).contains("<title>About</title>").doesNotContain("canonical");
        assertThat(PLAIN_THREAD.get()).contains("<title>About</title>").doesNotContain("canonical");
    }

    @Startup
    @Singleton
    public static class StartupRenderer {

        @Inject
        @Location("about.html")
        Template about;

        void render(@Observes StartupEvent event) throws InterruptedException {
            try {
                STARTUP_THREAD.set(about.render());
            } catch (RuntimeException e) {
                FAILURE.set(e);
            }
            Thread thread = new Thread(() -> {
                try {
                    PLAIN_THREAD.set(about.render());
                } catch (RuntimeException e) {
                    FAILURE.set(e);
                }
            });
            thread.start();
            thread.join();
        }
    }
}
