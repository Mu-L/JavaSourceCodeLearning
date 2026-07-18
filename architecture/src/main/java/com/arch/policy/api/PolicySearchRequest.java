package com.arch.policy.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PolicySearchRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String criteria;
    private List<String> supplierIds = new ArrayList<String>();
    private long totalTimeoutMillis = 3000;

    public String getCriteria() { return criteria; }
    public void setCriteria(String criteria) { this.criteria = criteria; }
    public List<String> getSupplierIds() { return Collections.unmodifiableList(supplierIds); }
    public void setSupplierIds(List<String> supplierIds) {
        this.supplierIds = supplierIds == null ? new ArrayList<String>() : new ArrayList<String>(supplierIds);
    }
    public long getTotalTimeoutMillis() { return totalTimeoutMillis; }
    public void setTotalTimeoutMillis(long totalTimeoutMillis) { this.totalTimeoutMillis = totalTimeoutMillis; }
}
