package com.accionmfb.omnix.savings.target_saving.service;

import com.accionmfb.omnix.savings.target_saving.constant.Constants;
import com.accionmfb.omnix.savings.target_saving.constant.ResponseCodes;
import com.accionmfb.omnix.savings.target_saving.constant.TargetSavingStatus;
import com.accionmfb.omnix.savings.target_saving.dto.ErrorResponse;
import com.accionmfb.omnix.savings.target_saving.dto.PayloadResponse;
import com.accionmfb.omnix.savings.target_saving.dto.Response;
import com.accionmfb.omnix.savings.target_saving.jwt.JwtTokenUtil;
import com.accionmfb.omnix.savings.target_saving.model.TargetSavingSchedule;
import com.accionmfb.omnix.savings.target_saving.model.TargetSavings;
import com.accionmfb.omnix.savings.target_saving.payload.request.AccountDetailsRequestPayload;
import com.accionmfb.omnix.savings.target_saving.payload.request.CustomerDetailsRequestPayload;
import com.accionmfb.omnix.savings.target_saving.payload.request.FundsTransferPayload;
import com.accionmfb.omnix.savings.target_saving.payload.request.TargetSavingTerminationRequestPayload;
import com.accionmfb.omnix.savings.target_saving.payload.response.AccountDetailsResponsePayload;
import com.accionmfb.omnix.savings.target_saving.payload.response.CustomerDetailsResponsePayload;
import com.accionmfb.omnix.savings.target_saving.payload.response.FundsTransferResponsePayload;
import com.accionmfb.omnix.savings.target_saving.payload.response.TargetSavingsResponsePayload;
import com.accionmfb.omnix.savings.target_saving.repository.TargetSavingsRepository;
import com.accionmfb.omnix.savings.target_saving.util.TargetSavingsUtils;
import com.google.gson.Gson;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.accionmfb.omnix.savings.target_saving.constant.Terminator.SYSTEM;
import static com.accionmfb.omnix.savings.target_saving.constant.Terminator.USER;
import static com.accionmfb.omnix.savings.target_saving.util.TargetSavingsUtils.clean;
import static com.accionmfb.omnix.savings.target_saving.util.TargetSavingsUtils.generateRequestId;

/**
 * This service is responsible for the termination of a set target saving goal as per request from the customer by
 * virtue of the client program.
 */
