package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.CommonParams;
import com.psddev.cms.db.Site;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.PageWriter;
import com.psddev.cms.tool.Search;
import com.psddev.cms.tool.SearchResultRenderer;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.PredicateParser;
import com.psddev.dari.db.SolrDatabase;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;


@RoutingFilter.Path(application = "cms", value = "/content/suggestions")
public class SearchResultSuggestions extends PageServlet {

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {

        if (page.requireUser()) {
            return;
        }

        try {
            Map<String, Object> searchData = (Map<String, Object>) ObjectUtils.fromJson(page.param(String.class, "search"));
            Search search = new Search(page);

            search.getState().setValues(searchData);

            Map<String, Object> objectData = (Map<String, Object>) ObjectUtils.fromJson(page.param(String.class, "object"));
            Object object = Database.Static.getDefault().getEnvironment().createObject(ObjectUtils.to(UUID.class, objectData.get("_type")), ObjectUtils.to(UUID.class, objectData.get("_id")));
            State objectState = State.getInstance(object);

            objectState.setValues(objectData);

            ObjectType objectType = objectState.getType();

            if (objectType == null) {
                return;
            }

            String fieldName = page.param(String.class, "field");
            ObjectField field = objectType.getField(fieldName);

            if (field == null) {
                return;
            }

            Map<Object, Float> suggestions = new HashMap<Object, Float>();
            StringBuilder filter = new StringBuilder();

            for (ObjectType t : field.as(ToolUi.class).findDisplayTypes()) {
                filter.append(SolrDatabase.Static.escapeValue(t.getId()));
                filter.append(" || ");
            }

            Site site = page.getUser().getCurrentSite();

            for (Object item : findSimilar(object, filter, 10)) {
                Float score = SolrDatabase.Static.getNormalizedScore(item);

                if (score != null && score > 0.7) {
                    if (site != null &&
                            !PredicateParser.Static.evaluate(item, site.itemsPredicate())) {
                        continue;
                    }

                    suggestions.put(item, score);
                }
            }

            filter.setLength(0);

            String fieldClass = field.getJavaDeclaringClassName();

            for (ObjectType type : objectState.getDatabase().getEnvironment().getTypes()) {
                ObjectField f = type.getField(fieldName);

                if (f != null && ObjectUtils.equals(fieldClass, f.getJavaDeclaringClassName())) {
                    filter.append(SolrDatabase.Static.escapeValue(type.getId()));
                    filter.append(" || ");
                }
            }

            Map<Object, Integer> similar = new HashMap<Object, Integer>();

            for (Object item : findSimilar(object, filter, 20)) {
                Float score = SolrDatabase.Static.getNormalizedScore(item);

                if (score > 0.7) {
                    if (site != null &&
                            !PredicateParser.Static.evaluate(item, site.itemsPredicate())) {
                        continue;
                    }

                    Object value = State.getInstance(item).get(fieldName);

                    if (value == null) {
                        continue;
                    }

                    if (value instanceof Map) {
                        value = ((Map<?, ?>) value).values();
                    }

                    if (value instanceof Iterable) {
                        for (Object v : (Iterable<?>) value) {
                            incrementCount(similar, v);
                        }

                    } else {
                        incrementCount(similar, value);
                    }
                }
            }

            for (Map.Entry<Object, Integer> entry : similar.entrySet()) {
                Integer count = entry.getValue();

                if (count > 1) {
                    suggestions.put(entry.getKey(), ((float) count) / similar.size());
                }
            }

            if (suggestions.isEmpty()) {
                return;
            }

            Object fieldValue = State.getInstance(object).get(fieldName);

            if (fieldValue != null) {
                if (fieldValue instanceof Map) {
                    fieldValue = ((Map<?, ?>) fieldValue).values();
                }

                if (fieldValue instanceof Iterable) {
                    for (Object v : (Iterable<?>) fieldValue) {
                        suggestions.remove(v);
                    }

                } else {
                    suggestions.remove(fieldValue);
                }
            }

            if (suggestions.isEmpty()) {
                return;
            }

            List<Map.Entry<Object, Float>> suggestionsList = new ArrayList<Map.Entry<Object, Float>>(suggestions.entrySet());

            Collections.sort(suggestionsList, new Comparator<Map.Entry<Object, Float>>() {
                public int compare(Map.Entry<Object, Float> x, Map.Entry<Object, Float> y) {
                    float xv = x.getValue();
                    float yv = y.getValue();

                    return xv == yv ? 0 : xv < yv ? 1 : -1;
                }
            });

            List<Object> sortedSuggestions = new ArrayList<Object>();

            for (Map.Entry<Object, Float> entry : suggestionsList) {
                sortedSuggestions.add(entry.getKey());
            }

            if (sortedSuggestions.size() > 10) {
                sortedSuggestions = sortedSuggestions.subList(0, 10);
            }

            PageWriter writer = page.getWriter();

            writer.start("div", "class", "searchSuggestions");
            writer.start("h2").html("Suggestions").end();
            new SearchResultRenderer(page, search).renderList(sortedSuggestions);
            writer.end();
        } catch (Exception error) {

        }
    }

    private static List<?> findSimilar(Object object, StringBuilder filter, int rows) {
        if (filter.length() < 1) {
            return Collections.emptyList();
        }

        filter.setLength(filter.length() - 4);
        filter.insert(0, "typeId:(");
        filter.append(")");

        List<Object> items = new ArrayList<>();
        SolrDatabase solr = Database.Static.getFirst(SolrDatabase.class);
        SolrQuery solrQuery = solr.buildSimilarQuery(object);

        solrQuery.add(CommonParams.FQ, filter.toString());
        solrQuery.setStart(0);
        solrQuery.setRows(rows);
        items = solr.queryPartialWithOptions(solrQuery, null).getItems();

        return items;
    }

    private static void incrementCount(Map<Object, Integer> map, Object object) {
        Integer count = map.get(object);

        map.put(object, count != null ? count + 1 : 1);
    }
}
