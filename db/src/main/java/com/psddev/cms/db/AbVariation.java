package com.psddev.cms.db;

import com.psddev.dari.db.Metric;
import com.psddev.dari.db.MetricInterval;
import com.psddev.dari.db.Record;

@Deprecated
@AbVariation.Embedded
public class AbVariation extends Record {

    private double weight;
    private Object value;

    @Indexed
    @MetricValue(interval = MetricInterval.None.class)
    private Metric impressions;

    @Indexed
    @MetricValue(interval = MetricInterval.None.class)
    private Metric conversions;

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
