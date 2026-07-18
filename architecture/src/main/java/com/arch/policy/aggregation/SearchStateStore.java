package com.arch.policy.aggregation;

import java.util.List;
import java.util.Set;

public interface SearchStateStore {
    void initialize(String searchKey, Set<String> supplierIds, long ttlSeconds);
    SearchState getState(String searchKey);
    List<String> getResultPayloads(String searchKey);
    void recordCallback(String searchKey, String supplierId, String payload,
                        boolean finished, long ttlSeconds);
    void markTimedOut(String searchKey, long ttlSeconds);
}
