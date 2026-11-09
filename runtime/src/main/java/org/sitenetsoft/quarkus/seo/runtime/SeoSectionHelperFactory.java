package org.sitenetsoft.quarkus.seo.runtime;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.ClientProxy;
import io.quarkus.qute.EngineConfiguration;
import io.quarkus.qute.Expression;
import io.quarkus.qute.ResultNode;
import io.quarkus.qute.Scope;
import io.quarkus.qute.SectionHelper;
import io.quarkus.qute.SectionHelperFactory;
import io.quarkus.qute.SingleResultNode;
import org.sitenetsoft.quarkus.seo.BaseUrlResolver;
import org.sitenetsoft.quarkus.seo.SeoHeadRenderer;

/**
 * Register the {@code seo} section tag that renders the head tags of the current page.
 */
@EngineConfiguration
public class SeoSectionHelperFactory implements SectionHelperFactory<SeoSectionHelperFactory.SeoSectionHelper> {

    /**
     * Name the tag parameters in the order of the documentation.
     */
    static final List<String> PARAMETERS = List.of("title", "absoluteTitle", "description", "canonical", "type",
            "image", "imageWidth", "imageHeight", "imageAlt", "locale", "noindex", "nofollow", "noimageindex",
            "twitterCard", "author", "section");

    @Override
    public List<String> getDefaultAliases() {
        return List.of("seo");
    }

    @Override
    public Scope initializeBlock(Scope previousScope, BlockInfo block) {
        for (Map.Entry<String, String> parameter : block.getParameters().entrySet()) {
            String name = parameter.getKey();
            if (!PARAMETERS.contains(name)) {
                String shown = isIndex(name) ? parameter.getValue() : name;
                throw block.error("The seo tag has no parameter [{name}]. Known parameters: {known}")
                        .origin(block.getOrigin())
                        .argument("name", shown)
                        .argument("known", PARAMETERS)
                        .build();
            }
            block.addExpression(name, parameter.getValue());
        }
        return previousScope;
    }

    @Override
    public SeoSectionHelper initialize(SectionInitContext context) {
        Map<String, Expression> expressions = new HashMap<>();
        for (String name : context.getParameters().keySet()) {
            Expression expression = context.getExpression(name);
            if (expression != null) {
                expressions.put(name, expression);
            }
        }
        return new SeoSectionHelper(expressions);
    }

    /**
     * Build the tag layer from the evaluated parameters.
     *
     * @param values the evaluated parameters by name
     * @return the tag layer
     */
    static SeoValues tagValues(Map<String, Object> values) {
        return new SeoValues()
                .title(text(values.get("title")))
                .absoluteTitle(text(values.get("absoluteTitle")))
                .description(text(values.get("description")))
                .canonical(text(values.get("canonical")))
                .type(text(values.get("type")))
                .image(text(values.get("image")))
                .imageWidth(number(values.get("imageWidth")))
                .imageHeight(number(values.get("imageHeight")))
                .imageAlt(text(values.get("imageAlt")))
                .locale(locale(values.get("locale")))
                .noindex(flag(values.get("noindex")))
                .nofollow(flag(values.get("nofollow")))
                .noimageindex(flag(values.get("noimageindex")))
                .twitterCard(text(values.get("twitterCard")))
                .author(text(values.get("author")))
                .section(text(values.get("section")));
    }

    /**
     * Render the head tags for a tag layer when no request context is active.
     *
     * <p>
     * The tag layer is resolved alone and nothing is stored.
     *
     * @param tag the tag layer
     * @return the head tags
     */
    static String renderAlone(SeoValues tag) {
        ArcContainer container = Arc.container();
        SeoHeadRenderer renderer = container.instance(SeoHeadRenderer.class).get();
        SeoResolver resolver = container.instance(SeoResolver.class).get();
        return renderer.render(resolver.resolve(tag, new SeoValues(), Optional.empty()));
    }

    private static boolean isIndex(String name) {
        for (int i = 0; i < name.length(); i++) {
            if (!Character.isDigit(name.charAt(i))) {
                return false;
            }
        }
        return !name.isEmpty();
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }

    private static Boolean flag(Object value) {
        if (value instanceof Boolean bool) {
            return bool ? Boolean.TRUE : null;
        }
        return value != null && "true".equalsIgnoreCase(value.toString().trim()) ? Boolean.TRUE : null;
    }

    private static Integer number(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Locale locale(Object value) {
        if (value instanceof Locale locale) {
            return locale;
        }
        String tag = text(value);
        if (tag == null || tag.isBlank()) {
            return null;
        }
        Locale locale = Locale.forLanguageTag(tag.trim());
        return locale.getLanguage().isEmpty() ? null : locale;
    }

    /**
     * Evaluate the tag parameters and write the head tags.
     */
    public static class SeoSectionHelper implements SectionHelper {

        private final Map<String, Expression> expressions;

        /**
         * Create the helper.
         *
         * @param expressions the parameter expressions by name
         */
        SeoSectionHelper(Map<String, Expression> expressions) {
            this.expressions = expressions;
        }

        /**
         * Evaluate the parameters and render the head tags.
         *
         * <p>
         * The method reads the request state before it evaluates the parameters. A parameter can complete on another
         * thread, where no request context is active. The captured layers and request still apply there.
         *
         * @param context the resolution context
         * @return the result node with the head tags
         */
        @Override
        public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
            ArcContainer container = Arc.container();
            if (!container.requestContext().isActive()) {
                return context.evaluate(expressions)
                        .thenApply(values -> new SingleResultNode(renderAlone(tagValues(values))));
            }
            SeoLayers layers = ClientProxy.unwrap(container.instance(SeoLayers.class).get());
            Optional<RequestInfo> request = RequestInfo.current(container.instance(BaseUrlResolver.class).get(),
                    container.instance(SeoSettings.class).get());
            SeoHeadRenderer renderer = container.instance(SeoHeadRenderer.class).get();
            SeoResolver resolver = container.instance(SeoResolver.class).get();
            return context.evaluate(expressions).thenApply(values -> {
                layers.tag(tagValues(values));
                return new SingleResultNode(renderer.render(resolver.resolve(layers.tag(), layers.java(), request)));
            });
        }
    }
}
