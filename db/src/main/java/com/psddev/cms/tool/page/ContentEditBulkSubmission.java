package com.psddev.cms.tool.page;

import com.psddev.cms.db.Content;
import com.psddev.cms.db.Site;
import com.psddev.cms.db.ToolUser;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.State;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;
import com.psddev.dari.util.Task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import java.util.stream.Collectors;

import javax.servlet.ServletException;

public class ContentEditBulkSubmission extends Record {

    @Indexed
    private Site submitSite;

    @Indexed
    private ToolUser submitUser;

    @Indexed
    private Date submitDate = new Date();

    private Query<?> query;
    private long count;
    private Map<String, Object> replaces;
    private Map<String, Object> adds;
    private Map<String, Object> removes;
    private Set<String> clears;
    private long successes;
    private long failures;
    private BulkErrorRecord bulkErrorRecord;

    @Indexed
    private Date finishDate;

    public Site getSubmitSite() {
        return submitSite;
    }

    public void setSubmitSite(Site submitSite) {
        this.submitSite = submitSite;
    }

    public ToolUser getSubmitUser() {
        return submitUser;
    }

    public void setSubmitUser(ToolUser submitUser) {
        this.submitUser = submitUser;
    }

    public Date getSubmitDate() {
        return submitDate;
    }

    public void setSubmitDate(Date submitDate) {
        this.submitDate = submitDate;
    }

    public Query<?> getQuery() {
        return query;
    }

