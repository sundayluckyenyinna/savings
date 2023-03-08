package com.accionmfb.omnix.savings.target_saving.service;

import com.accionmfb.omnix.savings.target_saving.constant.AccountStatus;
import com.accionmfb.omnix.savings.target_saving.constant.CustomerStatus;
import com.accionmfb.omnix.savings.target_saving.constant.ResponseCodes;
import com.accionmfb.omnix.savings.target_saving.payload.request.*;
import com.accionmfb.omnix.savings.target_saving.payload.response.*;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * This class provides the core implementation for the external service invocation and
 * all necessary handshake required for external data transformation.
 */

@Service
public class ExternalServiceImpl implements ExternalService
{
    @Autowired
    private CustomerService customerService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private GenericService genericService;

    @Autowired
    private FundsTransferService fundsTransferService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TerminationService terminationService;

    @Autowired
    private Gson gson;


    /** CUSTOMER RELATED SERVICE INVOCATION */

    /**
     * This method returns the details of the customer that is requesting for the setting of a Target Saving
     * goal. If the customer does not exist, an empty object is returned with all fields as null.
     * Therefore, this method can never return 'null'.
     * The only way to check if the customer service is actually reached is to check that the response code
     * is not of the value associated with SERVICE_UNAVAILABLE. Other response code is from the Customer
     * microservice.
     * @param targetSavingsRequestPayload
     * @param token
     * @return customerDetails : CustomerDetailsResponsePayload
     */
    @Override
    public CustomerDetailsResponsePayload
    getCustomerDetailsFromCustomerService
    (TargetSavingsRequestPayload targetSavingsRequestPayload, String token)
    {

        // Create the request payload.
        CustomerDetailsRequestPayload customerDetailsRequestPayload = genericService
                .createCustomerDetailsRequestPayloadFromTargetSavingsRequest(
                        targetSavingsRequestPayload,
                        token
                );

        // Call the Customer microservice.
        String responseString;
        try{
            responseString = customerService
                    .customerDetails(token, customerDetailsRequestPayload).getBody();
        }catch (Exception exception){
            CustomerDetailsResponsePayload response = new CustomerDetailsResponsePayload();
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            String prefix = "Customer details failure: ";
            response.setResponseMessage(prefix + exception.getMessage());
            return response;
        }


        // If the response is just null, return object with all empty fields, but with code and message.
        if( responseString == null ){
            CustomerDetailsResponsePayload customerDetailsResponsePayload = new CustomerDetailsResponsePayload();
            customerDetailsResponsePayload.setResponseCode(ResponseCodes.SERVICE_UNAVAILABLE.getResponseCode());
            String message = genericService.getMessageOfCustomer("service.unavailable");
            customerDetailsResponsePayload.setResponseMessage(message);
            return customerDetailsResponsePayload;
        }

        return gson.fromJson(responseString, CustomerDetailsResponsePayload.class);
    }


    @Override
    public CustomerDetailsResponsePayload
    getCustomerDetailsFromCustomerService(CustomerDetailsRequestPayload requestPayload, String token)
    {
        // Call the Customer microservice.
        String responseString;
        try{
            responseString = customerService
                    .customerDetails(token, requestPayload).getBody();
        }catch (Exception exception){
            CustomerDetailsResponsePayload response = new CustomerDetailsResponsePayload();
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            String prefix = "Customer details failure: ";
            response.setResponseMessage(prefix + exception.getMessage());
            return response;
        }

        // If the response is just null, return object with all empty fields.
        if( responseString == null ){
            CustomerDetailsResponsePayload customerDetailsResponsePayload = new CustomerDetailsResponsePayload();
            customerDetailsResponsePayload.setResponseCode(ResponseCodes.SERVICE_UNAVAILABLE.getResponseCode());
            String message = genericService.getMessageOfCustomer("service.unavailable");
            customerDetailsResponsePayload.setResponseMessage(message);
            return customerDetailsResponsePayload;
        }

        return gson.fromJson(responseString, CustomerDetailsResponsePayload.class);
    }

