package com.accionmfb.omnix.savings.target_saving.constant;

public class ApiPaths
{
    // A
    public static final String ACCOUNT_DETAILS = "/details";

    public static final String ACCOUNT_BALANCE = "/balance";
    //C
    public static final String CUSTOMER_DETAILS = "/details";

    public static final String HEADER_STRING = "Authorization";

    // T
    public static final String TOKEN_PREFIX = "Bearer";
    public static final String TARGET_SAVINGS_SET = "/goal/set";
    public static final String TARGET_SAVINGS_TERMINATION = "/goal/terminate";
    public static final String TARGET_SAVINGS_EXECUTE = "/goal/execute";
    public static final String TARGET_SAVINGS_MISSED = "/goal/missed";
    public static final String TARGET_SAVINGS_LIST = "/goal/list";

    public static final String TARGET_SAVINGS_ALL = "/goal/all";

    public static final String TARGET_SAVINGS_DETAILS = "/goal/details";
    public static final String TARGET_SAVINGS_SCHEDULE_LIST = "/goal/schedule";
    public static final String TARGET_SAVINGS_UPDATE = "/goal/update";
    public static final String TARGET_SAVINGS_INTEREST_SIMULATOR = "/goal/interest/simulate";


    // PROXY
    public static final String PROXY_CONTROLLER_BASE_URL = "/proxy";
}
