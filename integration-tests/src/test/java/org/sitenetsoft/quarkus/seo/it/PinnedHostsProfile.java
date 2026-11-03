package org.sitenetsoft.quarkus.seo.it;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class PinnedHostsProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.seo.site-url", "", "quarkus.seo.allowed-hosts", "a.example,b.example");
    }
}
