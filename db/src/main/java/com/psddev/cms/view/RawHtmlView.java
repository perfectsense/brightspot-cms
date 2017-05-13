package com.psddev.cms.view;

@ViewInterface
@ViewRendererClass(RawHtmlViewRenderer.class)
public interface RawHtmlView {

    String getHtml();
}
