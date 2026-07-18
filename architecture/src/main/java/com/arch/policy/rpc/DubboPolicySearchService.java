package com.arch.policy.rpc;

import com.arch.policy.api.PolicySearchRequest;
import com.arch.policy.api.PolicySearchResponse;
import com.arch.policy.api.PolicySearchRpcService;
import com.arch.policy.aggregation.AsyncSearchCoordinator;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@DubboService(version = "1.0.0", timeout = 3000)
public final class DubboPolicySearchService implements PolicySearchRpcService {
    private final AsyncSearchCoordinator searchService;
    private final Executor searchExecutor;

    public DubboPolicySearchService(AsyncSearchCoordinator searchService,
                                    @Qualifier("policySearchExecutor") Executor searchExecutor) {
        this.searchService = searchService;
        this.searchExecutor = searchExecutor;
    }

    @Override public CompletableFuture<PolicySearchResponse> asyncSearch(final PolicySearchRequest request) {
        final CompletableFuture<PolicySearchResponse> future = new CompletableFuture<PolicySearchResponse>();
        searchExecutor.execute(new Runnable() {
            @Override public void run() {
                try { future.complete(searchService.search(request)); }
                catch (Exception failure) { future.completeExceptionally(failure); }
            }
        });
        return future;
    }
}
