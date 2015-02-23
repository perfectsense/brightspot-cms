package com.psddev.cms.db;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;

import com.google.common.io.BaseEncoding;
import com.psddev.cms.tool.CmsTool;
import com.psddev.cms.tool.Dashboard;
import com.psddev.cms.tool.SearchResultSelection;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.State;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.Password;
import com.psddev.dari.util.Settings;
import com.psddev.dari.util.StorageItem;

/** User that uses the CMS and other related tools. */
@ToolUi.IconName("object-toolUser")
@Record.BootstrapPackages("Users and Roles")
@Record.BootstrapTypeMappable(groups = Content.class, uniqueKey = "email")
public class ToolUser extends Record implements ToolEntity {

    @Indexed
    @ToolUi.Note("If left blank, the user will have full access to everything.")
    private ToolRole role;

    @Indexed
    @Required
    private String name;

    @Indexed(unique = true)
    private String email;

    @Indexed(unique = true)
    @ToolUi.Placeholder(dynamicText = "${content.email}")
    private String username;

    @ToolUi.FieldDisplayType("password")
    private String password;

    private StorageItem avatar;

    @ToolUi.Tab("Dashboard")
    private Dashboard dashboard;

    @ToolUi.Hidden
    private Date passwordChangedDate;

    private Locale locale = Locale.getDefault();

    @ToolUi.FieldDisplayType("timeZone")
    private String timeZone;

    @ToolUi.Hidden
    private UUID currentPreviewId;

    private String phoneNumber;
    private Set<NotificationMethod> notifyVia;

    @ToolUi.Hidden
    private Map<String, Object> settings;

    @ToolUi.Hidden
    private Site currentSite;

    @ToolUi.Hidden
    private Schedule currentSchedule;

    @ToolUi.Hidden
    private boolean tfaEnabled;

    @ToolUi.Hidden
    private String totpSecret;

    @ToolUi.Hidden
    private long lastTotpCounter;

    @Indexed
    @ToolUi.Hidden
    private String totpToken;

    @ToolUi.Hidden
    private long totpTokenTime;

    @Indexed(unique = true)
    @ToolUi.Hidden
    private Set<String> contentLocks;

    @ToolUi.Hidden
    private Set<UUID> automaticallySavedDraftIds;

    @ToolUi.Hidden
    private boolean external;

    @ToolUi.FieldDisplayType("toolUserSavedSearches")
    @ToolUi.Tab("Search")
    private Map<String, String> savedSearches;

    @ToolUi.Placeholder("All Contents")
    private InlineEditing inlineEditing;

    @ToolUi.Tab("Advanced")
    private boolean returnToDashboardOnSave;

    @ToolUi.Tab("Advanced")
    private boolean disableNavigateAwayAlert;

    @ToolUi.Note("Force the user to change the password on next log in.")
    private boolean changePasswordOnLogIn;

    @Indexed
    @ToolUi.Hidden
    private String changePasswordToken;

    @ToolUi.Hidden
    private long changePasswordTokenTime;

    @Deprecated
    @ToolUi.Placeholder("Default")
    @ToolUi.Tab("Advanced")
    @ToolUi.Values({ "v2", "v3" })
    private String theme;

    @ToolUi.Hidden
    private SearchResultSelection currentSearchResultSelection;

    @ToolUi.Hidden
    private Map<String, String> searchViews;

    @ToolUi.Hidden
    private Map<String, List<String>> searchResultFieldsByTypeId;

    @Indexed
    @Embedded
    @ToolUi.Hidden
    private List<LoginToken> loginTokens;

    /** Returns the role. */
    public ToolRole getRole() {
        return role;
    }

    /** Sets the role. */
    public void setRole(ToolRole role) {
        this.role = role;
    }

    /** Returns the name. */
    public String getName() {
        return name;
    }

    /** Sets the name. */
    public void setName(String name) {
        this.name = name;
    }

    /** Returns the email. */
    public String getEmail() {
        return email;
    }

    /** Sets the email. */
    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    /** Returns the password. */
    public Password getPassword() {
        return Password.valueOf(password);
    }

    /** Sets the password. */
    public void setPassword(Password password) {
        this.password = password.toString();
        this.passwordChangedDate = new Date();
    }

