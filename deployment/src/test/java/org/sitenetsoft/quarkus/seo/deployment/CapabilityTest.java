package org.sitenetsoft.quarkus.seo.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.test.QuarkusUnitTest;

class CapabilityTest {

    static final String KEY = "seo-test.capability-present";

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .addBuildChainCustomizer(probe());

    @Test
    void capabilityIsPresentByDefault() {
        assertThat(ConfigProvider.getConfig().getValue(KEY, Boolean.class)).isTrue();
    }

    static Consumer<BuildChainBuilder> probe() {
        return builder -> builder.addBuildStep(context -> {
            boolean present = context.consume(Capabilities.class).isPresent("org.sitenetsoft.seo");
            context.produce(new RunTimeConfigurationDefaultBuildItem(KEY, Boolean.toString(present)));
        }).consumes(Capabilities.class).produces(RunTimeConfigurationDefaultBuildItem.class).build();
    }
}
