package com.accionmfb.omnix.savings.target_saving.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalanceResponsePayload
{
    private String availableBalance;
    private String ledgerBalance;
    private String accountNumber;
    private String responseCode;
    private String responseMessage;
}
