package org.sitenetsoft.quarkus.seo.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class SeoStartupTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withEmptyApplication()
            .setLogRecordPredicate(r -> r.getMessage() != null && r.getMessage().contains("Installed features"))
            .assertLogRecords(records -> assertThat(records)
                    .anyMatch(r -> String.valueOf(Arrays.toString(r.getParameters())).contains("seo")));

    @Test
    void startsWithNoSeoConfigAndInstallsTheFeature() {
        // The assertion is in assertLogRecords. Startup must not fail with no quarkus.seo keys.
    }
}
