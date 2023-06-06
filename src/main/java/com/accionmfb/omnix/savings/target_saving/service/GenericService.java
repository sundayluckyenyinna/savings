package com.accionmfb.omnix.savings.target_saving.service;

import com.accionmfb.omnix.savings.target_saving.model.TargetSavings;
import com.accionmfb.omnix.savings.target_saving.payload.request.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;


@Service
public interface GenericService
{
    public String generateTransRef(String transType);

    public String decryptString(String textToDecrypt, String encryptionKey);

    String encryptString(String textToEncrypt, String token);

    String encryptPayloadToString(Object payload, String token);

    String getJoinedPayloadValues(Object payload);

    String encryptFundTransferPayload(FundsTransferPayload fundsTransferPayload, String token);

    char getTimePeriod();

    double[] calculateInterestComponentsForTargetSavings(
            String contributionAmount, String interestRate,
            String frequency, String tenor
    );

    String getMessageOfAccount(String suffix);

    String getMessageOfAccount(String suffix, Object[] parameters);
    String getMessageOfCustomer(String suffix);

    String getMessageOfFunds(String suffix);

    String getMessageOfFunds(String suffix, Object[] parameters);
    String getMessageOfCustomer(String suffix, Object[] parameters);

    String getMessageOfNotification(String suffix);

    String getMessageOfNotification (String suffix, Object[] parameters);

    String getMessageOfRequest(String suffix);

    String getMessageOfRequest(String suffix, Object[] parameters);
    public String getMessageOfTargetSavings(String suffix);

    public String getMessageOfTargetSavings(String suffix, Object[] parameters);

    CustomerDetailsRequestPayload
    createCustomerDetailsRequestPayloadFromTargetSavingsRequest
            (TargetSavingsRequestPayload targetSavingsRequestPayload, String token);

    AccountDetailsRequestPayload
    createAccountDetailsRequestPayloadFromTargetSavingsRequest
            (TargetSavingsRequestPayload targetSavingsRequestPayload, String token);


    AccountBalanceRequestPayload
    createAccountBalanceRequestPayloadFromTargetSavingsRequest
            (TargetSavingsRequestPayload targetSavingsRequestPayload, String token);

    void generateLog(String app, String token, String logMessage, String logType, String logLevel, String requestId);

    LocalDateTime getCurrentDateTime();
}
