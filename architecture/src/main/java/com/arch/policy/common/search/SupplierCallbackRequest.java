package com.arch.policy.common.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SupplierCallbackRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String searchKey;
    private String supplierId;
    private List<String> results = new ArrayList<String>();
    private boolean searchFinished;

    public String getSearchKey() { return searchKey; }
    public void setSearchKey(String searchKey) { this.searchKey = searchKey; }
    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }
    public List<String> getResults() { return Collections.unmodifiableList(results); }
    public void setResults(List<String> results) {
        this.results = results == null ? new ArrayList<String>() : new ArrayList<String>(results);
    }
    public boolean isSearchFinished() { return searchFinished; }
    public void setSearchFinished(boolean searchFinished) { this.searchFinished = searchFinished; }
}
