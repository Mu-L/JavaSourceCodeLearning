package com.arch.policy.book.application;

public final class GdsBookingResult {
    private final ExternalResultStatus status;
    private final String pnr;
    private final String errorCode;

    public GdsBookingResult(ExternalResultStatus status, String pnr, String errorCode) {
        this.status = status;
        this.pnr = pnr;
        this.errorCode = errorCode;
    }

    public static GdsBookingResult success(String pnr) {
        return new GdsBookingResult(ExternalResultStatus.SUCCESS, pnr, null);
    }

    public static GdsBookingResult fail(String errorCode) {
        return new GdsBookingResult(ExternalResultStatus.FAIL, null, errorCode);
    }

    public static GdsBookingResult unknown(String errorCode) {
        return new GdsBookingResult(ExternalResultStatus.UNKNOWN, null, errorCode);
    }

    public ExternalResultStatus getStatus() { return status; }
    public String getPnr() { return pnr; }
    public String getErrorCode() { return errorCode; }
}
