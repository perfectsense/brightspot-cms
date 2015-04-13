<%@ page session="false"
    import="com.psddev.cms.tool.ToolPageContext,
    com.psddev.dari.db.ReferentialText,
    com.psddev.cms.db.Site,
    com.psddev.dari.db.Query,
    com.psddev.dari.util.StorageItem,
    com.psddev.cms.db.Variation,
    java.util.Iterator,
    java.util.UUID,
    java.util.HashMap,
    java.util.List,
    java.util.Map,
    java.util.ArrayList,
    com.psddev.cms.db.Content,
    com.psddev.cms.db.ContainerSection,
    com.psddev.cms.db.PageFilter,
    com.psddev.cms.db.Guide,
    com.psddev.cms.db.GuidePage,
    com.psddev.cms.db.GuideSection,
    com.psddev.cms.db.Template,
    com.psddev.dari.db.State,
    com.psddev.cms.tool.ToolPageContext"%>
<%@ taglib prefix="cms" uri="http://psddev.com/cms"%>
<jsp:useBean id="pageProductionGuide"
    class="com.psddev.cms.db.GuidePage" scope="request" />
<jsp:useBean id="sectionProductionGuide"
    class="com.psddev.cms.db.GuideSection" scope="request" />


<%
    // --- Logic ---

    ToolPageContext wp = new ToolPageContext(pageContext);
    if (wp.requirePermission("area/dashboard")) {
        return;
    }

    GuidePage pg = Query.findById(GuidePage.class, wp.uuidParam("pageGuideId"));
    Guide guide = Query.findById(Guide.class, wp.uuidParam("guideId"));
    String widgetTitle = "";
    if (guide != null) {
        widgetTitle = "Production Guide: " + guide.getTitle();
    } else if (pg != null) {
        widgetTitle = "Production Guide " + pg.getName();
    }

    List<GuidePage> pages = null;
    String nextTemplate = "";
    if (guide != null) {
        pages = guide.getTemplatesToIncludeInGuide();
    }

    // --- Presentation ---
%>


<style>
@media print {
    body {
        margin: 0 !important;
    }
    body > * {
        display: none !important;
    }
    body > .popup[name='guidePrint'] {
        display: block !important;
        position: static !important;
    }
    body > .popup[name='guidePrint'] .marker {
        display: none !important;
    }
    body > .popup[name='guidePrint'] .closeButton {
        display: none !important;
    }
    body > .popup[name='guidePrint'] .content {
        box-shadow: 0 0 0 !important;
        -webkit-box-shadow: 0 0 0 !important;
        -moz-box-shadow: 0 0 0 !important;;
    }
}
</style>


<%
    wp.include("/WEB-INF/header.jsp");

    if (guide != null)
%>

