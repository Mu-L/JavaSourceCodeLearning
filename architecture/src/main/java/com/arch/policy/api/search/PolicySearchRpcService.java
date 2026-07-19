package com.arch.policy.api.search;

import com.arch.policy.common.search.PolicySearchRequest;
import com.arch.policy.common.search.PolicySearchResponse;

import java.util.concurrent.CompletableFuture;

public interface PolicySearchRpcService {
    CompletableFuture<PolicySearchResponse> asyncSearch(PolicySearchRequest request);
}
