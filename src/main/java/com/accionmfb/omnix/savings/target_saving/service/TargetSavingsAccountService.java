package com.accionmfb.omnix.savings.target_saving.service;

import com.accionmfb.omnix.savings.target_saving.constant.ModelStatus;
import com.accionmfb.omnix.savings.target_saving.constant.ResponseCodes;
import com.accionmfb.omnix.savings.target_saving.constant.TargetSavingStatus;
import com.accionmfb.omnix.savings.target_saving.dto.ErrorResponse;
import com.accionmfb.omnix.savings.target_saving.dto.PayloadResponse;
import com.accionmfb.omnix.savings.target_saving.dto.Response;
import com.accionmfb.omnix.savings.target_saving.jwt.JwtTokenUtil;
import com.accionmfb.omnix.savings.target_saving.model.TargetSavings;
import com.accionmfb.omnix.savings.target_saving.payload.request.AccountDetailsRequestPayload;
import com.accionmfb.omnix.savings.target_saving.payload.request.CustomerDetailsRequestPayload;
import com.accionmfb.omnix.savings.target_saving.payload.request.TargetSavingsAccountPayload;
import com.accionmfb.omnix.savings.target_saving.payload.response.AccountDetailsResponsePayload;
import com.accionmfb.omnix.savings.target_saving.payload.response.CustomerDetailsResponsePayload;
import com.accionmfb.omnix.savings.target_saving.payload.response.TargetSavingsDataResponsePayload;
import com.accionmfb.omnix.savings.target_saving.payload.response.TargetSavingsResponsePayload;
import com.accionmfb.omnix.savings.target_saving.repository.TargetSavingsRepository;
import com.accionmfb.omnix.savings.target_saving.util.TargetSavingsUtils;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * This service class returns all the target savings goals associated with a customer's
 * account number.
 */
@Service
public class TargetSavingsAccountService
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


    public Response getAllTargetSavingsByAccountNumber(TargetSavingsAccountPayload payload, String token)
    {
        Response response;

        // Log the request
        genericService.generateLog("Get All Target Savings Goal By Id", token, "Getting all target savings by account number", "API Request", "INFO", payload.getRequestId());

        // Get the account and the customer Details
        AccountDetailsRequestPayload accountRequest = new AccountDetailsRequestPayload();
        accountRequest.setAccountNumber(payload.getAccountNumber());
        accountRequest.setRequestId(payload.getRequestId());
        accountRequest.setImei(payload.getImei());
        String hash = genericService.encryptPayloadToString(accountRequest, token);
        accountRequest.setHash(hash);

        AccountDetailsResponsePayload accountDetails = externalService
                .getAccountDetailsFromAccountService(accountRequest, token);
        String accountResponseCode = accountDetails.getResponseCode();

        CustomerDetailsRequestPayload customerRequest = new CustomerDetailsRequestPayload();
        customerRequest.setMobileNumber(accountDetails.getMobileNumber());
        customerRequest.setRequestId(payload.getRequestId());
        customerRequest.setImei(payload.getImei());
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
            genericService.generateLog("Target savings List retrieval", token, prefix + accountDetails.getResponseMessage(), "API Request", "INFO", payload.getRequestId());
            return response;
        }

        if(!customerCode.equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())){
            response = ErrorResponse.getInstance();
            response.setResponseCode(customerCode);
            prefix = "Customer details retrieval failure: ";
            response.setResponseMessage(prefix + customerDetails.getResponseMessage());
            // Log the error
            genericService.generateLog("Target savings List retrieval", token, prefix + customerDetails.getResponseMessage(), "API Request", "INFO", payload.getRequestId());
            return response;
        }

        String accountNumber = payload.getAccountNumber().trim();

        // Create a list of all the target savings goal for the client
        List<TargetSavingsResponsePayload> responsePayloadList = new ArrayList<>();

        List<TargetSavings> targetSavings = targetSavingsRepository.findAllTargetSavingsByAccountNumber(accountNumber);

        targetSavings
                .forEach(targetSaving -> responsePayloadList
                        .add(createTargetSavingsResponsePayloadFromTargetSavings
                                (targetSaving, accountDetails, customerDetails)));

        TargetSavingsDataResponsePayload dataResponsePayload = new TargetSavingsDataResponsePayload();
        dataResponsePayload.setData(responsePayloadList);
        dataResponsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());

        PayloadResponse payloadResponse = PayloadResponse.getInstance();
        payloadResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
        payloadResponse.setResponseMessage("success");
        payloadResponse.setResponseData(dataResponsePayload);
        return payloadResponse;

    }

    private TargetSavingsResponsePayload
    createTargetSavingsResponsePayloadFromTargetSavings(
                    TargetSavings targetSavings,
                    AccountDetailsResponsePayload accountDetails,
                    CustomerDetailsResponsePayload customerDetails
    )
    {
        // Generate the response payload to return to the client
        TargetSavingsResponsePayload responsePayload = new TargetSavingsResponsePayload();
        responsePayload.setId(targetSavings.getId().toString());
        responsePayload.setSavingsAmount(targetSavings.getSavingsAmount());
        responsePayload.setTargetAmount(targetSavings.getTargetAmount());
        responsePayload.setMobileNumber(accountDetails.getMobileNumber());
        responsePayload.setRefId(targetSavings.getTransRef());
        responsePayload.setStatus(targetSavings.getStatus().equalsIgnoreCase(TargetSavingStatus.SUCCESS.name()) ? ModelStatus.ACTIVE.name() : targetSavings.getStatus());
        responsePayload.setMilestoneAmount(targetSavings.getMilestoneAmount());
        responsePayload.setCustomerName(
                String.join(" ", customerDetails.getLastName(), customerDetails.getOtherName())
        );
        responsePayload.setStartDate(targetSavings.getStartDate().toString());
        responsePayload.setInterestRate(targetSavings.getInterestRate());
        responsePayload.setEndDate(targetSavings.getEndDate().toString());
        responsePayload.setDueAt(targetSavings.getTerminationDate().toString());
        responsePayload.setId(targetSavings.getId().toString());
        responsePayload.setAccountNumber(accountDetails.getAccountNumber());
        responsePayload.setContributionFrequency(targetSavings.getFrequency());
        responsePayload.setMilestonePercentage(targetSavings.getMilestonePercent());
        responsePayload.setEarliestTerminationDate
                (targetSavings.getEarliestTerminationDate().toString());
        responsePayload.setTenorInMonths(targetSavings.getTenorInMonth());
        responsePayload.setTotalMissedAmount(
                String.valueOf(targetSavingsRepository
                        .findCountOfMissedTargetSavingScheduleOfTargetSavings(targetSavings))

        );
        responsePayload.setGoalName(targetSavings.getGoalName());
        responsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
        responsePayload.setDateTerminated(targetSavings.getTerminationDate().toString());
        responsePayload.setTerminatedBy(targetSavings.getTerminatedBy());
        responsePayload.setInterest(TargetSavingsUtils.resolveTargetSavingsInterest(targetSavings.getInterestAccrued()));
        responsePayload.setTotalMissedAmount(String.valueOf(targetSavingsRepository.findCountOfMissedTargetSavingScheduleOfTargetSavings(targetSavings)));

        return responsePayload;
    }

}
