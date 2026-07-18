package com.arch.policy.api;

import java.util.concurrent.CompletableFuture;

public interface PolicySearchRpcService {
    CompletableFuture<PolicySearchResponse> asyncSearch(PolicySearchRequest request);
}
