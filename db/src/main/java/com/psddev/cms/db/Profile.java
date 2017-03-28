package com.psddev.cms.db;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import com.psddev.dari.db.Record;
import com.psddev.dari.util.PullThroughCache;
import com.psddev.dari.util.StringUtils;

/**
 * Profile of the entity visiting a CMS-rendered page.
 *
 * <p>An instance of this class is created automatically for each page view,
 * and contains information about the visitor that can be used to process pages
 * differently using {@linkplain Variation variations}.
 *
 * @deprecated No replacement.
 *
 */
@Deprecated
public class Profile extends Record {

    @Indexed(unique = true)
    @Required
    private String name;

    private Date visitDate;
    private String userAgent;
    private Integer deviceWidth;

    /** Returns the name. Displayed in the tool UI. */
    public String getName() {
        return name;
    }

    /** Sets the name. */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Never {@code null}.
     */
    public Date getVisitDate() {
        if (visitDate == null) {
            visitDate = new Date();
        }
        return visitDate;
    }

    public void setVisitDate(Date visitDate) {
        this.visitDate = visitDate;
    }

    /** Returns the user agent used by the visitor. */
    public String getUserAgent() {
        return userAgent;
    }

    /** Sets the user agent used by the visitor. */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    /** Returns the device width. */
    public Integer getDeviceWidth() {
        return deviceWidth;
    }

    /** Sets the device width. */
    public void setDeviceWidth(Integer deviceWidth) {
        this.deviceWidth = deviceWidth;
    }

    /**
     * Returns {@code true} if the user agent string includes the given
     * regular expression {@code pattern}.
     */
    public boolean checkUserAgent(String pattern) {
        if (pattern != null) {
            String userAgent = getUserAgent();
            if (userAgent != null) {
                return USER_AGENT_CHECKS.get(userAgent + USER_AGENT_CHECK_KEY_SEPARATOR + pattern);
            }
        }
        return false;
    }

    private static final char USER_AGENT_CHECK_KEY_SEPARATOR = '\0';

    private static final Map<String, Boolean> USER_AGENT_CHECKS = new PullThroughCache<String, Boolean>() {
        @Override
        protected Boolean produce(String key) {
            int separatorAt = key.indexOf(USER_AGENT_CHECK_KEY_SEPARATOR);
            String userAgent = key.substring(0, separatorAt);
            String pattern = key.substring(separatorAt + 1);
            return StringUtils.getMatcher(userAgent, "(?i).*" + pattern + ".*").find();
        }
    };

    /**
     * Returns {@code true} if the user agent uses the Gecko layout
     * engine.
     */
    public boolean isUserAgentGecko() {
        return checkUserAgent("Gecko");
    }

    /**
     * Returns {@code true} if the user agent uses the Trident layout
     * engine.
     */
    public boolean isUserAgentTrident() {
        return checkUserAgent("MSIE \\S+");
    }

    /**
     * Returns {@code true} if the user agent uses the Presto layout
     * engine.
     */
    public boolean isUserAgentPresto() {
        return checkUserAgent("Opera[ /]");
    }

    /**
     * Returns {@code true} if the user agent uses the WebKit layout
     * engine.
     */
    public boolean isUserAgentWebKit() {
        return checkUserAgent("WebKit/") && !isUserAgentChrome();
    }

    /** Returns {@code true} if the user agent is Google Chrome. */
    public boolean isUserAgentChrome() {
        return checkUserAgent("Chrome/");
    }

    /** Returns {@code true} if the user agent is Mozilla Firefox. */
    public boolean isUserAgentFirefox() {
        return checkUserAgent("Firefox/");
    }

    /**
     * Returns {@code true} if the user agent is Microsoft Internet
     * Explorer.
     */
    public boolean isUserAgentMsie() {
        return isTrident();
    }

    /**
     * Returns {@code true} if the user agent is Microsoft Internet
     * Explorer Mobile.
     */
    public boolean isUserAgentMsieMobile() {
        return checkUserAgent("IEMobile/");
    }

    /** Returns {@code true} if the user agent is Opera. */
    public boolean isUserAgentOpera() {
        return isPresto();
    }

    /** Returns {@code true} if the user agent is Apple Safari. */
    public boolean isUserAgentSafari() {
        return checkUserAgent("Safari/");
    }

    /** Returns {@code true} if the user agent is running on an iPad. */
    public boolean isUserAgentIpad() {
        return checkUserAgent("iPad");
    }

    /** Returns {@code true} if the user agent is running on an iPhone. */
    public boolean isUserAgentIphone() {
        return checkUserAgent("iPhone");
    }

    /** Returns {@code true} if the user agent is running on Android. */
    public boolean isUserAgentAndroid() {
        return checkUserAgent("Android");
    }

    /** Returns {@code true} if the user agent is running on Mac OS. */
    public boolean isUserAgentMac() {
        return checkUserAgent("Macintosh");
    }

    /** Returns {@code true} if the user agent is running on Windows. */
    public boolean isUserAgentWindows() {
        return checkUserAgent("Windows");
    }

    // --- Deprecated ---

    /** @deprecated No replacement. */
    @Deprecated
    public Map<String, String> getEnvironment() {
        return new LinkedHashMap<String, String>();
    }

    /** @deprecated No replacement. */
    @Deprecated
    public void setEnvironment(Map<String, String> environment) {
    }

    /** @deprecated Use {@link #isUserAgentGecko} instead. */
    @Deprecated
    public boolean isGecko() {
        return isUserAgentGecko();
    }

    /** @deprecated Use {@link #isUserAgentTrident} instead. */
    @Deprecated
    public boolean isTrident() {
        return isUserAgentTrident();
    }

    /** @deprecated Use {@link #isUserAgentPresto} instead. */
    @Deprecated
    public boolean isPresto() {
        return isUserAgentPresto();
    }

    /** @deprecated Use {@link #isUserAgentWebKit} instead. */
    @Deprecated
    public boolean isWebKit() {
        return isUserAgentWebKit();
    }

    /** @deprecated Use {@link #isUserAgentChrome} instead. */
    @Deprecated
    public boolean isChrome() {
        return isUserAgentChrome();
    }

    /** @deprecated Use {@link #isUserAgentFirefox} instead. */
    @Deprecated
    public boolean isFirefox() {
        return isUserAgentFirefox();
    }

    /** @deprecated Use {@link #isUserAgentMsie} instead. */
    @Deprecated
    public boolean isMsie() {
        return isUserAgentMsie();
    }

    /** @deprecated Use {@link #isUserAgentOpera} instead. */
    @Deprecated
    public boolean isOpera() {
        return isUserAgentOpera();
    }

    /** @deprecated Use {@link #isUserAgentSafari} instead. */
    @Deprecated
    public boolean isSafari() {
        return isUserAgentSafari();
    }

    /** @deprecated Use {@link #isUserAgentIpad} instead. */
    @Deprecated
    public boolean isIpad() {
        return isUserAgentIpad();
    }

    /** @deprecated Use {@link #isUserAgentIphone} instead. */
    @Deprecated
    public boolean isIphone() {
        return isUserAgentIphone();
    }

    /** @deprecated Use {@link #isUserAgentMac} instead. */
    @Deprecated
    public boolean isMac() {
        return isUserAgentMac();
    }

    /** @deprecated Use {@link #isUserAgentWindows} instead. */
    @Deprecated
    public boolean isWindows() {
        return isUserAgentWindows();
    }
}
