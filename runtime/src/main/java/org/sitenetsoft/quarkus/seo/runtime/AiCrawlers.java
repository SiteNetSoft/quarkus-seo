package org.sitenetsoft.quarkus.seo.runtime;

import java.util.List;

/**
 * List the crawler tokens of the AI presets.
 */
final class AiCrawlers {

    /** Crawlers that collect content to train models. */
    static final List<String> TRAINING = List.of("GPTBot", "ClaudeBot", "Google-Extended", "Applebot-Extended",
            "CCBot", "Bytespider", "Meta-ExternalAgent", "cohere-ai", "Amazonbot");

    /** Crawlers that serve search and user requests. */
    static final List<String> SEARCH = List.of("OAI-SearchBot", "ChatGPT-User", "Claude-SearchBot", "Claude-User",
            "PerplexityBot", "Perplexity-User");

    private AiCrawlers() {
    }
}
