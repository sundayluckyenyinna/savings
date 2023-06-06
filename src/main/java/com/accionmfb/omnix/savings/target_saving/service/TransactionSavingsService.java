package com.accionmfb.omnix.savings.target_saving.service;

import com.accionmfb.omnix.savings.target_saving.dto.Response;
import com.accionmfb.omnix.savings.target_saving.payload.request.TransactionSavingSetupRequestPayload;
import org.springframework.stereotype.Service;

@Service
public interface TransactionSavingsService
{
    Response processTransactionSavingSetupSaveOrUpdate(TransactionSavingSetupRequestPayload requestPayload, String token);
    Response processGetTransactionSavingSetup(String accountNumber, String transactionType);
    Response processTerminateTransactionSavingSetup(String accountNumber, String transactionType);
}
