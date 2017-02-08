package com.psddev.cms.rtc;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;

class RtcAsyncContextListener implements AsyncListener {

    private final RtcAsyncContext context;

    public RtcAsyncContextListener(RtcAsyncContext context) {
        this.context = context;
    }

    @Override
    public void onComplete(AsyncEvent event) {
        context.disconnect();
    }

    @Override
    public void onTimeout(AsyncEvent event) {
        context.disconnect();
    }

    @Override
    public void onError(AsyncEvent event) {
        context.disconnect();
    }

    @Override
    public void onStartAsync(AsyncEvent event) {
    }
}