    @Override
    public String getCustomerStatus(TargetSavingsRequestPayload targetSavingsRequestPayload, String token){

//         Get the customer details from the customer service.
        CustomerDetailsResponsePayload customerDetails = getCustomerDetailsFromCustomerService(
                targetSavingsRequestPayload,
                token
        );

        String customerStatus;
        String responseCode = customerDetails.getResponseCode();

        // Set customer status if the customer service could not be reached.
        if( responseCode.equalsIgnoreCase(ResponseCodes.SERVICE_UNAVAILABLE.getResponseCode()) )
            customerStatus = CustomerStatus.UNAVAILABLE.name();

        // Set the customer status if the customer status is possibly not set from the customer microservice
        else if(customerDetails.getStatus() == null)
            customerStatus = CustomerStatus.UNKNOWN.name();

        else
            customerStatus = customerDetails.getStatus().toUpperCase();

        return customerStatus;
    }



    /** ACCOUNT RELATED INVOCATION */

    @Override
    public AccountDetailsResponsePayload getAccountDetailsFromAccountService(
            TargetSavingsRequestPayload requestPayload, String token
    ){
        AccountDetailsResponsePayload responsePayload;

        AccountDetailsRequestPayload accountDetailsRequestPayload =
                genericService.createAccountDetailsRequestPayloadFromTargetSavingsRequest(
                        requestPayload, token
                );

        String responseString;
        try{
            responseString = accountService.accountDetails(token, accountDetailsRequestPayload)
                    .getBody();
        }catch (Exception exception){
            responsePayload = new AccountDetailsResponsePayload();
            responsePayload.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            String prefix = "Account details retrieval failure: ";
            responsePayload.setResponseMessage(prefix + exception.getMessage());
            return responsePayload;
        }

        if (responseString == null){
            responsePayload = new AccountDetailsResponsePayload();
            responsePayload.setResponseCode(ResponseCodes.SERVICE_UNAVAILABLE.getResponseCode());
            String message = genericService.getMessageOfAccount("service.unavailable");
            responsePayload.setResponseMessage(message);
            return responsePayload;
        }

        responsePayload = gson.fromJson(responseString, AccountDetailsResponsePayload.class);
        return responsePayload;
    }

    @Override
    public AccountDetailsResponsePayload
    getAccountDetailsFromAccountService(AccountDetailsRequestPayload accountDetailsRequestPayload, String token)
    {
        AccountDetailsResponsePayload responsePayload;

        String responseString;
        try{
            responseString = accountService.accountDetails(token, accountDetailsRequestPayload)
                    .getBody();
        }catch (Exception exception){
            responsePayload = new AccountDetailsResponsePayload();
            responsePayload.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            String prefix = "Account details retrieval failure: ";
            responsePayload.setResponseMessage(prefix + exception.getMessage());
            return responsePayload;
        }

        if (responseString == null){
            responsePayload = new AccountDetailsResponsePayload();
            responsePayload.setResponseCode(ResponseCodes.SERVICE_UNAVAILABLE.getResponseCode());
            String message = genericService.getMessageOfAccount("service.unavailable");
            responsePayload.setResponseMessage(message);
            return responsePayload;
        }

        responsePayload = gson.fromJson(responseString, AccountDetailsResponsePayload.class);
        return responsePayload;
    }

    @Override
    public AccountBalanceResponsePayload
    getAccountBalanceFromAccountService(TargetSavingsRequestPayload requestPayload, String token){
        AccountBalanceRequestPayload payload = genericService
                .createAccountBalanceRequestPayloadFromTargetSavingsRequest(requestPayload, token);

        String responseJson;
        try{
              responseJson = accountService.accountBalance(token, payload).getBody();
        }catch (Exception exception){
            AccountBalanceResponsePayload response = new AccountBalanceResponsePayload();
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            String prefix = "Account balance retrieval failure: ";
            response.setResponseMessage(prefix + exception.getMessage());
            return response;
        }

        AccountBalanceResponsePayload responsePayload = new AccountBalanceResponsePayload();

        if(responseJson == null){
            responsePayload.setResponseCode(ResponseCodes.SERVICE_UNAVAILABLE.getResponseCode());
            String message = genericService.getMessageOfAccount("service.unavailable");
            responsePayload.setResponseMessage(message);
            return responsePayload;
        }

        responsePayload = gson.fromJson(responseJson, AccountBalanceResponsePayload.class);

        return responsePayload;
    }

