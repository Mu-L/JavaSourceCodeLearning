package com.arch.policy.search.infrastructure.kafka;

import java.util.ArrayList;
import java.util.List;

public final class PolicyChangeMessage {
    private String operation;
    private int policyId;
    private String detail;
    private List<String> indexTerms = new ArrayList<String>();
    private long position;

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    public int getPolicyId() { return policyId; }
    public void setPolicyId(int policyId) { this.policyId = policyId; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public List<String> getIndexTerms() { return indexTerms; }
    public void setIndexTerms(List<String> indexTerms) { this.indexTerms = indexTerms; }
    public long getPosition() { return position; }
    public void setPosition(long position) { this.position = position; }
}
