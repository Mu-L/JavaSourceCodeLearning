package com.arch.policy.book.application;

public final class DirectOrderCreationSaga implements OrderCreationSaga {
    private final OrderCreationSagaStateServices services;

    public DirectOrderCreationSaga(OrderCreationSagaStateServices services) { this.services = services; }

    @Override public SagaStartResult start(String orderNo, String promotionId) {
        GdsBookingResult result;
        try {
            result = services.reservePnr(orderNo);
        } catch (RuntimeException uncertainFailure) {
            result = GdsBookingResult.unknown(uncertainFailure.getClass().getSimpleName());
        }
        if (result.getStatus() == ExternalResultStatus.SUCCESS) {
            services.markCreateSuccess(orderNo, result.getPnr());
        } else if (result.getStatus() == ExternalResultStatus.FAIL) {
            services.markCreateFailure(orderNo, result.getErrorCode(), promotionId);
        } else {
            services.markCreateUnknown(orderNo, result.getErrorCode());
        }
        return new SagaStartResult("DIRECT:" + orderNo, false);
    }
}
