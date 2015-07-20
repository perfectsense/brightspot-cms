package com.psddev.cms.tool.page;

import com.psddev.cms.db.Directory;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.SearchResultField;
import com.psddev.cms.tool.SearchResultSelection;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.cms.tool.Search;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.Metric;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ClassFinder;
import com.psddev.dari.util.CollectionUtils;
import com.psddev.dari.util.HtmlWriter;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.StorageItem;
import com.psddev.dari.util.TypeDefinition;
import com.psddev.dari.util.TypeReference;
import com.psddev.dari.util.UrlBuilder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@RoutingFilter.Path(application = "cms", value = ExportContent.PATH)
public class ExportContent extends PageServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExportContent.class);

    public static final String PATH = "exportContent";

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {

        execute(new Context(page));
    }

    public void execute(ToolPageContext page, Search search, SearchResultSelection selection) throws IOException, ServletException {

        execute(new Context(page, search, selection));
    }

    private void execute(Context page) throws IOException, ServletException {

        if (page.param(boolean.class, Context.ACTION_PARAMETER)) {

            HttpServletResponse response = page.getResponse();

            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", "attachment; filename=search-result-" + new DateTime(null, page.getUserDateTimeZone()).toString("yyyy-MM-dd-hh-mm-ss") + ".csv");

            page.writeHeaderRow();

            Query searchQuery = page.getSearch().toQuery(page.getSite());

            if (page.getSelection() != null) {
                searchQuery.where(page.getSelection().createItemsQuery().getPredicate());
            }

            for (Object item : searchQuery.iterable(0)) {
                page.writeDataRow(item);
            }

        } else {

            // Only display the button when a search has been refined to a single type
            if (page.getSearch() != null && page.getSearch().getSelectedType() != null) {

                page.writeStart("div", "class", "searchResult-action-simple");
                page.writeStart("a",
                        "class", "button",
                        "target", "_top",
                        "href", getActionUrl(page, null, Context.ACTION_PARAMETER, true));
                page.writeHtml("Export");
                page.writeHtml(page.getSelection() != null ? " Selected" : " All");
                page.writeEnd();
                page.writeEnd();
            }
        }
    }

    /**
     * Helper method for generating a stateful ExportContent servlet URL for forms and anchors.
     * @param page an instance of Context
     * @param exportType An ObjectType for which the export is requested.
     * @param params Additional query parameters to attach to the returned URL.
     * @return the requested URL
     */
    private String getActionUrl(Context page, ObjectType exportType, Object... params) {

        UrlBuilder urlBuilder = new UrlBuilder(page.getRequest())
                .absolutePath(page.cmsUrl(PATH));

        // reset action parameter
        urlBuilder.parameter(Context.ACTION_PARAMETER, null);

        if (page.getSearch() != null) {
            // Search uses current page parameters
            urlBuilder.currentParameters();
        }

        // SearchResultSelection uses an ID parameter
        urlBuilder.parameter(Context.SELECTION_ID_PARAMETER, page.getSelection() != null ? page.getSelection().getId() : null);

        // SearchResultSelection export requires an ObjectType to be selected
        urlBuilder.parameter(Context.TYPE_ID_PARAMETER, exportType != null ? exportType.getId() : null);

        for (int i = 0; i < params.length / 2; i++) {

            urlBuilder.parameter(params[i], params[i + 1]);
        }

        return urlBuilder.toString();
    }

    private static class Context extends ToolPageContext {

        public static final String SELECTION_ID_PARAMETER = "selectionId";
        public static final String SEARCH_PARAMETER = "search";
        public static final String ACTION_PARAMETER = "action-download";

        private static final String CSV_LINE_TERMINATOR = "\r\n";
        private static final Character CSV_BOUNDARY = '\"';
        private static final Character CSV_DELIMITER = ',';

        private static final String VALUE_DELIMITER = ", ";

        private Search search;
        private SearchResultSelection selection;

        public Context(PageContext pageContext) {
            this(pageContext.getServletContext(), (HttpServletRequest) pageContext.getRequest(), (HttpServletResponse) pageContext.getResponse(), null, null);
        }

        public Context(ToolPageContext page) {

            this(page.getServletContext(), page.getRequest(), page.getResponse(), null, null);
        }

        public Context(ToolPageContext page, Search search, SearchResultSelection selection) {

            this(page.getServletContext(), page.getRequest(), page.getResponse(), search, selection);
        }

        public Context(ServletContext servletContext, HttpServletRequest request, HttpServletResponse response, Search search, SearchResultSelection selection) {

            super(servletContext, request, response);

            String selectionId = param(String.class, SELECTION_ID_PARAMETER);

            if (selection != null) {

                setSelection(selection);

            } else if (!ObjectUtils.isBlank(selectionId)) {

                LOGGER.debug("Found " + SELECTION_ID_PARAMETER + " query parameter with value: " + selectionId);
                SearchResultSelection queriedSelection = (SearchResultSelection) Query.fromAll().where("_id = ?", selectionId).first();

                if (queriedSelection == null) {
                    throw new IllegalArgumentException("No SearchResultSelection exists for id " + selectionId);
                }

                setSelection(queriedSelection);
            }

            if (search != null) {

                setSearch(search);
            } else {

                Search searchFromJson = searchFromJson();

                if (searchFromJson == null) {

                    LOGGER.debug("Could not obtain Search object from JSON query parameter");
                    searchFromJson = new Search();
                }

                setSearch(searchFromJson);
            }
        }

        public Search getSearch() {
            return search;
        }

        public void setSearch(Search search) {
            this.search = search;
        }

        public SearchResultSelection getSelection() {
            return selection;
        }

        public void setSelection(SearchResultSelection selection) {
            this.selection = selection;
        }

        /**
         * Produces a Search object from JSON and prevents errors when the same query parameter name is used for non-JSON Search representation.
         * @return Search if a query parameter specifies valid Search JSON, null otherwise.
         */
        public Search searchFromJson() {

            Search search = null;

            String searchParam = param(String.class, SEARCH_PARAMETER);

            if (searchParam != null) {

                try {

                    Map<String, Object> searchJson = ObjectUtils.to(new TypeReference<Map<String, Object>>() {
                    }, ObjectUtils.fromJson(searchParam));
                    search = new Search();
                    search.getState().setValues(searchJson);

                } catch (Exception ignore) {

                    // Ignore.  Search will be constructed below using ToolPageContext
                }
            }

            return search;
        }

        public void writeHeaderRow() throws IOException {

            if (getSearch() == null || getSearch().getSelectedType() == null || !hasPermission("type/" + search.getSelectedType().getId() + "/read")) {

                return;
            }

            ObjectType selectedType = getSearch().getSelectedType();

            if (selectedType == null) {

                return;
            }

            writeRaw('\ufeff');
            writeRaw(CSV_BOUNDARY);

            writeCsvItem("Type");

            writeRaw(CSV_BOUNDARY).writeRaw(CSV_DELIMITER).writeRaw(CSV_BOUNDARY);

            writeCsvItem("Label");

            writeRaw(CSV_BOUNDARY);

            List<String> fieldNames = getUser().getSearchResultFieldsByTypeId().get(selectedType.getId().toString());

            if (fieldNames == null) {
                for (Class<? extends SearchResultField> c : ClassFinder.Static.findClasses(SearchResultField.class)) {
                    if (!c.isInterface() && !Modifier.isAbstract(c.getModifiers())) {
                        SearchResultField field = TypeDefinition.getInstance(c).newInstance();

                        if (field.isDefault(selectedType)) {
                            writeRaw(CSV_DELIMITER).writeRaw(CSV_BOUNDARY);
                            writeRaw(field.createHeaderCellText());
                            writeRaw(CSV_BOUNDARY);
                        }
                    }
                }

            } else {
                for (String fieldName : fieldNames) {
                    Class<?> fieldNameClass = ObjectUtils.getClassByName(fieldName);

                    if (fieldNameClass != null && SearchResultField.class.isAssignableFrom(fieldNameClass)) {
                        @SuppressWarnings("unchecked")
                        SearchResultField field = TypeDefinition.getInstance((Class<? extends SearchResultField>) fieldNameClass).newInstance();

                        if (field.isSupported(selectedType)) {
                            writeRaw(CSV_DELIMITER).writeRaw(CSV_BOUNDARY);
                            writeRaw(field.createHeaderCellText());
                            writeRaw(CSV_BOUNDARY);
                        }

                    } else {
                        ObjectField field = selectedType.getField(fieldName);

                        if (field == null) {
                            field = Database.Static.getDefault().getEnvironment().getField(fieldName);
                        }

                        if (field != null) {
                            writeRaw(CSV_DELIMITER).writeRaw(CSV_BOUNDARY);
                            writeCsvItem(field.getDisplayName());
                            writeRaw(CSV_BOUNDARY);
                        }
                    }
                }
            }
            writeRaw(CSV_LINE_TERMINATOR);
        }

        public void writeDataRow(Object item) throws IOException {

            if (getSearch() == null || getSearch().getSelectedType() == null || !hasPermission("type/" + search.getSelectedType().getId() + "/read")) {

                return;
            }

            ObjectType selectedType = getSearch().getSelectedType();

            if (selectedType == null) {

                return;
            }

            State itemState = State.getInstance(item);

            writeRaw(CSV_BOUNDARY);
            writeCsvItem(getTypeLabel(item));
            writeRaw(CSV_BOUNDARY).writeRaw(CSV_DELIMITER).writeRaw(CSV_BOUNDARY);
            writeCsvItem(getObjectLabel(item));
            writeRaw(CSV_BOUNDARY);

            List<String> fieldNames = getUser().getSearchResultFieldsByTypeId().get(selectedType.getId().toString());

            if (fieldNames == null) {
                for (Class<? extends SearchResultField> c : ClassFinder.Static.findClasses(SearchResultField.class)) {
                    if (!c.isInterface() && !Modifier.isAbstract(c.getModifiers())) {
                        SearchResultField field = TypeDefinition.getInstance(c).newInstance();

                        if (field.isDefault(selectedType)) {
                            writeRaw(field.createDataCellText(item));
                        }
                    }
                }

            } else {
                for (String fieldName : fieldNames) {
                    Class<?> fieldNameClass = ObjectUtils.getClassByName(fieldName);

                    if (fieldNameClass != null && SearchResultField.class.isAssignableFrom(fieldNameClass)) {
                        @SuppressWarnings("unchecked")
                        SearchResultField field = TypeDefinition.getInstance((Class<? extends SearchResultField>) fieldNameClass).newInstance();

                        if (field.isSupported(selectedType)) {
                            writeRaw(CSV_DELIMITER).writeRaw(CSV_BOUNDARY);
                            writeRaw(field.createDataCellText(item));
                            writeRaw(CSV_BOUNDARY);
                        }

                    } else {
                        ObjectField field = selectedType.getField(fieldName);

                        if (field == null) {
                            field = Database.Static.getDefault().getEnvironment().getField(fieldName);
                        }

                        if (field != null) {
                            writeRaw(CSV_DELIMITER).writeRaw(CSV_BOUNDARY);
                            if ("cms.directory.paths".equals(field.getInternalName())) {
                                for (Iterator<Directory.Path> i = itemState.as(Directory.ObjectModification.class).getPaths().iterator(); i.hasNext();) {
                                    Directory.Path p = i.next();
                                    String path = p.getPath();

                                    writeCsvItem(path);
                                    writeHtml(" (");
                                    writeCsvItem(p.getType());
                                    writeHtml(")");

                                    if (i.hasNext()) {
                                        writeRaw(VALUE_DELIMITER);
                                    }
                                }

                            } else {
                                for (Iterator<Object> i = CollectionUtils.recursiveIterable(itemState.getByPath(field.getInternalName())).iterator(); i.hasNext();) {
                                    Object value = i.next();
                                    writeCsvItem(value);
                                    if (i.hasNext()) {
                                        writeRaw(VALUE_DELIMITER);
                                    }
                                }
                            }
                            writeRaw(CSV_BOUNDARY);
                        }
                    }
                }
            }

            writeRaw(CSV_LINE_TERMINATOR);
        }

        private void writeCsvItem(Object item) throws IOException {

            StringWriter stringWriter = new StringWriter();
            HtmlWriter htmlWriter = new HtmlWriter(stringWriter);

            htmlWriter.putOverride(Recordable.class, (HtmlWriter writer, Recordable object) ->
                            writer.writeHtml(getObjectLabel(object))
            );

            // Override Metric fields to output the total sum
            htmlWriter.putOverride(Metric.class, (HtmlWriter writer, Metric object) ->
                            writer.write(Double.toString(object.getSum()))
            );

            htmlWriter.putOverride(StorageItem.class, (HtmlWriter writer, StorageItem storageItem) ->
                            writer.write(storageItem.getPublicUrl())
            );

            htmlWriter.writeObject(item);

            write(stringWriter.toString().replaceAll(CSV_BOUNDARY.toString(), CSV_BOUNDARY.toString() + CSV_BOUNDARY));
        }
    }
}

