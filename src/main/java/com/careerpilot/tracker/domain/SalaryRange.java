package com.careerpilot.tracker.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class SalaryRange {

    @Column(name = "salary_min")
    private Integer min;

    @Column(name = "salary_max")
    private Integer max;

    @Column(name = "currency", length = 3)
    private String currency;

    protected SalaryRange() {
    }

    public SalaryRange(Integer min, Integer max, String currency) {
        if (min != null && max != null && min > max) {
            throw new IllegalArgumentException("Minimum salary cannot exceed maximum salary");
        }
        this.min = min;
        this.max = max;
        this.currency = currency;
    }

    public Integer getMin() { return min; }
    public Integer getMax() { return max; }
    public String getCurrency() { return currency; }
}
