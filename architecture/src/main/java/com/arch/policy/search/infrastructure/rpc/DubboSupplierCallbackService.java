package com.arch.policy.search.infrastructure.rpc;

import com.arch.policy.search.application.SupplierCallbackService;
import com.arch.policy.common.search.CallbackResponse;
import com.arch.policy.common.search.SupplierCallbackRequest;
import com.arch.policy.api.search.SupplierCallbackRpcService;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService(version = "1.0.0", timeout = 3000)
public final class DubboSupplierCallbackService implements SupplierCallbackRpcService {
    private final SupplierCallbackService callbackService;
    public DubboSupplierCallbackService(SupplierCallbackService callbackService) {
        this.callbackService = callbackService;
    }
    @Override public CallbackResponse callback(SupplierCallbackRequest request) {
        try {
            callbackService.callback(request);
            return new CallbackResponse(true);
        } catch (Exception failure) {
            throw new IllegalStateException("supplier callback failed", failure);
        }
    }
}
