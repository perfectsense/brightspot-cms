package com.psddev.cms.db;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.TryCatchFinally;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.psddev.dari.util.JspUtils;
import com.psddev.dari.util.Settings;

public class CacheTag extends BodyTagSupport implements TryCatchFinally {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheTag.class);
    private static final ConcurrentMap<String, Output> OUTPUT_LOCKS = new ConcurrentHashMap<String, Output>();
    private static final Cache<String, Output> OUTPUT_CACHE = CacheBuilder
            .newBuilder()
            .maximumSize(Settings.getOrDefault(int.class, "brightspot/cacheTagOutputMaximumSize", 100000))
            .removalListener(new RemovalListener<String, Output>() {

                @Override
                public void onRemoval(RemovalNotification<String, Output> notification) {
                    OUTPUT_LOCKS.remove(notification.getKey());
                }
            })
            .build();

    private String name;
    private long duration;

    private Output output;

    public void setName(String name) {
        this.name = name;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    // --- TagSupport support ---

    @Override
    public int doStartTag() throws JspException {
        String key = JspUtils.getCurrentServletPath((HttpServletRequest) pageContext.getRequest()) + "/" + name;
        bodyContent = null;
        output = OUTPUT_LOCKS.get(key);

        // Output is expired? While producing, it's not considered expired
        // because lastProduced field is set far in the future.
        if (output != null
                && System.currentTimeMillis() - output.lastProduced > duration) {
            setOutput(output, null);
            OUTPUT_LOCKS.remove(key);
            OUTPUT_CACHE.invalidate(key);
            output = null;
        }

        // Output isn't cached, so flag it to be produced.
        if (output == null) {
            output = new Output();
            output.key = key;

            // Make sure there's only one producing output at [R].
            Output o = OUTPUT_LOCKS.putIfAbsent(key, output);
            if (o == null) {
                OUTPUT_CACHE.put(key, output);
                LOGGER.debug("Producing [{}] in [{}]", key, Thread.currentThread());
                return EVAL_BODY_BUFFERED;
            }

            output = o;
        }

        return SKIP_BODY;
    }

    private void setOutput(Output output, String body) {
        synchronized (output) {
            output.body = body;
            output.lastProduced = System.currentTimeMillis();
            output.notifyAll();
            LOGGER.debug("Produced [{}] at [{}]", output.key, output.lastProduced);
        }
    }

    @Override
    public int doEndTag() throws JspException {
        String body;

        // [R] Cache the produced output and wake up all other threads
        // that might be waiting.
        if (bodyContent != null) {
            body = bodyContent.getString();
            setOutput(output, body);

        // Wait if another thread is producing output.
        } else {
            try {
                synchronized (output) {
                    while (output.lastProduced == Output.PRODUCING) {
                        LOGGER.debug("Waiting for production of [{}] in [{}]", output.key, Thread.currentThread());
                        output.wait(1000);
                    }
                }
            } catch (InterruptedException ex) {
                throw new JspException(ex);
            }

            body = output.body;
        }

        try {
            if (body != null) {
                pageContext.getOut().write(body);
            }
        } catch (IOException ex) {
            throw new JspException(ex);
        }

        return EVAL_PAGE;
    }

    // --- TryCatchFinally support ---

    @Override
    public void doCatch(Throwable error) throws Throwable {
        setOutput(output, null);
        OUTPUT_LOCKS.remove(output.key);
        OUTPUT_CACHE.invalidate(output.key);
        throw error;
    }

    @Override
    public void doFinally() {
    }

    private static class Output {

        public static final long PRODUCING = Long.MAX_VALUE;

        public String key;
        public String body;
        public long lastProduced = PRODUCING;
    }
}
