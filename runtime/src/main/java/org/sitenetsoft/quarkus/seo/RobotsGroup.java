package org.sitenetsoft.quarkus.seo;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * Hold one group of robots.txt rules for a list of crawlers.
 */
public final class RobotsGroup {

    private final List<String> userAgents;
    private final List<String> allow = new ArrayList<>();
    private final List<String> disallow = new ArrayList<>();
    private OptionalInt crawlDelay = OptionalInt.empty();

    /**
     * Create a group.
     *
     * @param userAgents the crawler tokens the group applies to
     */
    public RobotsGroup(List<String> userAgents) {
        this.userAgents = new ArrayList<>(userAgents);
    }

    /**
     * Return the crawler tokens.
     *
     * @return the mutable list of user agents
     */
    public List<String> userAgents() {
        return userAgents;
    }

    /**
     * Return the allowed paths.
     *
     * @return the mutable list of paths
     */
    public List<String> allow() {
        return allow;
    }

    /**
     * Return the disallowed paths.
     *
     * @return the mutable list of paths
     */
    public List<String> disallow() {
        return disallow;
    }

    /**
     * Return the crawl delay.
     *
     * @return the delay in seconds, or empty when none is set
     */
    public OptionalInt crawlDelay() {
        return crawlDelay;
    }

    /**
     * Add allowed paths.
     *
     * @param paths the paths to add
     * @return this group
     */
    public RobotsGroup allow(String... paths) {
        allow.addAll(List.of(paths));
        return this;
    }

    /**
     * Add disallowed paths.
     *
     * @param paths the paths to add
     * @return this group
     */
    public RobotsGroup disallow(String... paths) {
        disallow.addAll(List.of(paths));
        return this;
    }

    /**
     * Set the crawl delay.
     *
     * @param seconds the delay in seconds
     * @return this group
     */
    public RobotsGroup crawlDelay(int seconds) {
        this.crawlDelay = OptionalInt.of(seconds);
        return this;
    }
}
