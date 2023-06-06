package com.accionmfb.omnix.savings.target_saving.payload.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalanceRequestPayload
{
    private String accountNumber;
    private String requestId;
    private String hash;
    private String imei;
}
