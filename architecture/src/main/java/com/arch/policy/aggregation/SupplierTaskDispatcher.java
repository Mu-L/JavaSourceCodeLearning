package com.arch.policy.aggregation;

import com.arch.policy.api.PolicySearchRequest;

import java.util.Set;

public interface SupplierTaskDispatcher {
    void dispatch(String searchKey, Set<String> supplierIds, PolicySearchRequest request);
}
