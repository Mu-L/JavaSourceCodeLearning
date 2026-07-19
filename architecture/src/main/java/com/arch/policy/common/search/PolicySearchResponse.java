package com.arch.policy.common.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PolicySearchResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String searchKey;
    private final String state;
    private final List<SupplierCallbackRequest> results;

    public PolicySearchResponse(String searchKey, String state, List<SupplierCallbackRequest> results) {
        this.searchKey = searchKey;
        this.state = state;
        this.results = Collections.unmodifiableList(new ArrayList<SupplierCallbackRequest>(results));
    }
    public String getSearchKey() { return searchKey; }
    public String getState() { return state; }
    public List<SupplierCallbackRequest> getResults() { return results; }
}
