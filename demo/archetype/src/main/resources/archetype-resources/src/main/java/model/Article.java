package ${package};

import com.psddev.cms.db.Content;
import com.psddev.cms.db.ToolUi;

public class Article extends Content {

    private String headline;

    @ToolUi.RichText
    private String body;

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
