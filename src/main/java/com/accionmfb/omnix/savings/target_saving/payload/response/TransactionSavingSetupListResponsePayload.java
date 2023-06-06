package com.accionmfb.omnix.savings.target_saving.payload.response;

import com.accionmfb.omnix.savings.target_saving.model.TransactionSavingSetup;
import lombok.Data;

import java.util.List;

@Data
public class TransactionSavingSetupListResponsePayload
{
    private String responseCode;
    private String responseMessage;
    private List<TransactionSavingSetup> data;
}
