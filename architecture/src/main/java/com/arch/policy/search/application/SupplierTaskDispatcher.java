package com.arch.policy.search.application;

import com.arch.policy.common.search.PolicySearchRequest;

import java.util.Set;

public interface SupplierTaskDispatcher {
    void dispatch(String searchKey, Set<String> supplierIds, PolicySearchRequest request);
}