    public Date getPasswordChangedDate() {
        return passwordChangedDate;
    }

    public StorageItem getAvatar() {
        return avatar;
    }

    public void setAvatar(StorageItem avatar) {
        this.avatar = avatar;
    }

    public Dashboard getDashboard() {
        return dashboard;
    }

    public void setDashboard(Dashboard dashboard) {
        this.dashboard = dashboard;
    }

    /**
     * @return the user's locale.
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Sets the user's locale.
     * @param locale the locale.
     */
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    /**
     * Returns the time zone.
     */
    public String getTimeZone() {
        return timeZone;
    }

    /**
     * Sets the time zone.
     */
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    /**
     * Finds the device that the user is using in the given {@code request}.
     *
     * @param request Can't be {@code null}.
     * @return Never {@code null}.
     */
    public ToolUserDevice findOrCreateCurrentDevice(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");

        if (userAgent == null) {
            userAgent = "Unknown Device";
        }

        ToolUserDevice device = null;

        for (ToolUserDevice d : Query.
                from(ToolUserDevice.class).
                where("user = ?", this).
                selectAll()) {
            if (userAgent.equals(d.getUserAgent())) {
                device = d;
                break;
            }
        }

        if (device == null) {
            device = new ToolUserDevice();
            device.setUser(this);
            device.setUserAgent(userAgent);
            device.save();
        }

        return device;
    }

    /**
     * Finds the most recent device that the user was using.
     *
     * @return May be {@code null}.
     */
    public ToolUserDevice findRecentDevice() {
        ToolUserDevice device = null;

        for (ToolUserDevice d : Query.
                from(ToolUserDevice.class).
                where("user = ?").
                selectAll()) {
            if (device == null ||
                    device.findLastAction() == null ||
                    (d.findLastAction() != null &&
                    d.findLastAction().getTime() > device.findLastAction().getTime())) {
                device = d;
            }
        }

        return device;
    }

    /**
     * Saves the given {@code action} performed by this user in the device
     * associated with the given {@code request}.
     *
     * @param request Can't be {@code null}.
     * @param content If {@code null}, does nothing.
     */
    public void saveAction(HttpServletRequest request, Object content) {
        if (content == null ||
                ObjectUtils.to(boolean.class, request.getParameter("_mirror"))) {
            return;
        }

        ToolUserAction action = new ToolUserAction();
        StringBuilder url = new StringBuilder();
        String query = request.getQueryString();

        url.append(request.getServletPath());

        if (query != null) {
            url.append('?');
            url.append(query);
        }

        action.setContentId(State.getInstance(content).getId());
        action.setUrl(url.toString());
        findOrCreateCurrentDevice(request).saveAction(action);
    }

    public UUID getCurrentPreviewId() {
        return currentPreviewId;
    }

