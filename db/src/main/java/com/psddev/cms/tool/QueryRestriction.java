package com.psddev.cms.tool;

import com.psddev.dari.db.Query;
import com.psddev.dari.util.ClassFinder;
import com.psddev.dari.util.TypeDefinition;

import java.io.IOException;

public interface QueryRestriction {

    public static Iterable<Class<? extends QueryRestriction>> classIterable() {
        return ClassFinder.Static.findClasses(QueryRestriction.class);
    }

    public static void updateQueryUsingAll(Query<?> query, ToolPageContext page) {
        for (Class<? extends QueryRestriction> c : classIterable()) {
            TypeDefinition.getInstance(c).newInstance().updateQuery(query, page);
        }
    }

    public void writeHtml(ToolPageContext page) throws IOException;

    public void updateQuery(Query<?> query, ToolPageContext page);
}
