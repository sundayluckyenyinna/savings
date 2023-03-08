package com.accionmfb.omnix.savings.target_saving.constant;

public enum CustomerStatus
{
    ACTIVE,         // After a successful search from the customer service
    UNKNOWN,        // If the customer status has not been set from the customer service.
    UNAVAILABLE     // After failing to even get to the customer service.

}
