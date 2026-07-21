package com.arch.policy.book.application;

public final class GdsReconciliationService implements OrderReconciler {
    private final GdsBookingGateway gdsGateway;
    private final OrderCreationSagaStateServices sagaStates;

    public GdsReconciliationService(GdsBookingGateway gdsGateway,
                                    OrderCreationSagaStateServices sagaStates) {
        this.gdsGateway = gdsGateway;
        this.sagaStates = sagaStates;
    }

    @Override public void reconcile(String orderNo) {
        GdsBookingResult result = gdsGateway.queryPnr(orderNo);
        if (result.getStatus() == ExternalResultStatus.SUCCESS) {
            sagaStates.markGdsValidationSuccess(orderNo, result.getPnr());
            return;
        }
        if (result.getStatus() == ExternalResultStatus.FAIL) {
            sagaStates.markGdsValidationFailure(orderNo, result.getErrorCode());
            return;
        }
        throw new IllegalStateException("GDS result is still unknown");
    }
}
