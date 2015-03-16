package com.psddev.cms.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.psddev.dari.db.Query;
import com.psddev.dari.db.Record;
import com.psddev.dari.util.ObjectUtils;

/**
 * @deprecated No replacement. Create your own.
 */
@ContentStream.Embedded
@Deprecated
public abstract class ContentStream extends Record {

    public abstract List<?> findContents(int offset, int limit);

    public static class Mix extends ContentStream {

        private List<AreasStream> streams;

        public List<AreasStream> getStreams() {
            return streams;
        }

        public void setStreams(List<AreasStream> streams) {
            this.streams = streams;
        }

        public List<?> findContents(int offset, int limit) {
            List<Object> contents = new ArrayList<Object>();

            for (int i = 0; i < limit; ++ i) {
                contents.add(null);
            }

            for (AreasStream as : getStreams()) {
                List<Integer> areas = new ArrayList<Integer>(as.getAreas());
                int areasSize = areas.size();
                List<?> streamContents = as.getStream().findContents(0, areas.size());
                int streamContentsSize = streamContents.size();

                Collections.sort(areas);

                for (int i = 0; i < areasSize; ++ i) {
                    contents.set(areas.get(i), i < streamContentsSize ? streamContents.get(i) : null);
                }
            }

            return contents;
        }
    }

    /**
     * @deprecated No replacement. Create your own.
     */
    @Deprecated
    @Embedded
    public static class AreasStream extends Record {

        @Required
        private Set<Integer> areas;

        @Required
        private ContentStream stream;

        public Set<Integer> getAreas() {
            if (areas == null) {
                areas = new LinkedHashSet<Integer>();
            }
            return areas;
        }

        public void setAreas(Set<Integer> areas) {
            this.areas = areas;
        }

        public ContentStream getStream() {
            return stream;
        }

        public void setStream(ContentStream stream) {
            this.stream = stream;
        }

        @Override
        public String getLabel() {
            StringBuilder label = new StringBuilder();
            Set<Integer> areas = getAreas();

            if (!ObjectUtils.isBlank(areas)) {
                label.append(areas);
            }

            return label.toString();
        }
    }

    /**
     * @deprecated No replacement. Create your own.
     */
    @Deprecated
    public static class Search extends ContentStream {

        @Embedded
        @Required
        private Query<?> query;

        public Query<?> getQuery() {
            return query;
        }

        public void setQuery(Query<?> query) {
            this.query = query;
        }

        @Override
        public List<?> findContents(int offset, int limit) {
            return getQuery().select(offset, limit).getItems();
        }
    }

    /**
     * @deprecated No replacement. Create your own.
     */
    @Deprecated
    public static class Static extends ContentStream {

        private List<Content> contents;

        public List<Content> getContents() {
            if (contents == null) {
                contents = new ArrayList<Content>();
            }
            return contents;
        }

        public void setContents(List<Content> contents) {
            this.contents = contents;
        }

        @Override
        public List<Content> findContents(int offset, int limit) {
            return getContents();
        }
    }
}
