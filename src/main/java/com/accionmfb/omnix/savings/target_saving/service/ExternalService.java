package com.accionmfb.omnix.savings.target_saving.service;

import com.accionmfb.omnix.savings.target_saving.payload.request.*;
import com.accionmfb.omnix.savings.target_saving.payload.response.*;
import org.springframework.stereotype.Service;

/**
 * This class provides the necessary object data obtained from the remote intra micro-service
 * communication.
 */

@Service
public interface ExternalService
{
    CustomerDetailsResponsePayload
    getCustomerDetailsFromCustomerService(
            TargetSavingsRequestPayload targetSavingsRequestPayload, String token
    );

    CustomerDetailsResponsePayload
    getCustomerDetailsFromCustomerService(CustomerDetailsRequestPayload requestPayload, String token);

    String getCustomerStatus(TargetSavingsRequestPayload targetSavingsRequestPayload, String token);

    AccountDetailsResponsePayload
    getAccountDetailsFromAccountService(
            TargetSavingsRequestPayload requestPayload, String token
    );

    AccountDetailsResponsePayload
    getAccountDetailsFromAccountService(
            AccountDetailsRequestPayload requestPayload, String token
    );

    String getAccountStatus(TargetSavingsRequestPayload requestPayload, String token);

    AccountBalanceResponsePayload getAccountBalanceFromAccountService(
            TargetSavingsRequestPayload requestPayload,
            String token
    );

    AccountBalanceResponsePayload getAccountBalanceFromAccountService(
            AccountBalanceRequestPayload accountBalanceRequestPayload,
            String token
    );

    FundsTransferResponsePayload getFundsTransferResponse(String authToken, String requestJson);

    SMSResponsePayload sendSMS(
            String authToken, SMSRequestPayload requestPayload
    );

    TargetSavingsResponsePayload terminateTargetSavingsInternal(
            String authToken,
            TargetSavingTerminationRequestPayload requestPayload
    );
}