    public void setQuery(Query<?> query) {
        this.query = query;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public Map<String, Object> getReplaces() {
        if (replaces == null) {
            replaces = new CompactMap<>();
        }
        return replaces;
    }

    public void setReplaces(Map<String, Object> replaces) {
        this.replaces = replaces;
    }

    public Map<String, Object> getAdds() {
        if (adds == null) {
            adds = new CompactMap<>();
        }
        return adds;
    }

    public void setAdds(Map<String, Object> adds) {
        this.adds = adds;
    }

    public Map<String, Object> getRemoves() {
        if (removes == null) {
            removes = new CompactMap<>();
        }
        return removes;
    }

    public void setRemoves(Map<String, Object> removes) {
        this.removes = removes;
    }

    public Set<String> getClears() {
        if (clears == null) {
            clears = new LinkedHashSet<>();
        }
        return clears;
    }

    public void setClears(Set<String> clears) {
        this.clears = clears;
    }

    public long getSuccesses() {
        return successes;
    }

    public void setSuccesses(long successes) {
        this.successes = successes;
    }

    public long getFailures() {
        return failures;
    }

    public void setFailures(long failures) {
        this.failures = failures;
    }

    public BulkErrorRecord getBulkErrorRecord() {
        return bulkErrorRecord;
    }

    public void setBulkErrorRecord(BulkErrorRecord bulkErrorRecord) {
        this.bulkErrorRecord = bulkErrorRecord;
    }

    public Date getFinishDate() {
        return finishDate;
    }

    public void setFinishDate(Date finishDate) {
        this.finishDate = finishDate;
    }

    public void submitTask() {
        save();
        new ContentEditBulkTask().submit();
    }

    private class ContentEditBulkTask extends Task {

        public ContentEditBulkTask() {
            super("Content Edit Bulk", "");
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void doTask() {
            try {
                Site site = getSubmitSite();
                ToolUser user = getSubmitUser();
                Map<String, Object> replaces = getReplaces();
                Map<String, Object> adds = getAdds();
                Map<String, Object> removes = getRemoves();
                Set<String> clears = getClears();

                for (Iterator<?> i = getQuery().iterable(0).iterator(); shouldContinue() && i.hasNext();) {
                    Object item = i.next();
                    State itemState = State.getInstance(item);

                    itemState.putAll(replaces);

                    for (Map.Entry<String, Object> entry : adds.entrySet()) {
                        String fieldName = entry.getKey();
                        Object newValue = entry.getValue();
                        Object oldValue = itemState.get(fieldName);

                        if (oldValue instanceof Map) {
                            if (newValue instanceof Map) {
                                ((Map<Object, Object>) oldValue).putAll((Map<Object, Object>) newValue);

                            } else if (newValue instanceof Collection) {
                                ((Map<Object, Object>) oldValue).keySet().addAll((Collection<Object>) newValue);
                            }

                        } else if (oldValue instanceof Collection) {
                            if (newValue instanceof Map) {
                                ((Collection<Object>) oldValue).addAll(((Map<Object, Object>) newValue).values());

                            } else if (newValue instanceof Collection) {
                                ((Collection<Object>) oldValue).addAll((Collection<Object>) newValue);

                            } else {
                                ((Collection<Object>) oldValue).add(newValue);
                            }

                        } else {
                            itemState.put(fieldName, newValue);
                        }
                    }

                    for (Map.Entry<String, Object> entry : removes.entrySet()) {
                        String fieldName = entry.getKey();
                        Object newValue = entry.getValue();
                        Object oldValue = itemState.get(fieldName);

                        if (oldValue instanceof Map) {
                            if (newValue instanceof Map) {
                                ((Map<Object, Object>) oldValue).keySet().removeAll(((Map<Object, Object>) newValue).keySet());

                            } else if (newValue instanceof Collection) {
                                ((Map<Object, Object>) oldValue).keySet().removeAll((Collection<Object>) newValue);
                            }

                        } else if (oldValue instanceof Collection) {
                            if (newValue instanceof Map) {
                                ((Collection<Object>) oldValue).removeAll(((Map<Object, Object>) newValue).values());

                            } else if (newValue instanceof Collection) {
                                ((Collection<Object>) oldValue).removeAll((Collection<Object>) newValue);

                            } else {
                                ((Collection<Object>) oldValue).remove(newValue);
                            }
                        }
                    }

                    for (String clear : clears) {
                        itemState.remove(clear);
                    }

                    try {
                        Content.Static.publish(item, site, user);
                        setSuccesses(getSuccesses() + 1);

                    } catch (Exception e) {
                        setFailures(getFailures() + 1);

                        //Save first error
                        if (ObjectUtils.isBlank(getBulkErrorRecord())) {

                            BulkErrorRecord bulkErrorRecord = new BulkErrorRecord();
                            bulkErrorRecord.setRecordId(itemState.getId());
                            bulkErrorRecord.setRecordLabel(itemState.getLabel());

                            Throwable error = e;
                            if (error instanceof ServletException) {
                                Throwable cause = error.getCause();
                                if (cause != null) {
                                    error = cause;
                                }
                            }

                            List<BulkErrorCause> bulkErrorCauseList = new ArrayList<>();

                            List<Throwable> causes = new ArrayList<>();
                            for (; error != null; error = error.getCause()) {
                                causes.add(error);
                            }

                            Collections.reverse(causes);
                            for (Throwable cause : causes) {
                                BulkErrorCause bulkErrorCause = new BulkErrorCause();
                                bulkErrorCause.setName(cause.getClass().getName());
                                bulkErrorCause.setMessage(cause.getMessage());

                                bulkErrorCause.setStackElements(Arrays.stream(cause.getStackTrace())
                                        .map(element -> element.toString())
                                        .filter(element -> StringUtils.isBlank(element))
                                        .collect(Collectors.toList()));
                                bulkErrorCauseList.add(bulkErrorCause);
                            }
                            bulkErrorRecord.setBulkErrorCauses(bulkErrorCauseList);
                            setBulkErrorRecord(bulkErrorRecord);

                        }
                    }

                    save();
                }

            } finally {
                setFinishDate(new Date());
                save();
            }
        }
    }

    @Embedded
    public static class BulkErrorRecord extends Record {
        private UUID recordId;

        private String recordLabel;

        private List<BulkErrorCause> bulkErrorCauses;

        public UUID getRecordId() {
            return recordId;
        }

        public void setRecordId(UUID recordId) {
            this.recordId = recordId;
        }

        public String getRecordLabel() {
            return recordLabel;
        }

        public void setRecordLabel(String recordLabel) {
            this.recordLabel = recordLabel;
        }

        public List<BulkErrorCause> getBulkErrorCauses() {
            return bulkErrorCauses;
        }

        public void setBulkErrorCauses(List<BulkErrorCause> bulkErrorCauses) {
            this.bulkErrorCauses = bulkErrorCauses;
        }
    }

    @Embedded
    public static class BulkErrorCause extends Record {
        private String name;

        private String message;

        private List<String> stackElements;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public List<String> getStackElements() {
            return stackElements;
        }

        public void setStackElements(List<String> stackElements) {
            this.stackElements = stackElements;
        }
    }
}
