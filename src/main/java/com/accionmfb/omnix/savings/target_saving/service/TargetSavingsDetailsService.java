package com.accionmfb.omnix.savings.target_saving.service;

import com.accionmfb.omnix.savings.target_saving.constant.ResponseCodes;
import com.accionmfb.omnix.savings.target_saving.dto.ErrorResponse;
import com.accionmfb.omnix.savings.target_saving.dto.PayloadResponse;
import com.accionmfb.omnix.savings.target_saving.dto.Response;
import com.accionmfb.omnix.savings.target_saving.jwt.JwtTokenUtil;
import com.accionmfb.omnix.savings.target_saving.model.TargetSavingSchedule;
import com.accionmfb.omnix.savings.target_saving.model.TargetSavings;
import com.accionmfb.omnix.savings.target_saving.payload.request.AccountBalanceRequestPayload;
import com.accionmfb.omnix.savings.target_saving.payload.request.AccountDetailsRequestPayload;
import com.accionmfb.omnix.savings.target_saving.payload.request.CustomerDetailsRequestPayload;
import com.accionmfb.omnix.savings.target_saving.payload.request.TargetSavingsDetailsRequestPayload;
import com.accionmfb.omnix.savings.target_saving.payload.response.AccountBalanceResponsePayload;
import com.accionmfb.omnix.savings.target_saving.payload.response.AccountDetailsResponsePayload;
import com.accionmfb.omnix.savings.target_saving.payload.response.CustomerDetailsResponsePayload;
import com.accionmfb.omnix.savings.target_saving.payload.response.TargetSavingsResponsePayload;
import com.accionmfb.omnix.savings.target_saving.repository.TargetSavingsRepository;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
public class TargetSavingsDetailsService
{
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private GenericService genericService;

    @Autowired
    private ExternalService externalService;

    @Autowired
    private Gson gson;

    @Autowired
    private TargetSavingsRepository targetSavingsRepository;


    public Response
    processTargetSavingsDetails(TargetSavingsDetailsRequestPayload requestPayload, String token)
    {

        Response response;

        String username = jwtTokenUtil.getUsernameFromToken(token);
        String channel = jwtTokenUtil.getChannelFromToken(token);

        // Log the request to the console
        genericService.generateLog("Target savings Details retrieval", token, username + " : " + channel, "API Request", "INFO", requestPayload.getRequestId());

        // Get the parent target savings
        String goalName = requestPayload.getGoalName();
        String accountNumber = requestPayload.getAccountNumber();

        Long targetSavingsId = null;

        if(requestPayload.getTargetSavingsId() != null){
            if(!requestPayload.getTargetSavingsId().matches("[0-9]{1,}")){
                Response errorResponse = ErrorResponse.getInstance();
                String message = "Numerical target savings Id required";
                errorResponse.setResponseCode(ResponseCodes.FORMAT_EXCEPTION.getResponseCode());
                errorResponse.setResponseMessage(message);
                // Log the error
                genericService.generateLog("Target savings Details retrieval", token, message, "API Request", "DEBUG", requestPayload.getRequestId());
                return errorResponse;
            }
            targetSavingsId = Long.valueOf(requestPayload.getTargetSavingsId());
        }

        Optional<TargetSavings> targetSavings;

        if(goalName != null) {
            if(accountNumber == null){
                response = ErrorResponse.getInstance();
                response.setResponseCode(ResponseCodes.FAILED_MODEL.getResponseCode());
                String message = genericService
                        .getMessageOfTargetSavings("accountnumbermissingforgoalname");
                response.setResponseMessage(message);
                // Log the error
                genericService.generateLog("Target savings Details retrieval", token, message, "API Request", "DEBUG", requestPayload.getRequestId());
                return response;
            }
            targetSavings = targetSavingsRepository
                    .findTargetSavingsByGoalNameAndAccountNumber(goalName, accountNumber);
        }
        else
            targetSavings = targetSavingsRepository.findTargetSavingsById(targetSavingsId);

        Optional<ErrorResponse> errorResponseOptional = handleAllErrors
                (requestPayload, targetSavings);
        if (errorResponseOptional.isPresent()){
            // Log the error
            genericService.generateLog("Target savings Details retrieval", token, errorResponseOptional.get().getResponseMessage(), "API Request", "INFO", requestPayload.getRequestId());
            return errorResponseOptional.get();
        }


        // Get the account and the customer Details
        AccountDetailsRequestPayload accountRequest = new AccountDetailsRequestPayload();
        accountRequest.setAccountNumber(targetSavings.get().getAccountNumber());
        accountRequest.setRequestId(requestPayload.getRequestId());
        String hash = genericService.encryptPayloadToString(accountRequest, token);
        accountRequest.setHash(hash);

        AccountDetailsResponsePayload accountDetails = externalService
                .getAccountDetailsFromAccountService(accountRequest, token);
        String accountResponseCode = accountDetails.getResponseCode();

        CustomerDetailsRequestPayload customerRequest = new CustomerDetailsRequestPayload();
        customerRequest.setMobileNumber(accountDetails.getMobileNumber());
        customerRequest.setRequestId(requestPayload.getRequestId());
        String hash1 = genericService.encryptPayloadToString(customerRequest, token);
        customerRequest.setHash(hash1);

        CustomerDetailsResponsePayload customerDetails = externalService
                .getCustomerDetailsFromCustomerService(customerRequest, token);
        String customerCode = customerDetails.getResponseCode();

        String prefix;

        // Do validation of responses
        if(!accountResponseCode.equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())){
            response = ErrorResponse.getInstance();
            response.setResponseCode(accountResponseCode);
            prefix = "Account details retrieval failure: ";
            response.setResponseMessage(prefix + accountDetails.getResponseMessage());
            // Log the error
            genericService.generateLog("Target savings Details retrieval", token, prefix + accountDetails.getResponseMessage(), "API Request", "INFO", requestPayload.getRequestId());
            return response;
        }

