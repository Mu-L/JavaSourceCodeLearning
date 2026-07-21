package com.arch.policy.book.infrastructure.demo;

import com.arch.policy.book.application.GdsBookingGateway;
import com.arch.policy.book.application.GdsBookingResult;

public final class DemoGdsBookingGateway implements GdsBookingGateway {
    @Override public GdsBookingResult createPnr(String orderNo) {
        return GdsBookingResult.success("PNR-" + orderNo.substring(Math.max(0, orderNo.length() - 6)));
    }

    @Override public GdsBookingResult queryPnr(String orderNo) {
        return createPnr(orderNo);
    }

    @Override public void cancelPnr(String orderNo, String childOrderNo, String pnr) {
        // Demo adapter: production uses a GDS RPC client with the same idempotency key.
    }
}
