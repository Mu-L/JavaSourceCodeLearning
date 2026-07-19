package com.arch.policy.search.application;

import com.arch.policy.common.search.PolicySearchRequest;
import com.arch.policy.common.search.PolicySearchResponse;
import com.arch.policy.common.search.SupplierCallbackRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AsyncSearchCoordinatorTest {
    @Test
    void waitsForAllSuppliersAndAggregatesAsyncCallbacks() throws Exception {
        final MemoryStore store = new MemoryStore();
        final LocalSearchWaiters waiters = new LocalSearchWaiters();
        final ObjectMapper mapper = new ObjectMapper();
        SupplierTaskDispatcher dispatcher = (searchKey, suppliers, request) -> {
            new Thread(() -> {
                try {
                    for (String supplier : suppliers) {
                        SupplierCallbackRequest callback = callback(searchKey, supplier, supplier + "-quote", true);
                        store.recordCallback(searchKey, supplier, mapper.writeValueAsString(callback), true, 60);
                    }
                    waiters.signal(searchKey);
                } catch (Exception failure) { throw new RuntimeException(failure); }
            }).start();
        };
        AsyncSearchCoordinator coordinator = new AsyncSearchCoordinator(store, dispatcher, waiters, mapper, 60);
        PolicySearchRequest request = request(1000, "A", "B");

        PolicySearchResponse response = coordinator.search(request);

        assertEquals(SearchState.COMPLETED.name(), response.getState());
        assertEquals(2, response.getResults().size());
    }

    @Test
    void timeoutReturnsPartialResultsAndRejectsLateCallbacks() throws Exception {
        final MemoryStore store = new MemoryStore();
        final ObjectMapper mapper = new ObjectMapper();
        SupplierTaskDispatcher dispatcher = (searchKey, suppliers, request) -> {
            try {
                SupplierCallbackRequest partial = callback(searchKey, "A", "A-quote", true);
                store.recordCallback(searchKey, "A", mapper.writeValueAsString(partial), true, 60);
            } catch (Exception failure) { throw new RuntimeException(failure); }
        };
        AsyncSearchCoordinator coordinator = new AsyncSearchCoordinator(
                store, dispatcher, new LocalSearchWaiters(), mapper, 60);

        PolicySearchResponse response = coordinator.search(request(30, "A", "B"));

        assertEquals(SearchState.TIMED_OUT.name(), response.getState());
        assertEquals(1, response.getResults().size());
    }

    private static PolicySearchRequest request(long timeout, String... suppliers) {
        PolicySearchRequest request = new PolicySearchRequest();
        request.setCriteria("demo");
        request.setSupplierIds(Arrays.asList(suppliers));
        request.setTotalTimeoutMillis(timeout);
        return request;
    }

    private static SupplierCallbackRequest callback(String key, String supplier, String result, boolean done) {
        SupplierCallbackRequest request = new SupplierCallbackRequest();
        request.setSearchKey(key);
        request.setSupplierId(supplier);
        request.setResults(Collections.singletonList(result));
        request.setSearchFinished(done);
        return request;
    }

    private static final class MemoryStore implements SearchStateStore {
        private SearchState state;
        private final Set<String> pending = new HashSet<String>();
        private final List<String> results = new ArrayList<String>();
        @Override public synchronized void initialize(String key, Set<String> suppliers, long ttl) {
            pending.addAll(suppliers);
            state = suppliers.isEmpty() ? SearchState.COMPLETED : SearchState.WAITING;
        }
        @Override public synchronized SearchState getState(String key) { return state; }
        @Override public synchronized List<String> getResultPayloads(String key) {
            return new ArrayList<String>(results);
        }
        @Override public synchronized void recordCallback(String key, String supplier, String payload,
                                                           boolean finished, long ttl) {
            if (state != SearchState.WAITING) return;
            if (!payload.isEmpty()) results.add(payload);
            if (finished) pending.remove(supplier);
            if (pending.isEmpty()) state = SearchState.COMPLETED;
        }
        @Override public synchronized void markTimedOut(String key, long ttl) {
            if (state == SearchState.WAITING) state = SearchState.TIMED_OUT;
        }
    }
}
