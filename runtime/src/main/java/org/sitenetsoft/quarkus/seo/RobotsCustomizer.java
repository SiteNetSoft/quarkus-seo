package org.sitenetsoft.quarkus.seo;

/**
 * Edit the robots.txt rules in Java after the configuration and the AI preset are applied.
 */
public interface RobotsCustomizer {

    /**
     * Change the rules before they are written.
     *
     * @param rules the rules to edit
     */
    void customize(RobotsRules rules);
}
