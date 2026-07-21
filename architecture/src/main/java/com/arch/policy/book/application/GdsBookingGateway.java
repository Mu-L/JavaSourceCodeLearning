package com.arch.policy.book.application;

public interface GdsBookingGateway {
    GdsBookingResult createPnr(String orderNo);

    GdsBookingResult queryPnr(String orderNo);

    void cancelPnr(String orderNo, String childOrderNo, String pnr);
}
