package org.sitenetsoft.quarkus.seo.deployment;

import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;

@MessageBundle
public interface AppMessages {

    @Message("Bundle about")
    String about_title();
}
