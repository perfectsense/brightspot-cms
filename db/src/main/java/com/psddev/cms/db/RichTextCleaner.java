package com.psddev.cms.db;

import java.util.UUID;

import org.jsoup.nodes.Element;

import com.psddev.dari.db.ReferentialText;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectUtils;

public class RichTextCleaner implements ReferentialText.Cleaner {

    @Override
    public void before(Element body) {
    }

    @Override
    public void after(Element body) {
        for (Element a : body.getElementsByTag("a")) {
            if (ObjectUtils.equals(a.attr("href"), a.attr("data-cms-href"))) {
                UUID id = ObjectUtils.to(UUID.class, a.attr("data-cms-id"));

                if (id != null) {
                    Object item = Query.fromAll().where("_id = ?", id).first();

                    if (item != null) {
                        String href = item instanceof Content
                                ? ((Content) item).getPermalink()
                                : State.getInstance(item).as(Directory.ObjectModification.class).getPermalink();

                        if (href != null) {
                            a.attr("href", href);
                        }
                    }
                }
            }

            a.removeAttr("data-cms-href");
            a.removeAttr("data-cms-id");
        }
    }
}
