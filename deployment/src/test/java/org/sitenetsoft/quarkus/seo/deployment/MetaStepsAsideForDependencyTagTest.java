package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sitenetsoft.quarkus.seo.Seo;

import io.quarkus.arc.Arc;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.test.QuarkusUnitTest;

class MetaStepsAsideForDependencyTagTest {

    static final String KEY = "seo-test.capability-present";

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .addBuildChainCustomizer(probe())
            .withApplicationRoot(jar -> jar.addClasses(TagPage.class)
                    .addAsResource(new StringAsset(TagPage.TEMPLATE), "templates/tag-page.html"))
            .withAdditionalDependency(
                    jar -> jar.addAsResource(new StringAsset(TagPage.APP_TAG), "templates/tags/seo.html"));

    @Test
    void theApplicationTagRenders() {
        String body = given().get("/tag-page").then().statusCode(200).extract().asString();
        assertThat(body).isEqualTo("<head>APP TAG Home</head>");
    }

    @Test
    void registersNoSeoBean() {
        assertThat(Arc.container().instance(Seo.class).isAvailable()).isFalse();
    }

    @Test
    void capabilityIsAbsent() {
        assertThat(ConfigProvider.getConfig().getValue(KEY, Boolean.class)).isFalse();
    }

    static Consumer<BuildChainBuilder> probe() {
        return builder -> builder.addBuildStep(context -> {
            boolean present = context.consume(Capabilities.class).isPresent("org.sitenetsoft.seo");
            context.produce(new RunTimeConfigurationDefaultBuildItem(KEY, Boolean.toString(present)));
        }).consumes(Capabilities.class).produces(RunTimeConfigurationDefaultBuildItem.class).build();
    }
}
