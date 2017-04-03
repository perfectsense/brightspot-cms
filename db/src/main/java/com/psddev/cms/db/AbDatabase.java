package com.psddev.cms.db;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.psddev.dari.db.ForwardingDatabase;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.PaginatedResult;

@Deprecated
public class AbDatabase extends ForwardingDatabase {

    private HttpServletRequest request;

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    // --- ForwardingDatabase support ---

    private <T> T ab(T object) {
        if (object != null) {
            State state = State.getInstance(object);

            for (Map.Entry<String, AbVariationField> entry : state.as(AbVariationObject.class).getFields().entrySet()) {
                String fieldName = entry.getKey();
                AbVariationField variationField = entry.getValue();

                if (variationField == null) {
                    continue;
                }

                List<AbVariation> variations = variationField.getVariations();
                double total = 0.0;

                for (AbVariation variation : variations) {
                    total += variation.getWeight();
                }

                double accum = 0.0;
                double random = AbFilter.Static.random(getRequest(), state, fieldName);

                for (ListIterator<AbVariation> i = variations.listIterator(variations.size());
                        i.hasPrevious();
                        ) {
                    AbVariation variation = i.previous();
                    accum += variation.getWeight();

                    if (random < accum / total) {
                        state.put(fieldName, variation.getValue());
                        break;
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
            ab(item);
        }

        return all;
    }

    @Override
    public <T> T readFirst(Query<T> query) {
        return ab(super.readFirst(query));
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
            return ab(delegate.next());
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
            ab(item);
        }

        return result;
    }

    // --- Deprecated ---

    @Deprecated
    @Override
    public <T> List<T> readList(Query<T> query) {
        List<T> list = super.readList(query);

        for (T item : list) {
            ab(item);
        }

        return list;
    }
}
