package com.accionmfb.omnix.savings.target_saving.constant;

public enum TargetSavingStatus
{
    SUCCESS,      // The goal was a success.
    PENDING,        // The goal is still pending.
    MISSED,         // The goal is missed.
    TERMINATED,     // The goal is terminated by the user.
    EXPIRED,         // The goal has expired as terminated by the system.
    FAILED          // The goal failed while trying to be exeuted
}