        if(!customerCode.equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())){
            response = ErrorResponse.getInstance();
            response.setResponseCode(customerCode);
            prefix = "Customer details retrieval failure: ";
            response.setResponseMessage(prefix + customerDetails.getResponseMessage());
            // Log the error
            genericService.generateLog("Target savings Details retrieval", token, prefix + customerDetails.getResponseMessage(), "API Request", "INFO", requestPayload.getRequestId());
            return response;
        }

        // Generate the response payload to return to the client
        TargetSavingsResponsePayload responsePayload = new TargetSavingsResponsePayload();
        responsePayload.setId(targetSavings.get().getId().toString());
        responsePayload.setSavingsAmount(targetSavings.get().getSavingsAmount());
        responsePayload.setTargetAmount(targetSavings.get().getTargetAmount());
        responsePayload.setMobileNumber(accountDetails.getMobileNumber());
        responsePayload.setRefId(targetSavings.get().getTransRef());
        responsePayload.setStatus(targetSavings.get().getStatus());
        responsePayload.setMilestoneAmount(targetSavings.get().getMilestoneAmount());
        responsePayload.setCustomerName(
                String.join(" ", customerDetails.getLastName(), customerDetails.getOtherName())
        );
        responsePayload.setStartDate(targetSavings.get().getStartDate().toString());
        responsePayload.setInterestRate(targetSavings.get().getInterestRate());
        responsePayload.setEndDate(targetSavings.get().getEndDate().toString());
        responsePayload.setDueAt(targetSavings.get().getTerminationDate().toString());
        responsePayload.setId(targetSavings.get().getId().toString());
        responsePayload.setAccountNumber(accountDetails.getAccountNumber());
        responsePayload.setContributionFrequency(targetSavings.get().getFrequency());
        responsePayload.setMilestonePercentage(targetSavings.get().getMilestonePercent());
        responsePayload.setEarliestTerminationDate
                (targetSavings.get().getEarliestTerminationDate().toString());
        responsePayload.setTenorInMonths(targetSavings.get().getTenorInMonth());
        responsePayload.setTotalMissedAmount(
                String.valueOf(targetSavingsRepository
                        .findCountOfMissedTargetSavingScheduleOfTargetSavings(targetSavings.get()))

        );
        responsePayload.setGoalName(targetSavings.get().getGoalName());
        responsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
        responsePayload.setDateTerminated(targetSavings.get().getTerminationDate().toString());
        responsePayload.setTerminatedBy(targetSavings.get().getTerminatedBy());

        // Prepare the response payload.
        PayloadResponse payloadResponse = PayloadResponse.getInstance();
        payloadResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
        payloadResponse.setResponseMessage("Success");
        payloadResponse.setResponseData(responsePayload);

        return payloadResponse;
    }

    private Optional<ErrorResponse>
    handleAllErrors(TargetSavingsDetailsRequestPayload requestPayload, Optional<TargetSavings> targetSavings)
    {

        ErrorResponse errorResponse = ErrorResponse.getInstance();
        errorResponse.setResponseCode(ResponseCodes.FAILED_MODEL.getResponseCode());
        String message;


        // Validate the existence of the parent targetSavings
        if( !targetSavings.isPresent() ) {
            // Check if it is due to the goalName or targetSavingsId
            if( requestPayload.getGoalName() != null ){
                message = genericService.getMessageOfTargetSavings("notexistname",
                        new String[]{requestPayload.getGoalName()});
            }else {
                message = genericService.getMessageOfTargetSavings("notexist",
                        new String[]{requestPayload.getTargetSavingsId()});
            }

            errorResponse.setResponseMessage(message);
            return Optional.ofNullable(errorResponse);
        }

        // Check that the requestId is unique
        if (targetSavingsRepository.findTargetSavingsByRequestId(requestPayload.getRequestId())
                .isPresent())
        {
            message = genericService.getMessageOfRequest("sameid",
                    new String[]{requestPayload.getRequestId()});
            errorResponse.setResponseMessage(message);
            return Optional.ofNullable(errorResponse);
        }

        // If all the above checks passes, return an empty optional error

        return Optional.empty();
    }


}