<div class="widget">
    <h1 class="icon-page_white_find">
        <%=widgetTitle%>
    </h1>

    <div class="variation">
    </div>



    <%
        GuideSection section = null;
        int curCnt = 1;
        Iterator iterT = null;
    %>
    <div class="content-edit">
        <form href="" class="guideForm">

            <%
                // Write overview page
                if (guide != null) {
                    wp.write("<div class=\"guideForm-page\">");
                    wp.write("<div class=\"guideForm-main\">");
                    // Main Production Guide Overview
                    // Display the Overview
                    request.setAttribute("productionGuide", guide);
            %><div class="guideOverview">
                <cms:render value="${productionGuide.overview}" />
            </div>
            <%
                wp.write("</div>"); //end guideForm-main
                    wp.write("</div>"); //end guideForm-page

                    GuidePage prevT = null;
                    GuidePage nxtT = null;
                    GuidePage curT = null;

                    iterT = pages.iterator();
                    if (iterT.hasNext()) {
                        pg = (GuidePage) iterT.next();
                    } else {
                        pg = null;
                    }
                }
                while (pg != null) {

                    if (guide != null) {
                        wp.write("<div style=\"page-break-before:always\">");
                    } else {
                        wp.write("<div>");
                    }
                    wp.write("<div class=\"guideForm-main\">");
                    StorageItem samplePage = pg.getSamplePageSnapshot();

                    wp.write("<div class=\"guideTop\">");

                    // Display the Summary Page
                    request.setAttribute("pageProductionGuide",
                            pg);
                    wp.write("<strong>");
                    if (guide != null) {
                        wp.write("Production Guide: " + guide.getTitle() + "<br/>");
                    }
                    if (pg.getName() != null && !pg.getName().isEmpty()) {
                        wp.write(pg.getName() + " ");
                    }
                    wp.write("Summary: ");
                    wp.write("</strong></div>");
            %><div class="guideDescription">
                <cms:render value="${pageProductionGuide.description}" />
            </div>
            <%
                    wp.write("</div>"); //end guideForm-main
                    wp.write("</div>"); //end guideForm-page

                    // If there's a snapshot, print it here
                    if (samplePage != null && samplePage.getUrl() != null) {
                        wp.write("<div class=\"guideForm-page\">");
                        wp.write("<div class=\"guideForm-main\">");
                        %>
                        <div style="page-break-before:always">
                        <div class="guidePreviewFrame">
                    <cms:img src="${pageProductionGuide.samplePageSnapshot}" />
                    </div>
                    </div>
                    <%

                        wp.write("</div>"); // end guideForm-page
                        wp.write("</div>"); // end guideForm-main
                    }

                    //Get the list of sections that are in the layout
                    Iterable<GuideSection> sections = null;
                    HashMap<UUID, String> nameMap = null;
                    Iterator iter = null;
                    sections = pg.getSectionDescriptions();
                    if (sections != null) {
                        nameMap = Guide.Static.getSectionNameMap(sections);
                        iter = sections.iterator();
                    }

                    while (iter != null && iter.hasNext()) {
                        section = (GuideSection) iter.next();

                        GuideSection sectionGuide = section;
                        if (sectionGuide == null
                                || sectionGuide.getDescription() == null
                                || sectionGuide.getDescription().isEmpty()) {
                            continue;
                        }
                        wp.write("<div style=\"page-break-before:always\">");
                        wp.write("<div class=\"guideForm-main\">");
                        wp.write("<div class=\"guideTop\">");
                        request.setAttribute("sectionProductionGuide", sectionGuide);
                        wp.write("<strong>");
                        if (guide != null) {
                            wp.write("Production Guide: " + guide.getTitle()
                                    + "<br/>");
                        }
                        if (pg.getName() != null && !pg.getName().isEmpty()) {
                            wp.write(pg.getName() + " ");
                        }
                        wp.write("Section: ");
                        wp.write(section.getSectionName());
                        wp.write("</strong></div>"); // end guideTop
            %><div class="guideDescription">
                <cms:render value="${sectionProductionGuide.description}" />
            </div>
            <%
                List<GuidePage> references = Guide.Static.getSectionReferences(
                                section, pg);

                        if (references != null && references.size() > 0) {
                            wp.write("<div class=\"guideModuleReferences\"");
                            wp.write("<p>This module also appears on:</p>");
                            for (GuidePage reference : references) {
                                wp.write("<li>", reference.getName(), "</li>");
                            }
                            wp.write("</div>");
                        }
                        if (sectionGuide != null && sectionGuide.getTips() != null
                                && !sectionGuide.getTips().isEmpty()) {
            %><div class="guideTips">
                <cms:render value="${sectionProductionGuide.tips}" />
            </div>

            <%
                }
                        wp.write("</div>"); // end guideForm-main
                        wp.write("</div>"); // end guideForm-page
                    } // end while section
                    if (guide != null && iterT.hasNext()) {
                        pg = (GuidePage) iterT.next();
                    } else {
                        pg = null;
                    }
                } // end while template
            %>
        </form>
    </div>
</div>

<script>
onload:window.print();
</script>

<%
    wp.include("/WEB-INF/footer.jsp");
%>
