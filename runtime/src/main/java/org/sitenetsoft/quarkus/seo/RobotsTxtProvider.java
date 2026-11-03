package org.sitenetsoft.quarkus.seo;

/**
 * Write the robots.txt file. A bean of this type replaces the default one.
 */
public interface RobotsTxtProvider {

    /**
     * Write the file.
     *
     * @param baseUrl the base URL of the site, including the root path
     * @return the text of robots.txt
     */
    String robotsTxt(String baseUrl);
}
