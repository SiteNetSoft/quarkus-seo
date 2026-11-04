package org.sitenetsoft.quarkus.seo.deployment.devui;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import org.sitenetsoft.quarkus.seo.deployment.SeoOutputsBuildItem;
import org.sitenetsoft.quarkus.seo.runtime.SeoRecorder;
import org.sitenetsoft.quarkus.seo.runtime.devui.SeoDevCapture;

/**
 * Register the dev mode parts of the extension.
 */
@BuildSteps(onlyIf = IsLocalDevelopment.class)
public class SeoDevUiProcessor {

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
}
