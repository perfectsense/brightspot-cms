<%@ page session="false" import="

com.psddev.cms.tool.ToolPageContext,

com.psddev.dari.db.ValidationException,

com.psddev.dari.util.ObjectUtils,

com.psddev.dari.util.Settings,

java.util.ArrayList,
java.util.Collections,
java.util.List,

javax.servlet.ServletException
" %><%

// --- Presentation ---

ToolPageContext wp = new ToolPageContext(pageContext);
List<Throwable> errors = wp.getErrors();
if (ObjectUtils.isBlank(errors)) {
    return;
}

wp.write("<div class=\"message message-error\"><ul>");
for (Throwable error : errors) {
    if (error instanceof ServletException) {
        Throwable cause = error.getCause();

        if (cause != null) {
            error = cause;
        }
    }

    wp.write("<li>");

    if (error instanceof ValidationException) {
        wp.writeHtml(wp.localize("com.psddev.cms.tool.page.content.Errors", "error.validation"));

    } else if (error instanceof IllegalArgumentException) {
        wp.write(wp.h(error.getMessage()));

    } else {
        List<Throwable> causes = new ArrayList<Throwable>();
        for (; error != null; error = error.getCause()) {
            causes.add(error);
        }
        Collections.reverse(causes);

        wp.writeHtml(wp.localize("com.psddev.cms.tool.page.content.Errors", "error.general"));
        wp.write("<ul class=\"exception\">");
        if (!Settings.get(boolean.class, "cms/tool/hideErrorStackTrace")) {
            for (Throwable cause : causes) {
                    wp.write("<li>");
                    wp.write(wp.h(cause.getClass().getName()));
                    wp.write(": ");
                    wp.write(wp.h(cause.getMessage()));
                    wp.write("<ul class=\"stackTrace\">");
                    for (StackTraceElement e : cause.getStackTrace()) {
                        wp.write("<li>", wp.h(e), "</li>");
                    }
                    wp.write("</ul></li>");
            }
        }
        wp.write("</ul>");
    }

    wp.write("</li>");
}

wp.write("</ul></div>");
%>
