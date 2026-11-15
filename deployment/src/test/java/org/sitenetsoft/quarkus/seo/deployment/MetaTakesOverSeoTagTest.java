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

class MetaTakesOverSeoTagTest {

    static final String KEY = "seo-test.capability-present";

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.meta.on-conflict", "take-over")
            .overrideConfigKey("quarkus.seo.title.template", "%s")
            .addBuildChainCustomizer(probe())
            .withApplicationRoot(jar -> jar.addClasses(TagPage.class)
                    .addAsResource(new StringAsset(TagPage.TEMPLATE), "templates/tag-page.html"))
            .withAdditionalDependency(
                    jar -> jar.addAsResource(new StringAsset(TagPage.APP_TAG), "templates/tags/seo.html"));

    @Test
    void ourTagRenders() {
        String body = given().get("/tag-page").then().statusCode(200).extract().asString();
        assertThat(body).contains("<title>Home</title>").doesNotContain("APP TAG");
    }

    @Test
    void registersTheSeoBean() {
        assertThat(Arc.container().instance(Seo.class).isAvailable()).isTrue();
    }

    @Test
    void capabilityIsPresent() {
        assertThat(ConfigProvider.getConfig().getValue(KEY, Boolean.class)).isTrue();
    }

    static Consumer<BuildChainBuilder> probe() {
        return builder -> builder.addBuildStep(context -> {
            boolean present = context.consume(Capabilities.class).isPresent("org.sitenetsoft.seo");
            context.produce(new RunTimeConfigurationDefaultBuildItem(KEY, Boolean.toString(present)));
        }).consumes(Capabilities.class).produces(RunTimeConfigurationDefaultBuildItem.class).build();
    }
}
