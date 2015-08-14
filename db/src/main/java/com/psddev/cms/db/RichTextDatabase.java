package com.psddev.cms.db;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.psddev.dari.db.ForwardingDatabase;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.ReferentialText;
import com.psddev.dari.db.State;
import com.psddev.dari.util.PaginatedResult;

public class RichTextDatabase extends ForwardingDatabase {

    private static final LoadingCache<String, String> PUBLISHABLES = CacheBuilder
            .newBuilder()
            .maximumSize(10000)
            .build(new CacheLoader<String, String>() {

                @Override
                public String load(String value) {
                    List<Object> publishables = new ReferentialText(value, true).toPublishables(true, new RichTextCleaner());

                    return publishables.isEmpty() ? "" : (String) publishables.get(0);
                }
            });

    private final Set<UUID> cleaned = new HashSet<UUID>();

    // --- ForwardingDatabase support ---

    public <T> T clean(T object) {
        if (object == null) {
            return null;
        }

        State state = State.getInstance(object);

        if (!cleaned.add(state.getId())) {
            return object;
        }

        ObjectType type = state.getType();

        if (type != null) {
            for (ObjectField field : type.getFields()) {
                if (field.as(ToolUi.class).isRichText()) {
                    String fieldName = field.getInternalName();
                    Object value = state.get(fieldName);

                    if (value instanceof String) {
                        try {
                            state.put(fieldName, PUBLISHABLES.getUnchecked((String) value));

                        } catch (IllegalStateException error) {
                            List<Object> publishables = new ReferentialText((String) value, true).toPublishables(true, new RichTextCleaner());

                            state.put(fieldName, publishables.isEmpty() ? "" : (String) publishables.get(0));
                        }
                    }
                }
            }
        }

        return object;
    }

    @Override
    public <T> List<T> readAll(Query<T> query) {
        List<T> all = super.readAll(query);

        for (T item : all) {
            clean(item);
        }

        return all;
    }

    @Override
    public <T> T readFirst(Query<T> query) {
        return clean(super.readFirst(query));
    }

    @Override
    public <T> Iterable<T> readIterable(Query<T> query, int fetchSize) {
        return new FilteringIterable<T>(super.readIterable(query, fetchSize));
    }

    private class FilteringIterable<E> implements Iterable<E> {

        private final Iterable<E> delegate;

        public FilteringIterable(Iterable<E> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Iterator<E> iterator() {
            return new FilteringIterator<E>(delegate.iterator());
        }
    }

    private class FilteringIterator<E> implements Closeable, Iterator<E> {

        private final Iterator<E> delegate;

        public FilteringIterator(Iterator<E> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void close() throws IOException {
            if (delegate instanceof Closeable) {
                ((Closeable) delegate).close();
            }
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public E next() {
            return clean(delegate.next());
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public <T> PaginatedResult<T> readPartial(Query<T> query, long offset, int limit) {
        PaginatedResult<T> result = super.readPartial(query, offset, limit);

        for (T item : result.getItems()) {
            clean(item);
        }

        return result;
    }

    // --- Deprecated ---

    @Deprecated
    @Override
    public <T> List<T> readList(Query<T> query) {
        List<T> list = super.readList(query);

        for (T item : list) {
            clean(item);
        }

        return list;
    }
}
