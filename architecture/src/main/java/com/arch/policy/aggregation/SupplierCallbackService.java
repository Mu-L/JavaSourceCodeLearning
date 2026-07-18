package com.arch.policy.aggregation;

import com.arch.policy.api.SupplierCallbackRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class SupplierCallbackService {
    private final SearchStateStore stateStore;
    private final ObjectMapper objectMapper;
    private final long redisTtlSeconds;

    public SupplierCallbackService(SearchStateStore stateStore, ObjectMapper objectMapper,
                                   long redisTtlSeconds) {
        this.stateStore = stateStore;
        this.objectMapper = objectMapper;
        this.redisTtlSeconds = redisTtlSeconds;
    }

    public void callback(SupplierCallbackRequest request) throws Exception {
        validate(request);
        String payload = request.getResults().isEmpty() ? "" : objectMapper.writeValueAsString(request);
        stateStore.recordCallback(request.getSearchKey(), request.getSupplierId(), payload,
                request.isSearchFinished(), redisTtlSeconds);
    }

    public void markFinishedWithoutResult(String searchKey, String supplierId) throws Exception {
        SupplierCallbackRequest request = new SupplierCallbackRequest();
        request.setSearchKey(searchKey);
        request.setSupplierId(supplierId);
        request.setSearchFinished(true);
        callback(request);
    }

    private static void validate(SupplierCallbackRequest request) {
        if (request == null || request.getSearchKey() == null || request.getSupplierId() == null) {
            throw new IllegalArgumentException("searchKey and supplierId are required");
        }
    }
}
