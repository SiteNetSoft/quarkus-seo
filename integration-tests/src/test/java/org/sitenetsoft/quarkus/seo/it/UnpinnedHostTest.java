package org.sitenetsoft.quarkus.seo.it;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(NoSiteUrlProfile.class)
class UnpinnedHostTest {

    @Test
    void sitemapIsComputedOnEachRequestWithoutAPinnedHost() {
        int before = calls();
        given().header("Host", "a.example").get("/sitemap.xml").then().statusCode(200);
        given().header("Host", "a.example").get("/sitemap.xml").then().statusCode(200);
        assertThat(calls()).isEqualTo(before + 2);
    }

    private static int calls() {
        return Integer.parseInt(given().get("/contributor-calls").then().statusCode(200).extract().asString());
    }
}
