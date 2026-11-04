package org.sitenetsoft.quarkus.seo.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class SecurityTxtWriterTest {

    private static final String CANON = "https://ex.com/.well-known/security.txt";

    private static SeoConfig.SecurityTxt cfg(TestConfig test) {
        return test.config().securityTxt();
    }

    private static String write(TestConfig test) {
        return SecurityTxtWriter.write(cfg(test), CANON).orElseThrow();
    }

    @Test
    void fullFileInRfcOrder() {
        TestConfig test = new TestConfig()
                .with("securityTxt.contact", List.of("mailto:sec@ex.com", "https://ex.com/report"))
                .with("securityTxt.expires", "2027-01-01T00:00:00Z")
                .with("securityTxt.encryption", "https://ex.com/pgp.txt")
                .with("securityTxt.policy", "https://ex.com/policy")
                .with("securityTxt.preferredLanguages", List.of("en", "fr"))
                .with("securityTxt.hiring", "https://ex.com/jobs");
        assertThat(SecurityTxtWriter.write(cfg(test), CANON)).contains(
                "Contact: mailto:sec@ex.com\nContact: https://ex.com/report\nExpires: 2027-01-01T00:00:00Z\n"
                        + "Encryption: https://ex.com/pgp.txt\nPolicy: https://ex.com/policy\n"
                        + "Preferred-Languages: en, fr\nHiring: https://ex.com/jobs\n"
                        + "Canonical: https://ex.com/.well-known/security.txt\n");
    }

    @Test
    void bareEmailGetsMailto() {
        assertThat(write(new TestConfig().with("securityTxt.contact", List.of("sec@ex.com"))))
                .contains("Contact: mailto:sec@ex.com\n");
    }

    @Test
    void dateOnlyExpiresIsStartOfDayUtc() {
        assertThat(write(new TestConfig().with("securityTxt.contact", List.of("sec@ex.com"))
                .with("securityTxt.expires", "2027-01-01"))).contains("Expires: 2027-01-01T00:00:00Z\n");
    }

    @Test
    void offsetIsNormalizedToUtc() {
        assertThat(write(new TestConfig().with("securityTxt.contact", List.of("sec@ex.com"))
                .with("securityTxt.expires", "2027-01-01T02:00:00+02:00"))).contains("Expires: 2027-01-01T00:00:00Z\n");
    }

    @Test
    void invalidExpiresLeavesTheLineOut() {
        assertThat(write(new TestConfig().with("securityTxt.contact", List.of("sec@ex.com"))
                .with("securityTxt.expires", "next-year"))).doesNotContain("Expires:");
        assertThat(SecurityTxtWriter.expires("next-year")).isEmpty();
        assertThat(SecurityTxtWriter.expires("  ")).isEmpty();
    }

    @Test
    void noContactGivesEmpty() {
        assertThat(SecurityTxtWriter.write(cfg(new TestConfig()), CANON)).isEmpty();
        assertThat(SecurityTxtWriter.write(cfg(new TestConfig().with("securityTxt.contact", List.of(" "))), CANON))
                .isEmpty();
    }

    @Test
    void newlineInAValueIsDropped() {
        String text = write(new TestConfig().with("securityTxt.contact", List.of("mailto:a@ex.com\nPolicy: x")));
        assertThat(text).contains("Contact: mailto:a@ex.com\n").doesNotContain("Policy:");
        assertThat(text.split("Contact:", -1)).hasSize(2);
    }
}
