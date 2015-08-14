package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.psddev.cms.db.ToolUser;
import com.psddev.cms.tool.AuthenticationFilter;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.Settings;
import com.psddev.dari.util.StringUtils;

@RoutingFilter.Path(application = "cms", value = "inlineEditorCookie")
public class InlineEditorCookie extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final byte[] ONE_PIXEL_GIF = {
            71, 73, 70, 56, 57, 97, 1, 0, 1, 0, -128, 0, 0, -1, -1, -1, 0, 0,
            0, 33, -7, 4, 1, 0, 0, 0, 0, 44, 0, 0, 0, 0, 1, 0, 1, 0, 0, 2, 2,
            68, 1, 0, 59 };

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String userId = request.getParameter("userId");
        String token = request.getParameter("token");

        if (userId != null) {
            String signature = StringUtils.hex(StringUtils.hmacSha1(Settings.getSecret(), userId + token));

            if (signature.equals(request.getParameter("signature"))) {
                ToolUser user = Query
                        .from(ToolUser.class)
                        .where("_id = ?", ObjectUtils.to(UUID.class, userId))
                        .first();

                if (user != null) {
                    AuthenticationFilter.Static.logIn(request, response, user, token);

                } else {
                    AuthenticationFilter.Static.logOut(request, response);
                }
            }
        }

        response.setContentType("image/gif");
        response.getOutputStream().write(ONE_PIXEL_GIF);
    }
}
