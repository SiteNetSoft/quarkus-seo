package org.sitenetsoft.quarkus.seo.deployment.devui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;

class SecurityTxtWithContactDevTest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setLogRecordPredicate(record -> record.getLoggerName() != null
                    && record.getLoggerName().startsWith("org.sitenetsoft.quarkus.seo")
                    && record.getLevel().intValue() >= Level.WARNING.intValue())
            .withApplicationRoot(jar -> jar.addAsResource(new StringAsset(
                    "quarkus.seo.security-txt.enabled=true\nquarkus.seo.security-txt.contact=mailto:sec@ex.com\n"),
                    "application.properties"));

    SecurityTxtWithContactDevTest() {
        super("quarkus-seo");
    }

    @Test
    void warnsAboutTheMissingExpiry() throws Exception {
        JsonNode result = executeJsonRPCMethod("getOverview");

        assertThat(result.get("securityTxtWarnings").get(0).get("code").asText())
                .isEqualTo("security-txt-expires-missing");
        assertThat(TEST.getLogRecords())
                .anyMatch(record -> record.getMessage().contains("[security-txt-expires-missing]"));
    }
}
