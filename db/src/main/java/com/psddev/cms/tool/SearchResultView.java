package com.psddev.cms.tool;

import java.io.IOException;

public interface SearchResultView {

    /**
     * @return Never {@code null}.
     */
    public String getIconName();

    /**
     * @return Never {@code null}.
     */
    public String getDisplayName();

    /**
     * Returns {@code true} if this view supports displaying the given
     * {@code type}.
     *
     * @param search Can't be {@code null}.
     */
    public boolean isSupported(Search search);

    /**
     * Returns {@code true} if this view is the preferred default for
     * displaying the given {@code type}.
     *
     * @param search Can't be {@code null}.
     */
    public boolean isPreferred(Search search);

    /**
     * Returns {@code true} if the output of the {@link #writeHtml} call
     * should be wrapped with standard controls like view selection and
     * search result actions.
     *
     * @param search Can't be {@code null}.
     * @param page Can't be {@code null}.
     * @param itemWriter Can't be {@code null}.
     */
    public boolean isHtmlWrapped(
            Search search,
            ToolPageContext page,
            SearchResultItem itemWriter);

    /**
     * @param search Can't be {@code null}.
     * @param page Can't be {@code null}.
     * @param itemWriter Can't be {@code null}.
     * @throws IOException if unable to write to the given {@code page}.
     */
    public void writeHtml(
            Search search,
            ToolPageContext page,
            SearchResultItem itemWriter)
            throws IOException;
}
