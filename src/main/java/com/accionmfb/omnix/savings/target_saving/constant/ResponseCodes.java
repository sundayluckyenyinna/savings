package com.accionmfb.omnix.savings.target_saving.constant;

public enum ResponseCodes {

    SUCCESS_CODE("00"),
    IP_BANNED_CODE("01"),
    RECORD_EXIST_CODE("02"),
    RECORD_NOT_EXIST_CODE("03"),
    RECORD_INUSE_CODE("04"),
    REQUEST_PROCESSING("05"),
    FAILED_TRANSACTION("06"),
    FAILED_MODEL("07"),
    FORMAT_EXCEPTION("08"),
    ID_EXPIRED("09"),
    CUSTOMER_DISABLED("10"),
    PASSWORD_PIN_MISMATCH("11"),
    CUSTOMER_BOARDED("12"),
    CUSTOMER_TIER_REACHED("13"),
    CUSTOMER_NUMBER_MISSING("14"),
    CUSTOMER_NUMBER_MOBILE_MISMATCH("15"),
    MOBILE_NUMBER_CUSTOMER_NAME_MISMATCH("16"),
    ACCOUNT_CUSTOMER_MISMATCH("17"),
    IRREVERSIBLE_TRANSACTION("18"),
    NO_PRIMARY_ACCOUNT("19"),
    INSUFFICIENT_BALANCE("20"),
    NO_CALLBACK_SERVICE("21"),
    SERVICE_UNAVAILABLE("22"),
    CORRUPT_DATA("23"),
    SAME_ACCOUNT("24"),
    ACTIVE_LOAN_EXIST("25"),
    OUT_OF_RANGE("26"),
    NAME_MISMATCH("27"),
    INVALID_TYPE("28"),
    DUPLICATE_CUSTOMER_RECORD("29"),
    OTP_REQUIRED("30"),
    OTP_INVALID("31"),
    NO_ROLE("32"),
    TRANSACTION_BRANCH_MISMATCH("33"),
    LIMIT_EXCEEDED("34"),
    LOAN_DECLINED("35"),
    LOAN_CUSTOMER_MISMATCH("36"),
    INVALID_TOKEN("37"),
    INTERNAL_SERVER_ERROR("99"),
    BAD_REQUEST("400"),
    REVERSE_CODE("98");

    private String responseCode;

    public String getResponseCode() {
        return this.responseCode;
    }

    ResponseCodes(String responseCode) {
        this.responseCode = responseCode;
    }
}
