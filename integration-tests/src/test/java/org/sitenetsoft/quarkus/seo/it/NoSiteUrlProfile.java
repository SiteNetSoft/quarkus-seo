package org.sitenetsoft.quarkus.seo.it;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class NoSiteUrlProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.seo.site-url", "");
    }
}