@Service
public class TargetSavingsTerminateService
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

    @Value("${omnix.target.savings.poolaccount}")
    private String targetSavingsPoolAccount;


    public Response processTargetSavingsTermination(TargetSavingTerminationRequestPayload requestPayload, String token) {
        String username = jwtTokenUtil.getUsernameFromToken(token);
        String channel = jwtTokenUtil.getChannelFromToken(token);

        // Log the request
        genericService.generateLog("Target Savings termination", token, channel, "API Request", "DEBUG", requestPayload.getRequestId());

        // Check and balance all possible errors.
        Optional<ErrorResponse> optionalError = handleAllErrors(requestPayload);
        if(optionalError.isPresent()){
            genericService.generateLog("Target Savings termination", token, optionalError.get().getResponseMessage(), "API Request", "DEBUG", requestPayload.getRequestId());
            return optionalError.get();
        }

        String goalName = requestPayload.getGoalName();

        String accountNumber = requestPayload.getAccountNumber();

        Long targetSavingsId = null;

        if(requestPayload.getTargetSavingsId() != null){
            targetSavingsId = Long.valueOf(clean(requestPayload.getTargetSavingsId()));
        }

        TargetSavings targetSavings;

        // Get the target savings associated with the incoming goal name;
        if(goalName != null){
            if(accountNumber == null){
                Response errorResponse = ErrorResponse.getInstance();
                errorResponse.setResponseCode(ResponseCodes.FAILED_MODEL.getResponseCode());
                errorResponse.setResponseMessage(genericService
                        .getMessageOfTargetSavings("accountnumbermissingforgoalname"));
                // Log the error
                genericService.generateLog("Target Savings termination", token, errorResponse.getResponseMessage(), "API Request", "DEBUG", requestPayload.getRequestId());
                return errorResponse;
            }
            targetSavings = targetSavingsRepository
                    .findTargetSavingsByGoalNameAndAccountNumber
                            (requestPayload.getGoalName(), accountNumber).get();
        }else {
            targetSavings = targetSavingsRepository
                    .findTargetSavingsById(targetSavingsId).get();
        }

        Response response = handleTargetSavingsTermination(targetSavings, token, requestPayload.getRequestId(), requestPayload.getImei());

        if (response instanceof ErrorResponse){
            genericService.generateLog("Target Savings termination", token, response.getResponseMessage(), "API Request", "DEBUG", requestPayload.getRequestId());
            return response;
        }

        // Check if the response is from termination of no contribution.
        if(response.getResponseMessage().equalsIgnoreCase(Constants.TERMINATION_SUCCESS_MESSAGE_NO_CONTRIBUTION)) {
            genericService.generateLog("Target Savings Termination for no contribution", token, response.getResponseMessage(), "API Request", "INFO", requestPayload.getRequestId());
            return getResponseForNoContribution(response, targetSavings);
        }

        // Generate the response payload to the client
        PayloadResponse res = (PayloadResponse) response;
        FundsTransferResponsePayload fundsPayload = (FundsTransferResponsePayload) res.getResponseData();

        // Generate the details of the customer
        CustomerDetailsRequestPayload customerRequest = new CustomerDetailsRequestPayload();
        customerRequest.setMobileNumber(fundsPayload.getMobileNumber());
        customerRequest.setRequestId(generateRequestId());
        customerRequest.setImei(requestPayload.getImei());
        String hash = genericService.encryptPayloadToString(customerRequest, token);
        customerRequest.setHash(hash);

        CustomerDetailsResponsePayload customerDetails = externalService.getCustomerDetailsFromCustomerService(customerRequest, token);

        String fullName = String.join(" ", customerDetails.getLastName(), customerDetails.getOtherName());

        TargetSavingsResponsePayload responsePayload = new TargetSavingsResponsePayload();
        responsePayload.setId(targetSavings.getId().toString());
        responsePayload.setRefId(fundsPayload.getTransRef());
        responsePayload.setTerminatedBy(USER.name() + ": " + fundsPayload.getMobileNumber());
        responsePayload.setStatus(TargetSavingStatus.TERMINATED.name());
        responsePayload.setMilestoneAmount(targetSavings.getMilestoneAmount());
        responsePayload.setTargetAmount(targetSavings.getTargetAmount());
        responsePayload.setMobileNumber(fundsPayload.getMobileNumber());
        responsePayload.setSavingsAmount(targetSavings.getSavingsAmount());
        responsePayload.setResponseCode(fundsPayload.getResponseCode());
        responsePayload.setContributionFrequency(targetSavings.getFrequency());
        responsePayload.setCustomerName(fullName);
        responsePayload.setMilestonePercentage(targetSavings.getMilestonePercent());
        responsePayload.setInterestRate(targetSavings.getInterestRate());
        responsePayload.setGoalName(targetSavings.getGoalName());
        responsePayload.setStartDate(targetSavings.getStartDate().toString());
        responsePayload.setEndDate(targetSavings.getEndDate().toString());
        responsePayload.setDateTerminated(LocalDate.now().toString());
        responsePayload.setEarliestTerminationDate(targetSavings.getEarliestTerminationDate().toString());
        responsePayload.setTenorInMonths(targetSavings.getTenorInMonth());
        responsePayload.setInterest(TargetSavingsUtils.resolveTargetSavingsInterest(targetSavings.getInterestAccrued()));
        responsePayload.setTotalMissedAmount(String.valueOf(targetSavingsRepository.findCountOfMissedTargetSavingScheduleOfTargetSavings(targetSavings)));
        responsePayload.setTotalMissedAmount(String.valueOf(getTotalMissedSchedule(targetSavings)));

        PayloadResponse payloadResponse = PayloadResponse.getInstance();
        payloadResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
        payloadResponse.setResponseMessage("Success");
        payloadResponse.setResponseData(responsePayload);

        // Log the response
        genericService.generateLog("Target Savings Termination response", token, "Successful response generation", "API Request", "INFO", requestPayload.getRequestId());
        return payloadResponse;
    }

    private int getTotalMissedSchedule(TargetSavings targetSavings) {
        List<TargetSavingSchedule> schedules = targetSavingsRepository.findAllTargetSavingSchedulesByParent(targetSavings);
        List<TargetSavingSchedule> missedSchedules = schedules.stream()
                .filter(schedule -> schedule.getStatus().equalsIgnoreCase(TargetSavingStatus.MISSED.name()))
                .collect(Collectors.toList());
        return missedSchedules.size();
    }

    Response getResponseForNoContribution(Response response, TargetSavings targetSavings) {
        TargetSavingsResponsePayload responsePayload = new TargetSavingsResponsePayload();
        responsePayload.setId(targetSavings.getId().toString());
        responsePayload.setStatus(TargetSavingStatus.TERMINATED.name());
        responsePayload.setMilestoneAmount(targetSavings.getMilestoneAmount());
        responsePayload.setTargetAmount(targetSavings.getTargetAmount());
        responsePayload.setSavingsAmount(targetSavings.getSavingsAmount());
        responsePayload.setResponseCode(response.getResponseCode());
        responsePayload.setContributionFrequency(targetSavings.getFrequency());
        responsePayload.setMilestonePercentage(targetSavings.getMilestonePercent());
        responsePayload.setInterestRate(targetSavings.getInterestRate());
        responsePayload.setGoalName(targetSavings.getGoalName());
        responsePayload.setStartDate(targetSavings.getStartDate().toString());
        responsePayload.setEndDate(targetSavings.getEndDate().toString());
        responsePayload.setDateTerminated(LocalDate.now().toString());
        responsePayload.setEarliestTerminationDate(targetSavings.getEarliestTerminationDate().toString());
        responsePayload.setTenorInMonths(targetSavings.getTenorInMonth());

        // Get the total missed of the target savings goal.
        responsePayload.setTotalMissedAmount(String.valueOf(getTotalMissedSchedule(targetSavings)));
        responsePayload.setInterest(TargetSavingsUtils.resolveTargetSavingsInterest(targetSavings.getInterestAccrued()));
        PayloadResponse payloadResponse = PayloadResponse.getInstance();
        payloadResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
        payloadResponse.setResponseMessage("Success");
        payloadResponse.setResponseData(responsePayload);
        genericService.generateLog("Target Savings Termination response", "", "Successful response generation", "API Request", "INFO", targetSavings.getRequestId());
        return payloadResponse;
    }
    
    Optional<ErrorResponse> handleAllErrors(TargetSavingTerminationRequestPayload requestPayload) {
        ErrorResponse errorResponse = ErrorResponse.getInstance();
        errorResponse.setResponseCode(ResponseCodes.FAILED_MODEL.getResponseCode());

        String message;

        // Find the target savings by goalName or id
        String goalName = requestPayload.getGoalName();
        String accountNumber = requestPayload.getAccountNumber();

        Long targetSavingsId = null;

        // If the targetSavingsId is provided, it must be a numerical value.
        if(requestPayload.getTargetSavingsId() != null){
            if(!requestPayload.getTargetSavingsId().matches("[0-9]{1,}")){
                message = "Numerical target savings Id required";
                errorResponse.setResponseMessage(message);
                errorResponse.setResponseCode(ResponseCodes.FORMAT_EXCEPTION.getResponseCode());
                return Optional.ofNullable(errorResponse);
            }
            targetSavingsId = Long.valueOf(clean(requestPayload.getTargetSavingsId()));
        }

        // Validate that at least one of goalName or targetSavingsId is present.
        if(goalName == null && targetSavingsId == null){
            message = genericService.getMessageOfTargetSavings("nameandidmissing");
            errorResponse.setResponseMessage(message);
            return Optional.ofNullable(errorResponse);
        }

        // Validate that not both goalName and targetSavingsId is present
        if(goalName != null && targetSavingsId != null){
            message = genericService.getMessageOfTargetSavings("nameandidpresent");
            errorResponse.setResponseMessage(message);
            return Optional.ofNullable(errorResponse);
        }

        Optional<TargetSavings> targetSavings;

        if(goalName != null) {
            if (accountNumber == null) {
                message = genericService.getMessageOfTargetSavings("accountnumbermissingforgoalname");
                errorResponse.setResponseMessage(message);
                return Optional.ofNullable(errorResponse);
            }
            targetSavings = targetSavingsRepository
                    .findTargetSavingsByGoalNameAndAccountNumber
                            (requestPayload.getGoalName(), accountNumber);
        }
        else {
            targetSavings = targetSavingsRepository
                    .findTargetSavingsById(targetSavingsId);
        }

        // Validate the existence of the parent targetSavings
        if(targetSavings.isEmpty()) {
            // Check if it is due to the goalName or targetSavingsId
            if( goalName != null ){
                message = genericService.getMessageOfTargetSavings("notexistname",
                        new String[]{requestPayload.getGoalName()});
            }else {
                message = genericService.getMessageOfTargetSavings("notexist",
                        new String[]{requestPayload.getTargetSavingsId()});
            }

            errorResponse.setResponseMessage(message);
            return Optional.ofNullable(errorResponse);
        }

        // Check that the requestId is not already used.
        else if(targetSavingsRepository.findTargetSavingsByRequestId(requestPayload.getRequestId()).isPresent()) {
            message = genericService.getMessageOfRequest("sameid",
                    new String[] { requestPayload.getRequestId() });
            errorResponse.setResponseMessage(message);
            return Optional.ofNullable(errorResponse);
        }

        // Check that the target savings is not terminated already
        else if( targetSavings.get().getStatus().equalsIgnoreCase(TargetSavingStatus.TERMINATED.name())) {
            if(goalName != null){
                message = genericService.getMessageOfTargetSavings("terminatedname",
                        new String[]{requestPayload.getGoalName()});
            }else {
                message = genericService.getMessageOfTargetSavings("terminatedid",
                        new String[]{targetSavingsId.toString()});
            }

            errorResponse.setResponseMessage(message);
            return Optional.ofNullable(errorResponse);
        }

        // Check that the target savings the earliest termination date is due
        else if (targetSavings.get().getEarliestTerminationDate().isAfter(genericService.getCurrentDateTime().toLocalDate())) {
            message = genericService.getMessageOfTargetSavings("earliest.termination", new String[]{targetSavings.get().getEarliestTerminationDate().toString()});
            errorResponse.setResponseMessage(message);
            return Optional.ofNullable(errorResponse);
        }

        // If all the check and balances passes, return an empty optional error
        return Optional.empty();
    }


    // handle the termination of the operation
    private Response handleTargetSavingsTermination(TargetSavings targetSavings, String token, String requestId, String imei) {
        // Check if there is any contribution at all
        boolean isContributed = new BigDecimal(clean(targetSavings.getMilestoneAmount())).compareTo(BigDecimal.ZERO) > 0;

        // If there is no contribution, then terminate without transfer
        if( !isContributed )
            return terminateForNoContribution(targetSavings);
        else
            return terminateForContribution(targetSavings, token, requestId, imei);

    }

    private Response terminateForNoContribution(TargetSavings targetSavings) {
        return terminate(targetSavings, Strings.EMPTY);
    }

    private Response terminateForContribution(TargetSavings targetSavings, String token, String requestId, String imei) {
        // Do fund transfer to the customer's account first.
        Response fundsResponse = doTerminationFundTransfer(targetSavings, token, requestId, imei);

        // If the funds transfer fails, return the error and failure to the calling method. Don't terminate!
        if (fundsResponse instanceof ErrorResponse){
            targetSavings.setInterestFailureReason(fundsResponse.getResponseMessage());
            targetSavingsRepository.updateTargetSavings(targetSavings);
            return fundsResponse;
        }

        // The funds transfer is a success, try terminating the goal.
        FundsTransferResponsePayload fundsTrans = (FundsTransferResponsePayload) ((PayloadResponse) fundsResponse).getResponseData();

        Response terminationResponse = terminate(targetSavings, fundsTrans.getT24TransRef());

        // If the termination fails (which is highly unlikely as per DB operations!), return the error
        if(terminationResponse instanceof ErrorResponse)
            return terminationResponse;

        // The funds transfer and goal termination was successful, response is a Payload response.
        return fundsResponse;

    }

    /**
     * This method terminates the target savings goal. It must be called after the funds transfer
     * to the customer. The funds transfer will include crediting the customer with their milestone
     * savings and the total interest accrued. Hence, this method set the status of the Interest paid
     * to a status of TRUE.
     * @param targetSavings: TargetSavings
     * @return terminationResponse : Response
     */
    private Response terminate(TargetSavings targetSavings, String transRef) {
        Response response;
        try{

            // Get all the schedules associated with the target savings.
            List<TargetSavingSchedule> schedules = targetSavingsRepository.findAllTargetSavingSchedulesByParent(targetSavings);

            // Filter out the schedules that are pending to be terminated.
            schedules = schedules.stream()
                    .filter(schedule -> schedule.getStatus().equalsIgnoreCase(TargetSavingStatus.PENDING.name()))
                    .collect(Collectors.toList());

            // Terminate all the pending schedules by updating the status
            schedules.forEach(s -> {
                s.setStatus(TargetSavingStatus.TERMINATED.name());
                s.setExecutedAt(LocalDate.now());
                targetSavingsRepository.updateTargetSavingSchedule(s);
            });

            // Terminate the parent target savings by updating the status
            targetSavings.setStatus(TargetSavingStatus.TERMINATED.name());
            targetSavings.setTerminatedBy(SYSTEM.name());
            targetSavings.setTerminationDate(LocalDate.now());
            targetSavings.setInterestPaid(false);
            targetSavings.setInterestPaidAt(LocalDate.now());
            targetSavings.setTransRef(transRef);
            targetSavingsRepository.updateTargetSavings(targetSavings);

            response = PayloadResponse.getInstance();
            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setResponseMessage(Constants.TERMINATION_SUCCESS_MESSAGE_NO_CONTRIBUTION);

        }catch (Exception exception){
            response = ErrorResponse.getInstance();
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(exception.getMessage());
        }

        return response;
    }

    
    private Response doTerminationFundTransfer(TargetSavings targetSavings, String token, String reqId, String imei) {
        Response response;

        String prefix;

        // Fetch the account details
        String accountNumber = clean(targetSavings.getAccountNumber());
        AccountDetailsRequestPayload detailsRequestPayload = new AccountDetailsRequestPayload();
        detailsRequestPayload.setAccountNumber(accountNumber);
        detailsRequestPayload.setRequestId(reqId);
        detailsRequestPayload.setImei(imei);
        String hash = genericService.encryptPayloadToString(detailsRequestPayload, token);
        detailsRequestPayload.setHash(hash);

        AccountDetailsResponsePayload accountDetails = externalService.getAccountDetailsFromAccountService(detailsRequestPayload, token);

        String responseCode = accountDetails.getResponseCode();

        if(!responseCode.equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
            response = ErrorResponse.getInstance();
            response.setResponseCode(responseCode);
            prefix = "Account details retrieval failure: ";
            response.setResponseMessage(prefix + accountDetails.getResponseMessage());
            genericService.generateLog("Target Savings Termination response", token, prefix + accountDetails.getResponseMessage(), "API Request", "INFO", targetSavings.getRequestId());
            return response;
        }

        // Now the fetching of the account details was a  success
        String narration = targetSavings.getGoalName() + "-TERMINATION-" + targetSavings.getAccountNumber();

        // Prepare the total amount earned by the customer
        BigDecimal totalEarnings = new BigDecimal(clean(targetSavings.getMilestoneAmount()))
                .add(new BigDecimal(clean(targetSavings.getInterestAccrued())))
                .setScale(2, RoundingMode.FLOOR);

        // Create the fund transfer payload
        FundsTransferPayload fundsTransferPayload = new FundsTransferPayload();
        fundsTransferPayload.setMobileNumber(accountDetails.getMobileNumber());
        fundsTransferPayload.setAmount(clean(totalEarnings.toString()));
        fundsTransferPayload.setCreditAccount(clean(targetSavings.getAccountNumber()));
        fundsTransferPayload.setDebitAccount(targetSavingsPoolAccount);
        fundsTransferPayload.setTransType("ACTF");
        fundsTransferPayload.setBranchCode("NG0010068"); // Defaulted to the Digital Branch
        fundsTransferPayload.setInputter(SYSTEM.name() + "-" + accountDetails.getMobileNumber());
        fundsTransferPayload.setAuthorizer(SYSTEM.name() + "-" + accountDetails.getMobileNumber());
        fundsTransferPayload.setNoOfAuthorizer("0");
        fundsTransferPayload.setRequestId(genericService.generateTransRef("TS"));
        fundsTransferPayload.setToken(token);
        fundsTransferPayload.setNarration(narration);
        fundsTransferPayload.setImei(imei);
        String hash1 = genericService.encryptFundTransferPayload(fundsTransferPayload, token);
        fundsTransferPayload.setHash(hash1);

        String requestJson = gson.toJson(fundsTransferPayload);

        // Call the funds transfer microservice.
        FundsTransferResponsePayload fundsTransferResponsePayload = externalService.getFundsTransferResponse(token, requestJson);

        // Check that the service was even reached in the first place.
        String fresponseCode = fundsTransferResponsePayload.getResponseCode();

        // Now check if the transfer was not successful.
        if( !fresponseCode.equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())){
            Response errorResponse = ErrorResponse.getInstance();
            errorResponse.setResponseCode(fresponseCode);
            prefix = "Funds transfer failure: ";
            errorResponse.setResponseMessage(prefix + fundsTransferResponsePayload.getResponseMessage());
            targetSavings.setFailureReason(prefix + fundsTransferResponsePayload.getResponseMessage());
            targetSavingsRepository.updateTargetSavings(targetSavings);

            genericService.generateLog("Target Savings Termination response", token, prefix + fundsTransferResponsePayload.getResponseMessage(), "API Request", "INFO", targetSavings.getRequestId());
            return errorResponse;
        }

        // Now the transaction was successful
        PayloadResponse payloadResponse = PayloadResponse.getInstance();
        payloadResponse.setResponseCode(fresponseCode);
        payloadResponse.setResponseData(fundsTransferResponsePayload);

        genericService.generateLog("Target Savings Termination response", token, "Successful funds response generation", "API Request", "INFO", targetSavings.getRequestId());

        return payloadResponse;
    }

}
