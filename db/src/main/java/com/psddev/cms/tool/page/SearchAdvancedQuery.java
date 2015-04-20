package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;

import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.CompoundPredicate;
import com.psddev.dari.db.Predicate;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.StringUtils;

@RoutingFilter.Path(application = "cms", value = "searchAdvancedQuery")
public class SearchAdvancedQuery extends PageServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected String getPermissionId() {
        return null;
    }

    private <T extends SearchAdvancedPredicate> T findSearchAdvancedPredicate(Class<T> predicateClass, String value) {
        for (T p : Query.
                from(predicateClass).
                sortAscending("dari.singleton.key").
                selectAll()) {
            if (p.getParameterValue().equals(value)) {
                return p;
            }
        }

        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        List<String> paramNames = page.paramNamesList();

        for (String paramName : paramNames) {
            if (paramName.startsWith("action-remove-")) {
                String index = paramName.substring(14);

                page.getResponse().sendRedirect(page.url("", paramName, null).
                        replaceAll("\\?p" + index + "=1", "?").
                        replaceAll("&p" + index + "=1", "").
                        replaceAll("\\?" + index + "\\.[^=]+=[^&]*", "?").
                        replaceAll("&" + index + "\\.[^=]+=[^&]*", ""));
                return;
            }
        }

        int lastIndex = -1;
        Predicate globalPredicate = null;
        SearchAdvancedPredicate.Compound globalPredicateType = findSearchAdvancedPredicate(SearchAdvancedPredicate.Compound.class, page.param(String.class, "gpt"));

        Collections.sort(paramNames);

        page.writeHeader();
            page.writeStart("div", "class", "widget widget-searchAdvancedQuery");
                page.writeStart("h1", "class", "icon icon-wrench");
                    page.writeHtml("Advanced Query Builder");
                page.writeEnd();

                page.writeStart("style", "type", "text/css");
                    page.writeCss(".form-submit",
                            "opacity", "0.5",
                            "pointer-events", "none");
                page.writeEnd();

                page.writeStart("form",
                        "method", "get",
                        "action", page.url(null),
                        "onsubmit", "$(this).addClass('form-submit');return true;");
                    page.writeStart("select",
                            "data-bsp-autosubmit", "",
                            "name", "gpt");
                        for (SearchAdvancedPredicate.Compound pt : Query.
                                from(SearchAdvancedPredicate.Compound.class).
                                sortAscending("dari.singleton.key").
                                selectAll()) {
                            if (globalPredicateType == null) {
                                globalPredicateType = pt;
                            }

                            page.writeStart("option",
                                    "selected", pt.equals(globalPredicateType) ? "selected" : null,
                                    "value", pt.getParameterValue());
                                page.writeHtml(pt.getLabel());
                                page.writeHtml(":");
                            page.writeEnd();
                        }
                    page.writeEnd();

                    for (String paramName : paramNames) {
                        if (paramName.startsWith("p")) {
                            Integer index = ObjectUtils.to(Integer.class, paramName.substring(1));

                            if (index != null) {
                                if (lastIndex < index) {
                                    lastIndex = index;
                                }
                            }
                        }
                    }

                    page.writeStart("button",
                            "class", "icon icon-action-add link",
                            "name", "p" + (lastIndex + 1),
                            "value", 1);
                        page.writeHtml("Add Another ");
                        page.writeHtml(globalPredicateType.getLabel());
                    page.writeEnd();

                    page.writeStart("div", "class", "fixedScrollable");
                        page.writeStart("ul");
                            for (String paramName : paramNames) {
                                if (paramName.startsWith("p")) {
                                    Integer index = ObjectUtils.to(Integer.class, paramName.substring(1));

                                    if (index != null) {
                                        page.writeStart("li");
                                            globalPredicate = CompoundPredicate.combine(
                                                    globalPredicateType.getOperator(),
                                                    globalPredicate,
                                                    writeSearchAdvancedPredicate(page, paramNames, paramName, String.valueOf(index)));
                                        page.writeEnd();
                                    }
                                }
                            }
                        page.writeEnd();
                    page.writeEnd();

                    page.writeStart("div", "class", "actions");
                        page.writeStart("button",
                                "class", "icon icon-action-search",
                                "name", "action-search",
                                "value", true);
                            page.writeHtml("Search");
                        page.writeEnd();
                    page.writeEnd();
                page.writeEnd();
            page.writeEnd();

            String pageId = page.createId();

            page.writeStart("div", "id", pageId);
            page.writeEnd();

            page.writeStart("script", "type", "text/javascript");
                page.writeRaw("var $page = $('#" + pageId + "'),");
                page.writeRaw("$edit = $page.popup('source');");

                page.writeRaw("$edit.attr('href', '");
                page.writeRaw(StringUtils.escapeJavaScript(page.url("", "action-search", null)));
                page.writeRaw("');");

                if (page.param(String.class, "action-search") != null) {
                    page.writeRaw("var $input = $edit.closest('.searchFilter-advancedQuery').find('input[type=\"text\"]');");

                    page.writeRaw("$input.val('" + StringUtils.escapeJavaScript(globalPredicate != null ? globalPredicate.toString() : "") + "');");
                    page.writeRaw("$input.change();");
                    page.writeRaw("$page.popup('close');");
                }
            page.writeEnd();
        page.writeFooter();
    }

    public Predicate writeSearchAdvancedPredicate(
            ToolPageContext page,
            List<String> paramNames,
            String predicateParam,
            String paramPrefix)
            throws IOException {

        page.writeElement("input",
                "type", "hidden",
                "name", predicateParam,
                "value", 1);

        String predicateTypeParam = paramPrefix + ".pt";
        SearchAdvancedPredicate predicateType = findSearchAdvancedPredicate(SearchAdvancedPredicate.class, page.param(String.class, predicateTypeParam));

        if (predicateType == null) {
            predicateType = findSearchAdvancedPredicate(SearchAdvancedPredicate.class, "C");
        }

        page.writeStart("button",
                "class", "icon icon-action-remove icon-only link",
                "name", "action-remove-" + paramPrefix,
                "value", true);
            page.writeHtml("Remove");
        page.writeEnd();

        page.writeHtml(" ");
        page.writeStart("select",
                "data-bsp-autosubmit", "",
                "name", predicateTypeParam);
            for (SearchAdvancedPredicate pt : Query.
                    from(SearchAdvancedPredicate.class).
                    sortAscending("dari.singleton.key").
                    selectAll()) {
                page.writeStart("option",
                        "selected", pt.equals(predicateType) ? "selected" : null,
                        "value", pt.getParameterValue());
                    page.writeHtml(pt.getLabel());
                page.writeEnd();
            }
        page.writeEnd();

        return predicateType.writeInputs(
                this, page, paramNames, predicateParam, paramPrefix);
    }
}
