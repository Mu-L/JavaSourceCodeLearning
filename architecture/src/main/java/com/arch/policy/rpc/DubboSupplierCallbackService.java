package com.arch.policy.rpc;

import com.arch.policy.aggregation.SupplierCallbackService;
import com.arch.policy.api.CallbackResponse;
import com.arch.policy.api.SupplierCallbackRequest;
import com.arch.policy.api.SupplierCallbackRpcService;
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
