package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class IndexableProfileTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(Res.class));

    @Test
    void notIndexableUnderTheTestProfile() {
        get("/t/indexable").then().statusCode(200).body(is("false"));
    }
}