    public void setCurrentPreviewId(UUID currentPreviewId) {
        this.currentPreviewId = currentPreviewId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * @return Never {@code null}.
     */
    public Set<NotificationMethod> getNotifyVia() {
        if (notifyVia == null) {
            notifyVia = new LinkedHashSet<NotificationMethod>();
        }
        return notifyVia;
    }

    /**
     * @param notifyVia May be {@code null} to clear the set.
     */
    public void setNotifyVia(Set<NotificationMethod> notifyVia) {
        this.notifyVia = notifyVia;
    }

    /**
     * @deprecated No replacement.
     */
    @Deprecated
    public Set<Notification> getNotifications() {
        return new LinkedHashSet<Notification>();
    }

    /**
     * @deprecated No replacement.
     */
    @Deprecated
    public void setNotifications(Set<Notification> notifications) {
    }

    /** Returns the settings. */
    public Map<String, Object> getSettings() {
        if (settings == null) {
            settings = new LinkedHashMap<String, Object>();
        }
        return settings;
    }

    /** Sets the settings. */
    public void setSettings(Map<String, Object> settings) {
        this.settings = settings;
    }

    public Site getCurrentSite() {
        if ((currentSite == null &&
                hasPermission("site/global")) ||
                (currentSite != null &&
                hasPermission(currentSite.getPermissionId()))) {
            return currentSite;

        } else {
            for (Site s : Site.Static.findAll()) {
                if (hasPermission(s.getPermissionId())) {
                    return s;
                }
            }

            throw new IllegalStateException("No accessible site!");
        }
    }

    public void setCurrentSite(Site site) {
        this.currentSite = site;
    }

    public Schedule getCurrentSchedule() {
        return currentSchedule;
    }

    public void setCurrentSchedule(Schedule currentSchedule) {
        this.currentSchedule = currentSchedule;
    }

    public boolean isTfaEnabled() {
        return tfaEnabled;
    }

    public void setTfaEnabled(boolean tfaEnabled) {
        this.tfaEnabled = tfaEnabled;
    }

    public String getTotpSecret() {
        return totpSecret;
    }

    public String getTotpToken() {
        return totpToken;
    }

    public byte[] getTotpSecretBytes() {
        return BaseEncoding.base32().decode(getTotpSecret());
    }

    public void setTotpSecretBytes(byte[] totpSecretBytes) {
        this.totpSecret = BaseEncoding.base32().encode(totpSecretBytes);
    }

    public void setTotpToken(String totpToken) {
        this.totpToken = totpToken;
        this.totpTokenTime = System.currentTimeMillis();
    }

    private static final String TOTP_ALGORITHM = "HmacSHA1";
    private static final long TOTP_INTERVAL = 30000L;

    private int getTotpCode(long counter) {
        try {
            Mac mac = Mac.getInstance(TOTP_ALGORITHM);

            mac.init(new SecretKeySpec(getTotpSecretBytes(), TOTP_ALGORITHM));

            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array());
            int offset = hash[hash.length - 1] & 0xf;
            int binary =
                    ((hash[offset] & 0x7f) << 24) |
                    ((hash[offset + 1] & 0xff) << 16) |
                    ((hash[offset + 2] & 0xff) << 8) |
                    (hash[offset + 3] & 0xff);

            return binary % 1000000;

        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);

        } catch (InvalidKeyException error) {
            throw new IllegalStateException(error);
        }
    }

    public boolean verifyTotp(int code) {
        long counter = System.currentTimeMillis() / TOTP_INTERVAL - 2;

        for (long end = counter + 5; counter < end; ++ counter) {
            if (counter > lastTotpCounter &&
                    code == getTotpCode(counter)) {
                lastTotpCounter = counter;
                save();
                return true;
            }
        }

        return false;
    }

    private Set<String> createLocks(String idPrefix) {
        long counter = System.currentTimeMillis() / 10000;
        Set<String> locks = new HashSet<String>();

        locks.add(idPrefix + counter);
        locks.add(idPrefix + (counter + 1));

        return locks;
    }

    /**
     * Tries to lock the content with the given {@code id} for exclusive
     * writes.
     *
     * @param id Can't be {@code null}.
     * @return The tool user that holds the lock. Never {@code null}.
     */
    public ToolUser lockContent(UUID id) {
        if (Query.from(CmsTool.class).first().isDisableContentLocking()) {
            return this;
        }

        String idPrefix = id.toString() + '/';
        long counter = System.currentTimeMillis() / 10000;
        String currentCounter = String.valueOf(counter);
        String nextCounter = String.valueOf(counter + 1);
        String currentLock = idPrefix + currentCounter;
        String nextLock = idPrefix + nextCounter;
        ToolUser user = Query.
                from(ToolUser.class).
                where("_id != ?", this).
                and("contentLocks = ?", Arrays.asList(currentLock, nextLock)).
                first();

        if (user != null) {
            return user;
        }

        Set<String> newLocks = contentLocks != null ? contentLocks : new HashSet<String>();
        Set<String> oldLocks = new HashSet<String>(newLocks);

        for (Iterator<String> i = newLocks.iterator(); i.hasNext();) {
            String lock = i.next();

            if (lock.startsWith(idPrefix) ||
                    !(lock.endsWith(currentCounter) ||
                    lock.endsWith(nextCounter))) {
                i.remove();
            }
        }

        newLocks.add(currentLock);
        newLocks.add(nextLock);

        if (!newLocks.equals(oldLocks)) {
            contentLocks = newLocks;
            save();
        }

        return this;
    }

    /**
     * Releases the exclusive write lock on the content with the given
     * {@code id}.
     *
     * @param id Can't be {@code null}.
     */
    public void unlockContent(UUID id) {
        String idPrefix = id.toString() + '/';
        Set<String> locks = createLocks(idPrefix);
        ToolUser user = Query.
                from(ToolUser.class).
                where("_id != ?", this).
                and("contentLocks = ?", locks).
                first();

        if (user != null) {
            for (Iterator<String> i = user.contentLocks.iterator(); i.hasNext();) {
                if (i.next().startsWith(idPrefix)) {
                    i.remove();
                }
            }

            user.save();
        }
    }

    public Set<UUID> getAutomaticallySavedDraftIds() {
        if (automaticallySavedDraftIds == null) {
            automaticallySavedDraftIds = new LinkedHashSet<UUID>();
        }
        return automaticallySavedDraftIds;
    }

    public void setAutomaticallySavedDraftIds(Set<UUID> draftIds) {
        this.automaticallySavedDraftIds = draftIds;
    }

    public boolean isExternal() {
        return external;
    }

    public void setExternal(boolean external) {
        this.external = external;
    }

    public Map<String, String> getSavedSearches() {
        if (savedSearches == null) {
            savedSearches = new CompactMap<String, String>();
        }
        return savedSearches;
    }

    public void setSavedSearches(Map<String, String> savedSearches) {
        this.savedSearches = savedSearches;
    }

    public InlineEditing getInlineEditing() {
        return inlineEditing;
    }

    public void setInlineEditing(InlineEditing inlineEditing) {
        this.inlineEditing = inlineEditing;
    }

    public boolean isReturnToDashboardOnSave() {
        return returnToDashboardOnSave;
    }

    public void setReturnToDashboardOnSave(boolean returnToDashboardOnSave) {
        this.returnToDashboardOnSave = returnToDashboardOnSave;
    }

    /**
     * @return the disableNavigateAwayAlert
     */
    public boolean isDisableNavigateAwayAlert() {
        return disableNavigateAwayAlert;
    }

    /**
     * @param disableNavigateAwayAlert the disableNavigateAwayAlert to set
     */
    public void setDisableNavigateAwayAlert(boolean disableNavigateAwayAlert) {
        this.disableNavigateAwayAlert = disableNavigateAwayAlert;
    }

    public boolean isChangePasswordOnLogIn() {
        return changePasswordOnLogIn;
    }

    public void setChangePasswordOnLogIn(boolean changePasswordOnLogIn) {
        this.changePasswordOnLogIn = changePasswordOnLogIn;
    }

    public String getChangePasswordToken() {
        return changePasswordToken;
    }

    public void setChangePasswordToken(String changePasswordToken) {
        this.changePasswordToken = changePasswordToken;
        this.changePasswordTokenTime = changePasswordToken == null ? 0L : System.currentTimeMillis();
    }

    @Deprecated
    public String getTheme() {
        return theme;
    }

    @Deprecated
    public void setTheme(String theme) {
        this.theme = theme;
    }

    public SearchResultSelection getCurrentSearchResultSelection() {
        return currentSearchResultSelection;
    }

    public void setCurrentSearchResultSelection(SearchResultSelection currentSearchResultSelection) {
        this.currentSearchResultSelection = currentSearchResultSelection;
    }

    public Map<String, String> getSearchViews() {
        if (searchViews == null) {
            searchViews = new CompactMap<>();
        }
        return searchViews;
    }

    public void setSearchViews(Map<String, String> searchViews) {
        this.searchViews = searchViews;
    }

    public Map<String, List<String>> getSearchResultFieldsByTypeId() {
        if (searchResultFieldsByTypeId == null) {
            searchResultFieldsByTypeId = new CompactMap<>();
        }
        return searchResultFieldsByTypeId;
    }

    public void setSearchResultFieldsByTypeId(Map<String, List<String>> searchResultFieldsByTypeId) {
        this.searchResultFieldsByTypeId = searchResultFieldsByTypeId;
    }

    public void updatePassword(Password password) {
        setPassword(password);
        setChangePasswordToken(null);
    }

    /**
     * Returns {@code true} if this user is allowed access to the
     * resources identified by the given {@code permissionId}.
     */
    public boolean hasPermission(String permissionId) {
        ToolRole role = getRole();
        return role != null ? role.hasPermission(permissionId) : true;
    }

    /**
     * Returns {@code true} if forgot paassword email was never sent
     * or was sent before the given {@code interval} in minutes.
     */
    public boolean isAllowedToRequestForgotPassword(long interval) {
        return changePasswordTokenTime + interval * 60L * 1000L < System.currentTimeMillis();
    }

    @Override
    protected void beforeSave() {
        String email = getEmail();
        String username = getUsername();

        if (ObjectUtils.isBlank(email)) {
            if (ObjectUtils.isBlank(username)) {
                throw new IllegalArgumentException("Email or username is required!");

            } else if (username.contains("@")) {
                setEmail(username);
                setUsername(null);
            }
        }
    }

    @Override
    public Iterable<? extends ToolUser> getUsers() {
        return Collections.singleton(this);
    }

    public String generateLoginToken() {
        LoginToken loginToken = new LoginToken();
        getLoginTokens().add(loginToken);
        save();

        return loginToken.getToken();
    }

    public void refreshLoginToken(String token) {
        Iterator<LoginToken> iter = getLoginTokens().iterator();
        while (iter.hasNext()) {
            LoginToken loginToken = iter.next();
            if (loginToken.getToken().equals(token)) {
                loginToken.refreshToken();
            } else if (!loginToken.isValid()) {
                iter.remove();
            }
        }

        save();
    }

    public void removeLoginToken(String token) {
        LoginToken loginToken = getLoginToken(token);
        if (loginToken != null) {
            getLoginTokens().remove(loginToken);
            save();
        }
    }

    public LoginToken getLoginToken(String token) {
        for (LoginToken loginToken : getLoginTokens()) {
            if (loginToken.getToken().equals(token) && loginToken.isValid()) {
                return loginToken;
            }
        }

        return null;
    }

    public List<LoginToken> getLoginTokens() {
        if (loginTokens == null) {
            loginTokens = new ArrayList<LoginToken>();
        }

        return loginTokens;
    }

    public void setLoginTokens(List<LoginToken> loginTokens) {
        this.loginTokens = loginTokens;
    }

    public static class LoginToken extends Record {

        @Indexed
        private String token;
        private Long expireTimestamp;

        public LoginToken() {
            this.token = UUID.randomUUID().toString();

            refreshToken();
        }

        public String getToken() {
            return token;
        }

        public Long getExpireTimestamp() {
            return expireTimestamp;
        }

        public void refreshToken() {
            long sessionTimeout = Settings.getOrDefault(long.class, "cms/tool/sessionTimeout", 0L);

            if (sessionTimeout == 0L) {
                this.expireTimestamp = 0L;
            } else {
                this.expireTimestamp = System.currentTimeMillis() + sessionTimeout;
            }
        }

        public boolean isValid() {
            if (getExpireTimestamp() == 0L) {
                return true;
            }

            return getExpireTimestamp() > System.currentTimeMillis();
        }
    }

    public static final class Static {

        private Static() {
        }

        public static ToolUser getByTotpToken(String totpToken) {
            ToolUser user = Query.from(ToolUser.class).where("totpToken = ?", totpToken).first();
            return user != null && user.totpTokenTime + 60000 > System.currentTimeMillis() ? user : null;
        }

        public static ToolUser getByChangePasswordToken(String changePasswordToken) {
            ToolUser user = Query.from(ToolUser.class).where("changePasswordToken = ?", changePasswordToken).first();
            long expiration = Settings.getOrDefault(long.class, "cms/tool/changePasswordTokenExpirationInHours", 24L) * 60L * 60L * 1000L;
            return user != null && user.changePasswordTokenTime + expiration > System.currentTimeMillis() ? user : null;
        }

        public static ToolUser getByToken(String token) {
            ToolUser user = Query.from(ToolUser.class).where("loginTokens/token = ?", token).first();
            return user != null && user.getLoginToken(token) != null ? user : null;
        }
    }

    public enum InlineEditing {

        ONLY_MAIN_CONTENT("Only Main Content"),
        DISABLED("Disabled");

        private final String label;

        private InlineEditing(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
