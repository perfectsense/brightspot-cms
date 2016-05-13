package com.psddev.cms.db;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.ApplicationFilter;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.AbstractFilter;
import com.psddev.dari.util.ObjectUtils;

/**
 * Intercepts /cms/content/edit.jsp?copyId=[UUID] and provides hook for custom
 * copy behavior through {@link Copyable#beforeCopy}.
 */
public class CopyFilter extends AbstractFilter implements AbstractFilter.Auto {

    private static final String COPY_ID_PARAM = "copyId";
    private static final String EDIT_JSP = "/content/edit.jsp";

    @Override
    protected void doRequest(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws Exception {

        // Short-circuit quickly without instantiating ToolPageContext.
        if (request.getParameter(COPY_ID_PARAM) != null) {

            ToolPageContext page = new ToolPageContext(getServletContext(), request, response);
            String editUrl = page.cmsUrl(EDIT_JSP);
            String normalizedRequestUrl = request.getRequestURI().replaceAll("/+", "/");

            if (!ObjectUtils.equals(editUrl, normalizedRequestUrl)
                || page.requireUser()
                || !doCopy(page)) {
                chain.doFilter(request, response);
            }

        } else {
            chain.doFilter(request, response);
        }
    }

    private static boolean doCopy(ToolPageContext page) throws IOException {

        UUID sourceId = page.param(UUID.class, COPY_ID_PARAM);
        Site site = page.getSite();

        // Query source object including invisible references.  Cache is prevented which insures both that invisibles
        // are properly resolved and no existing instance of the source object becomes linked to the copy.
        // This prevents mutations to the new copy from affecting the original source object if it is subsequently saved.
        Object source = sourceId != null ? Query.fromAll().where("id = ?", sourceId).resolveInvisible().noCache().first() : null;

        if (source == null
            || !(site == null || Site.Static.isObjectAccessible(site, source))) {
            // Fall back to normal copy behavior and error message
            return false;
        }

        State sourceState = State.getInstance(source);

        // Don't intercept copy actions for source States that aren't Copyable
        if (!Copyable.isCopyable(sourceState)) {
            return false;
        }

        State destinationState = Copyable.copy(source, site, page.getUser(), null);
        destinationState.save();

        page.redirect(EDIT_JSP, "id", destinationState.getId(), "typeId", sourceState.getType().getId());
        return true;
    }

    @Override
    public void updateDependencies(Class<? extends AbstractFilter> filterClass, List<Class<? extends Filter>> dependencies) {
        if (ApplicationFilter.class.isAssignableFrom(filterClass)) {
            dependencies.add(getClass());
        }
    }
}
