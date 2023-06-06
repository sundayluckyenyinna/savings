package com.accionmfb.omnix.savings.target_saving.payload.response;

import com.accionmfb.omnix.savings.target_saving.model.TransactionSavingSetup;
import lombok.Data;


@Data
public class TransactionSavingsSetupResponsePayload
{
    private String responseCode;
    private String responseMessage;
    private TransactionSavingSetup data;
}
