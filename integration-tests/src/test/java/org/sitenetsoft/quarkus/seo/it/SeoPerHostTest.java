package org.sitenetsoft.quarkus.seo.it;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(PinnedHostsProfile.class)
class SeoPerHostTest {

    @Test
    void cacheIsPerHost() {
        String a = sitemapFor("a.example");
        String b = sitemapFor("b.example");
        assertThat(a).contains("<loc>http://a.example/blog/first</loc>").doesNotContain("b.example");
        assertThat(b).contains("<loc>http://b.example/blog/first</loc>").doesNotContain("a.example");
        int before = calls();
        assertThat(sitemapFor("a.example")).isEqualTo(a);
        assertThat(calls()).isEqualTo(before);
    }

    private static int calls() {
        return Integer.parseInt(given().get("/contributor-calls").then().statusCode(200).extract().asString());
    }

    private static String sitemapFor(String host) {
        return given().header("Host", host).get("/sitemap.xml").then().statusCode(200).extract().asString();
    }
}
