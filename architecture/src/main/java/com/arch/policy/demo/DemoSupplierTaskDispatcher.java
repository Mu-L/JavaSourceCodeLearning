package com.arch.policy.demo;

import com.arch.policy.aggregation.SupplierCallbackService;
import com.arch.policy.aggregation.SupplierTaskDispatcher;
import com.arch.policy.api.PolicySearchRequest;
import com.arch.policy.api.SupplierCallbackRequest;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Simulates downstream suppliers asynchronously calling this module back. */
public final class DemoSupplierTaskDispatcher implements SupplierTaskDispatcher {
    private final ScheduledExecutorService executor;
    private final SupplierCallbackService callbackService;

    public DemoSupplierTaskDispatcher(ScheduledExecutorService executor,
                                      SupplierCallbackService callbackService) {
        this.executor = executor;
        this.callbackService = callbackService;
    }

    @Override public void dispatch(final String searchKey, Set<String> supplierIds,
                                   final PolicySearchRequest request) {
        int delay = 20;
        for (final String supplierId : supplierIds) {
            executor.schedule(new Runnable() {
                @Override public void run() {
                    try {
                        SupplierCallbackRequest callback = new SupplierCallbackRequest();
                        callback.setSearchKey(searchKey);
                        callback.setSupplierId(supplierId);
                        callback.setResults(Collections.singletonList(
                                supplierId + " quote for " + request.getCriteria()));
                        callback.setSearchFinished(true);
                        callbackService.callback(callback);
                    } catch (Exception failure) {
                        try { callbackService.markFinishedWithoutResult(searchKey, supplierId); }
                        catch (Exception ignored) { /* broker retry/alert belongs in a real adapter */ }
                    }
                }
            }, delay, TimeUnit.MILLISECONDS);
            delay += 20;
        }
    }
}
