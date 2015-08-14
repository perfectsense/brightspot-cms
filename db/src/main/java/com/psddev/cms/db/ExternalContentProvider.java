package com.psddev.cms.db;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.psddev.dari.util.HtmlWriter;
import com.psddev.dari.util.ObjectUtils;

/**
 * {@link ExternalContent} from providers that doesn't support the oEmbed
 * specification.
 */
public interface ExternalContentProvider {

    /**
     * @return {@code null} if this provider doesn't support embedding
     * the given {@code content}.
     */
    public Map<String, Object> createResponse(ExternalContent content);

    /**
     * Skeletal implementation of {@link ExternalContentProvider} optimized
     * for returning {@code rich} oEmbed type.
     */
    public abstract static class RichExternalContentProvider implements ExternalContentProvider {

        private ExternalContent content;

        public ExternalContent getContent() {
            return content;
        }

        /**
         * Returns the regular expression pattern against the URL that's used
         * to determined whether this provider supports embedded the external
         * content.
         *
         * <p>The resulting matcher is passed to {@link #updateHtml}.</p>
         *
         * @return Never {@code null}.
         */
        protected abstract Pattern getUrlPattern();

        /**
         * Updates the HTML that's returned in the response.
         *
         * @param urlMatcher Never {@code null}.
         * @param html Never {@code null}.
         */
        protected abstract void updateHtml(Matcher urlMatcher, HtmlWriter html) throws IOException;

        /**
         * Updates the given {@code response}.
         *
         * @param response Never {@code null}.
         */
        protected void updateResponse(Map<String, Object> response) {
        }

        @Override
        public final Map<String, Object> createResponse(ExternalContent content) {
            this.content = content;
            Matcher urlMatcher = getUrlPattern().matcher(content.getUrl());

            if (!urlMatcher.matches()) {
                return null;
            }

            StringWriter string = new StringWriter();
            HtmlWriter html = new HtmlWriter(string);

            try {
                try {
                    updateHtml(urlMatcher, html);

                } finally {
                    html.close();
                }

            } catch (IOException error) {
                throw new IllegalStateException(error);
            }

            Map<String, Object> response = new HashMap<String, Object>();

            response.put("type", "rich");
            response.put("html", string.toString());
            updateResponse(response);

            return response;
        }
    }

    public static class Instagram extends ExternalContentProvider.RichExternalContentProvider {

        private static final Pattern URL_PATTERN = Pattern.compile("(?i)https?://instagr(?:\\.am|am\\.com)/p/([^/]+).*");

        @Override
        protected Pattern getUrlPattern() {
            return URL_PATTERN;
        }

        @Override
        protected void updateHtml(Matcher urlMatcher, HtmlWriter html) throws IOException {
            html.writeStart("iframe",
                    "src", "//instagram.com/p/" + urlMatcher.group(1) + "/embed/",
                    "width", 640,
                    "height", 640,
                    "frameborder", 0,
                    "scrolling", "no");
            html.writeEnd();
        }

        @Override
        protected void updateResponse(Map<String, Object> response) {
            response.put("width", 640);
            response.put("height", 640);
        }
    }

    public static class Vine extends RichExternalContentProvider {

        private static final Pattern URL_PATTERN = Pattern.compile("\"(?i)https?:(//vine.co/[^/]+/[^/]+).*\"");

        @Override
        protected Pattern getUrlPattern() {
            return URL_PATTERN;
        }

        @Override
        protected void updateHtml(Matcher urlMatcher, HtmlWriter html) throws IOException {
            html.writeStart("iframe",
                    "src", "https://vine.co/v/" + urlMatcher.group(1) + "/embed/simple",
                    "width", 600,
                    "height", 600,
                    "frameborder", 0,
                    "scrolling", "no");
            html.writeEnd();
        }

        @Override
        protected void updateResponse(Map<String, Object> response) {
            response.put("width", 600);
            response.put("height", 600);
        }
    }

    public static class Storify extends RichExternalContentProvider {

        private static final Pattern URL_PATTERN = Pattern.compile("(?i)https?:(//storify.com/[^/]+/[^/]+).*");

        @Override
        protected Pattern getUrlPattern() {
            return URL_PATTERN;
        }

        @Override
        protected void updateHtml(Matcher urlMatcher, HtmlWriter html) throws IOException {
            html.writeStart("script",
                    "type", "text/javascript",
                    "src", urlMatcher.group(1) + ".js");
            html.writeEnd();
        }
    }

    public static class Pinterest extends RichExternalContentProvider {

        private static final Pattern URL_PATTERN = Pattern.compile("(?i)https?://(?:www\\.)?pinterest.com/([^/]+)(/[^/]+)?.*");

        @Override
        protected Pattern getUrlPattern() {
            return URL_PATTERN;
        }

        @Override
        protected void updateHtml(Matcher urlMatcher, HtmlWriter html) throws IOException {
            String pinDo;

            if ("pin".equals(urlMatcher.group(1))) {
                pinDo = "embedPin";

            } else if (ObjectUtils.isBlank(urlMatcher.group(2))) {
                pinDo = "embedUser";

            } else {
                pinDo = "embedBoard";
            }

            html.writeStart("a",
                    "data-pin-do", pinDo,
                    "href", urlMatcher.group(0));
            html.writeEnd();

            html.writeStart("script", "type", "text/javascript");
            html.writeRaw("(function() {");
            html.writeRaw("var w = window, d, f, p;");
            html.writeRaw("if (w.BRIGHTSPOT_PINTEREST) { return; }");
            html.writeRaw("d = w.document, f = d.getElementsByTagName('SCRIPT')[0], p = d.createElement('SCRIPT');");
            html.writeRaw("p.type = 'text/javascript';");
            html.writeRaw("p.async = true;");
            html.writeRaw("p.src = '//assets.pinterest.com/js/pinit.js';");
            html.writeRaw("f.parentNode.insertBefore(p, f);");
            html.writeRaw("w.BRIGHTSPOT_PINTEREST = true;");
            html.writeRaw("})()");
            html.writeEnd();
        }
    }

    public static class Facebook extends RichExternalContentProvider {

        private static final Pattern URL_PATTERN = Pattern.compile("(?i)https?://(?:www\\.)?facebook.com/([^/]+)(/[^/]+)?.*");

        @Override
        protected Pattern getUrlPattern() {
            return URL_PATTERN;
        }

        @Override
        protected void updateHtml(Matcher matcher, HtmlWriter page) throws IOException {
            page.writeStart("script");
            page.writeRaw("(function(d, s, id) {\n"
                    + "  var js, fjs = d.getElementsByTagName(s)[0];\n"
                    + "  if (d.getElementById(id)) return;\n"
                    + "  js = d.createElement(s); js.id = id;\n"
                    + "  js.src = \"//connect.facebook.net/en_US/sdk.js#xfbml=1&version=v2.2\";\n"
                    + "  fjs.parentNode.insertBefore(js, fjs);\n"
                    + "}(document, 'script', 'facebook-jssdk'));");
            page.writeEnd();

            page.writeStart("div",
                    "class", "fb-post",
                    "data-href", matcher.group(0));
            page.writeEnd();
        }
    }
}
