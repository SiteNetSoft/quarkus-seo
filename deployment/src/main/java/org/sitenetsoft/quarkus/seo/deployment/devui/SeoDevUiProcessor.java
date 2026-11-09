package org.sitenetsoft.quarkus.seo.deployment.devui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import org.sitenetsoft.quarkus.seo.deployment.SeoOutputsBuildItem;
import org.sitenetsoft.quarkus.seo.runtime.SeoBuildConfig;
import org.sitenetsoft.quarkus.seo.runtime.SeoPaths;
import org.sitenetsoft.quarkus.seo.runtime.SeoRecorder;
import org.sitenetsoft.quarkus.seo.runtime.SeoSettings;
import org.sitenetsoft.quarkus.seo.runtime.devui.SecurityTxtServed;
import org.sitenetsoft.quarkus.seo.runtime.devui.SeoDevCapture;
import org.sitenetsoft.quarkus.seo.runtime.devui.SeoJsonRpcService;

/**
 * Register the dev mode parts of the extension.
 */
@BuildSteps(onlyIf = IsLocalDevelopment.class)
public class SeoDevUiProcessor {
    private static final String EXTENSION = "extension";
    private static final String APPLICATION = "application";
    private static final String OFF = "off";

    /**
     * Register the bean that records the resolved heads.
     *
     * @param outputs the outputs that the extension registers
     * @return the bean, or null when the meta feature is switched off
     */
    @BuildStep
    AdditionalBeanBuildItem captureBean(SeoOutputsBuildItem outputs) {
        if (!outputs.meta()) {
            return null;
        }
        return AdditionalBeanBuildItem.builder().addBeanClasses(SeoDevCapture.class).setUnremovable().build();
    }

    /**
     * Log the security.txt content problems at start.
     *
     * @param recorder the recorder
     * @param outputs the outputs that the extension registers
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void securityTxtWarnings(SeoRecorder recorder, SeoOutputsBuildItem outputs) {
        if (!outputs.securityTxt()) {
            return;
        }
        recorder.logSecurityTxtWarnings();
    }

    /**
     * Record whether the extension serves security.txt as a bean for the Dev UI service.
     *
     * @param recorder the recorder
     * @param config the build-time configuration
     * @param outputs the outputs that the extension registers
     * @return the synthetic bean, or null when the extension is switched off
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    SyntheticBeanBuildItem securityTxtServed(SeoRecorder recorder, SeoBuildConfig config, SeoOutputsBuildItem outputs) {
        if (!config.enabled()) {
            return null;
        }
        return SyntheticBeanBuildItem.configure(SecurityTxtServed.class)
                .scope(Singleton.class)
                .unremovable()
                .supplier(recorder.securityTxtServed(outputs.securityTxt()))
                .done();
    }

    /**
     * Add the SEO card with the overview page and the page check page.
     *
     * <p>
     * The build-time data {@code outputs} lists each output with its path and with who serves it.
     *
     * @param config the build-time configuration
     * @param outputs the outputs that the extension registers
     * @return the card, or null when the extension is switched off
     */
    @BuildStep
    CardPageBuildItem card(SeoBuildConfig config, SeoOutputsBuildItem outputs) {
        if (!config.enabled()) {
            return null;
        }
        CardPageBuildItem card = new CardPageBuildItem();
        card.addPage(Page.webComponentPageBuilder()
                .title("Overview")
                .componentLink("qwc-seo-overview.js")
                .icon("font-awesome-solid:magnifying-glass"));
        card.addPage(Page.webComponentPageBuilder()
                .title("Page check")
                .componentLink("qwc-seo-page-check.js")
                .icon("font-awesome-solid:list-check"));
        card.addBuildTimeData("outputs", outputRows(config, outputs));
        return card;
    }

    /**
     * Register the bean that answers the JSON-RPC calls of the SEO pages.
     *
     * @param config the build-time configuration
     * @return the bean, or null when the extension is switched off
     */
    @BuildStep
    AdditionalBeanBuildItem jsonRpcBean(SeoBuildConfig config) {
        if (!config.enabled()) {
            return null;
        }
        return AdditionalBeanBuildItem.builder().addBeanClasses(SeoJsonRpcService.class).setUnremovable().build();
    }

    /**
     * Register the JSON-RPC service of the SEO pages.
     *
     * <p>
     * The service carries no execution model annotation, so the build item is produced in dev mode only.
     *
     * @param config the build-time configuration
     * @return the JSON-RPC provider, or null when the extension is switched off
     */
    @BuildStep
    JsonRPCProvidersBuildItem jsonRpcService(SeoBuildConfig config) {
        if (!config.enabled()) {
            return null;
        }
        return new JsonRPCProvidersBuildItem(SeoJsonRpcService.class);
    }

    /**
     * List the outputs with their path and with who serves each one.
     *
     * @param config the build-time configuration
     * @param outputs the outputs that the extension registers
     * @return one row per output, with the keys {@code name}, {@code path} and {@code servedBy}
     */
    private static List<Map<String, String>> outputRows(SeoBuildConfig config, SeoOutputsBuildItem outputs) {
        String rootPath = SeoSettings.normalizeRootPath(ConfigProvider.getConfig()
                .getOptionalValue("quarkus.http.root-path", String.class).orElse("/"));
        String sitemapPath = SeoPaths.normalize(config.sitemap().path());
        String manifestPath = SeoPaths.normalize(config.manifest().path());
        String indexNowPath = config.indexNow().key().isPresent() ? "/" + config.indexNow().key().get() + ".txt" : "";
        List<Map<String, String>> rows = new ArrayList<>();
        rows.add(row("meta", "", outputs.meta() ? EXTENSION : OFF));
        rows.add(row("sitemap", rootPath + sitemapPath, servedBy(outputs.sitemap(), outputs, sitemapPath)));
        rows.add(row("robots.txt", hostRoot(rootPath, config.robots().serveUnderRootPath()) + SeoPaths.ROBOTS_TXT,
                servedBy(outputs.robots(), outputs, SeoPaths.ROBOTS_TXT)));
        rows.add(row("security.txt",
                hostRoot(rootPath, config.securityTxt().serveUnderRootPath()) + SeoPaths.SECURITY_TXT,
                servedBy(outputs.securityTxt(), outputs, SeoPaths.SECURITY_TXT)));
        rows.add(row("manifest", rootPath + manifestPath, servedBy(outputs.manifest(), outputs, manifestPath)));
        rows.add(row("IndexNow key",
                indexNowPath.isEmpty() ? "" : hostRoot(rootPath, config.indexNow().serveUnderRootPath()) + indexNowPath,
                indexNowPath.isEmpty() ? OFF : servedBy(outputs.indexNow(), outputs, indexNowPath)));
        rows.add(row("llms.txt", hostRoot(rootPath, config.llmsTxt().serveUnderRootPath()) + SeoPaths.LLMS_TXT,
                servedBy(outputs.llmsTxt(), outputs, SeoPaths.LLMS_TXT)));
        return rows;
    }

    private static String servedBy(boolean served, SeoOutputsBuildItem outputs, String path) {
        if (served) {
            return EXTENSION;
        }
        return outputs.ownedByApplication().contains(path) ? APPLICATION : OFF;
    }

    private static String hostRoot(String rootPath, boolean underRootPath) {
        return underRootPath ? rootPath : "";
    }

    private static Map<String, String> row(String name, String path, String servedBy) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("name", name);
        row.put("path", path);
        row.put("servedBy", servedBy);
        return row;
    }
}