    @Override
    public AccountBalanceResponsePayload getAccountBalanceFromAccountService(
            AccountBalanceRequestPayload payload,
            String token
    )
    {

        String responseJson;
        try{
            responseJson = accountService.accountBalance(token, payload).getBody();
        }catch (Exception exception){
            AccountBalanceResponsePayload response = new AccountBalanceResponsePayload();
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            String prefix = "Account balance retrieval failure: ";
            response.setResponseMessage(prefix + exception.getMessage());
            return response;
        }

        AccountBalanceResponsePayload responsePayload = new AccountBalanceResponsePayload();

        if(responseJson == null){
            responsePayload.setResponseCode(ResponseCodes.SERVICE_UNAVAILABLE.getResponseCode());
            String message = genericService.getMessageOfAccount("service.unavailable");
            responsePayload.setResponseMessage(message);
            return responsePayload;
        }

        responsePayload = gson.fromJson(responseJson, AccountBalanceResponsePayload.class);

        return responsePayload;
    }

    @Override
    public String getAccountStatus(TargetSavingsRequestPayload requestPayload, String token){
        String accountStatus;

        AccountDetailsResponsePayload responsePayload = getAccountDetailsFromAccountService(
                requestPayload, token
        );

        String responseCode = responsePayload.getResponseCode();

        if( responseCode.equalsIgnoreCase(ResponseCodes.SERVICE_UNAVAILABLE.getResponseCode()) )
            accountStatus = AccountStatus.UNAVAILABLE.name();
        else if(responsePayload.getStatus() == null)
            accountStatus = AccountStatus.UNKNOWN.name();
        else
            accountStatus = responsePayload.getStatus().toUpperCase();

        return accountStatus;
    }

    /**
     * This interfaces with the call of the funds transfer microservice. Note that the return
     * of this method can never be null.
     * @param authToken
     * @param requestJson
     * @return
     */
    @Override
    public FundsTransferResponsePayload
    getFundsTransferResponse(String authToken, String requestJson)
    {

        FundsTransferResponsePayload result = new FundsTransferResponsePayload();

        String fundsTransferResponse;

        try{
            fundsTransferResponse = fundsTransferService
                    .localTransfer(authToken, requestJson).getBody();
        }catch (Exception exception){
            result.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            String prefix = "Funds transfer error: ";
            result.setResponseMessage(prefix + exception.getMessage());
            return result;
        }

        // Check that there is a response from the service and that it is not down.
        if ( fundsTransferResponse == null ){
            result.setResponseCode(ResponseCodes.SERVICE_UNAVAILABLE.getResponseCode());
            String message = genericService.getMessageOfFunds("service.unavailable");
            result.setResponseMessage(message);
            return result;
        }

        FundsTransferResponsePayload responsePayload = gson.fromJson(
                fundsTransferResponse, FundsTransferResponsePayload.class
        );

        return responsePayload;
    }


    @Override
    public SMSResponsePayload sendSMS(String authToken, SMSRequestPayload requestPayload
    )
    {
        SMSResponsePayload smsResponsePayload = new SMSResponsePayload();

        // Call the notification service
        String responseJson;

        try{
            responseJson = notificationService.smsNotification(authToken, requestPayload);
        }catch (Exception exception){
            smsResponsePayload.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            String prefix = "SMS notification failure: ";
            smsResponsePayload.setResponseMessage(prefix + exception.getMessage());
            return smsResponsePayload;
        }

        if(responseJson == null)
        {
            smsResponsePayload.setResponseCode(ResponseCodes.SERVICE_UNAVAILABLE.getResponseCode());
            String message = genericService.getMessageOfNotification("service.unavailable");
            smsResponsePayload.setResponseMessage(message);
            return smsResponsePayload;
        }

        return gson.fromJson(responseJson, SMSResponsePayload.class);
    }

    public TargetSavingsResponsePayload terminateTargetSavingsInternal(
            String authToken,
            TargetSavingTerminationRequestPayload requestPayload
    )
    {
        TargetSavingsResponsePayload response = new TargetSavingsResponsePayload();

        String terminationResponse;

         try {
             terminationResponse = terminationService
                     .terminateTargetSavings(authToken, requestPayload)
                     .getBody();
         }catch (Exception exception){
             response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
             String prefix = "Termination service failure: ";
             response.setResponseMessage(prefix + exception.getMessage());
             return response;
         }

        // No need to check for null value. The request will route back to this same savings service.
        response = gson.fromJson(terminationResponse, TargetSavingsResponsePayload.class);
        return response;
    }

}
