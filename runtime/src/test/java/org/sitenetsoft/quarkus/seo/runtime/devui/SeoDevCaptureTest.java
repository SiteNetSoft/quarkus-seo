package org.sitenetsoft.quarkus.seo.runtime.devui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.sitenetsoft.quarkus.seo.ResolvedSeo;

class SeoDevCaptureTest {

    private static ResolvedSeo seo() {
        return new ResolvedSeo("Title", Optional.of("A description."), List.of(), Optional.empty(), List.of(),
                "website", Optional.of("Title"), Optional.empty(), "en_US", List.of(), Optional.empty(),
                Optional.empty(), "summary", Optional.empty(), Optional.empty(), List.of(), Optional.empty(),
                List.of(), "", Optional.empty());
    }

    @Test
    void browsedPathStaysLoggedAfterManyExpects() {
        SeoDevCapture capture = new SeoDevCapture();
        capture.record("/a", seo());
        for (int i = 0; i < 1500; i++) {
            capture.expect("/p" + i);
        }
        assertThat(capture.logged("/a")).isTrue();
    }

    @Test
    void expectedPathDoesNotLogLater() {
        SeoDevCapture capture = new SeoDevCapture();
        capture.expect("/b");
        capture.record("/b", seo());
        assertThat(capture.logged("/b")).isTrue();
        assertThat(capture.last("/b")).isPresent();
    }

    @Test
    void capturedHeadsStayBounded() {
        SeoDevCapture capture = new SeoDevCapture();
        for (int i = 0; i <= 1000; i++) {
            capture.record("/p" + i, seo());
        }
        assertThat(capture.last("/p0")).isEmpty();
        assertThat(capture.last("/p1000")).isPresent();
    }
}
