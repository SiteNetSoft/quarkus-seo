package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class IndexableOverrideTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(Res.class)
                    .addAsResource(new StringAsset("quarkus.seo.indexable=true\n"), "application.properties"));

    @Test
    void indexableWhenSet() {
        get("/t/indexable").then().statusCode(200).body(is("true"));
    }
}
