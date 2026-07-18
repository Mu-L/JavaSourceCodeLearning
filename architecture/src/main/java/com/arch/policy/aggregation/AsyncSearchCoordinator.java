package com.arch.policy.aggregation;

import com.arch.policy.api.PolicySearchRequest;
import com.arch.policy.api.PolicySearchResponse;
import com.arch.policy.api.SupplierCallbackRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class AsyncSearchCoordinator {
    private static final long MAX_WAIT_SLICE_MILLIS = 200;
    private final SearchStateStore stateStore;
    private final SupplierTaskDispatcher dispatcher;
    private final LocalSearchWaiters localWaiters;
    private final ObjectMapper objectMapper;
    private final long redisTtlSeconds;

    public AsyncSearchCoordinator(SearchStateStore stateStore, SupplierTaskDispatcher dispatcher,
                                  LocalSearchWaiters localWaiters, ObjectMapper objectMapper,
                                  long redisTtlSeconds) {
        this.stateStore = stateStore;
        this.dispatcher = dispatcher;
        this.localWaiters = localWaiters;
        this.objectMapper = objectMapper;
        this.redisTtlSeconds = redisTtlSeconds;
    }

    public PolicySearchResponse search(PolicySearchRequest request) throws Exception {
        validate(request);
        String searchKey = UUID.randomUUID().toString();
        Set<String> suppliers = new LinkedHashSet<String>(request.getSupplierIds());
        stateStore.initialize(searchKey, suppliers, redisTtlSeconds);
        LocalSearchWaiters.Waiter waiter = localWaiters.register(searchKey);
        try {
            if (stateStore.getState(searchKey) == SearchState.COMPLETED) return aggregate(searchKey);
            dispatcher.dispatch(searchKey, suppliers, request);
            return awaitResults(searchKey, request.getTotalTimeoutMillis(), waiter);
        } finally {
            localWaiters.remove(searchKey, waiter);
        }
    }

    private PolicySearchResponse awaitResults(String searchKey, long timeoutMillis,
                                               LocalSearchWaiters.Waiter waiter) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (true) {
            SearchState state = stateStore.getState(searchKey);
            if (state == SearchState.COMPLETED) return aggregate(searchKey);
            long remaining = deadline - System.currentTimeMillis();
            if (state == SearchState.TIMED_OUT || remaining <= 0) {
                stateStore.markTimedOut(searchKey, redisTtlSeconds);
                return aggregate(searchKey);
            }
            waiter.await(Math.min(remaining, MAX_WAIT_SLICE_MILLIS));
        }
    }

    private PolicySearchResponse aggregate(String searchKey) throws Exception {
        List<SupplierCallbackRequest> results = new ArrayList<SupplierCallbackRequest>();
        for (String payload : stateStore.getResultPayloads(searchKey)) {
            results.add(objectMapper.readValue(payload, SupplierCallbackRequest.class));
        }
        SearchState state = stateStore.getState(searchKey);
        return new PolicySearchResponse(searchKey, state.name(), results);
    }

    private static void validate(PolicySearchRequest request) {
        if (request == null) throw new IllegalArgumentException("request is required");
        if (request.getTotalTimeoutMillis() <= 0) throw new IllegalArgumentException("timeout must be positive");
        if (request.getSupplierIds().isEmpty()) throw new IllegalArgumentException("supplierIds is required");
    }
}
