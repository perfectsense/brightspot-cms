package com.psddev.cms.db;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.psddev.cms.tool.AuthenticationFilter;
import com.psddev.dari.db.ForwardingDatabase;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.PaginatedResult;

// CHECKSTYLE:OFF
/**
 * Database wrapper that applies all profile-specific variations
 * automatically.
 *
 * @deprecated No replacement.
 */
@Deprecated
public class VaryingDatabase extends ForwardingDatabase {

    private HttpServletRequest request;
    private Profile profile;

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    /** Returns the profile. */
    public Profile getProfile() {
        HttpServletRequest request = getRequest();

        if (request != null &&
                !AuthenticationFilter.Static.isAuthenticated(request)) {
            return profile;

        } else {
            return null;
        }
    }

    /** Sets the profile. */
    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    // --- ForwardingDatabase support ---

    @Override
    public <T> List<T> readAll(Query<T> query) {
        List<T> all = super.readAll(query);
        Profile profile = getProfile();

        if (profile != null) {
            for (T item : all) {
                Variation.Static.applyAll(item, profile);
            }
        }

        return all;
    }

    @Override
    public <T> T readFirst(Query<T> query) {
        T first = super.readFirst(query);

        if (first != null) {
            Profile profile = getProfile();

            if (profile != null) {
                Variation.Static.applyAll(first, getProfile());
            }
        }

        return first;
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
            E item = delegate.next();

            if (item != null) {
                Profile profile = getProfile();

                if (profile != null) {
                    Variation.Static.applyAll(item, profile);
                }
            }

            return item;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public <T> PaginatedResult<T> readPartial(Query<T> query, long offset, int limit) {
        PaginatedResult<T> result = super.readPartial(query, offset, limit);
        Profile profile = getProfile();

        if (profile != null) {
            for (T item : result.getItems()) {
                Variation.Static.applyAll(item, profile);
            }
        }

        return result;
    }

    // --- Deprecated ---

    @Deprecated
    @Override
    public <T> List<T> readList(Query<T> query) {
        List<T> list = super.readList(query);
        Profile profile = getProfile();

        if (profile != null) {
            for (T item : list) {
                Variation.Static.applyAll(item, profile);
            }
        }

        return list;
    }
}
