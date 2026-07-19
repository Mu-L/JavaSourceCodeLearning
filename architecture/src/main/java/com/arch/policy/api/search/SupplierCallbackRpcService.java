package com.arch.policy.api.search;

import com.arch.policy.common.search.CallbackResponse;
import com.arch.policy.common.search.SupplierCallbackRequest;

public interface SupplierCallbackRpcService {
    CallbackResponse callback(SupplierCallbackRequest request);
}
