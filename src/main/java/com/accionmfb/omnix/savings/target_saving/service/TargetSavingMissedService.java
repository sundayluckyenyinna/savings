package com.accionmfb.omnix.savings.target_saving.service;

import com.accionmfb.omnix.savings.target_saving.constant.*;
import com.accionmfb.omnix.savings.target_saving.dto.ErrorResponse;
import com.accionmfb.omnix.savings.target_saving.dto.PayloadResponse;
import com.accionmfb.omnix.savings.target_saving.dto.Response;
import com.accionmfb.omnix.savings.target_saving.jwt.JwtTokenUtil;
import com.accionmfb.omnix.savings.target_saving.model.TargetSavingSchedule;
import com.accionmfb.omnix.savings.target_saving.model.TargetSavings;
import com.accionmfb.omnix.savings.target_saving.payload.request.*;
import com.accionmfb.omnix.savings.target_saving.payload.response.*;
import com.accionmfb.omnix.savings.target_saving.repository.TargetSavingsRepository;
import com.google.gson.Gson;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.accionmfb.omnix.savings.target_saving.constant.ResponseCodes.INSUFFICIENT_BALANCE;
import static com.accionmfb.omnix.savings.target_saving.constant.ResponseCodes.SUCCESS_CODE;
import static com.accionmfb.omnix.savings.target_saving.constant.Terminator.SYSTEM;
import static com.accionmfb.omnix.savings.target_saving.util.TargetSavingsUtils.clean;
import static com.accionmfb.omnix.savings.target_saving.util.TargetSavingsUtils.generateRequestId;

/**
 *  This service class is responsible for the execution of missed target savings schedules. Automatic funds transfer
 *  is done from the customer's account to the Accion MFB pool account. If all missed goals are executed and there are
 *  no pending schedules and the target saving goal is due for termination, automatic funds transfer is made to the
 *  customer.
 */

@Service
public class TargetSavingMissedService
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

    @Autowired
    private TargetSavingsTerminateService targetSavingsTerminateService;

    @Value("${targetSavings.missed.graceInDays}")
    private String graceOfMissedSchedules;


    public Response
    processTargetSavingsCatchup(TargetSavingsMissedRequestPayload requestPayload, String token)
    {
        Response response;

        String username = jwtTokenUtil.getUsernameFromToken(token);
        String channel = jwtTokenUtil.getChannelFromToken(token);

        // Log the request
        genericService.generateLog("Target Savings Missed", token, username + "-" + channel, "API Request", "INFO", requestPayload.getRequestId());

        Optional<TargetSavings> targetSavings;

        // Get the goalName and the targetSavingsId and find the parent TargetSavings
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
                genericService.generateLog("Target Savings Missed", token, message, "API Request", "DEBUG", requestPayload.getRequestId());
                return errorResponse;
            }
            targetSavingsId = Long.valueOf(clean(requestPayload.getTargetSavingsId()));
        }

        // Validate that at least one of goalName or targetSavingsId is present.
        if(goalName == null && targetSavingsId == null){
            Response errorResponse = ErrorResponse.getInstance();
            String message = genericService.getMessageOfTargetSavings("nameandidmissing");
            errorResponse.setResponseMessage(message);
            errorResponse.setResponseCode(ResponseCodes.FORMAT_EXCEPTION.getResponseCode());
            // Log the error
            genericService.generateLog("Target Savings Missed", token, message, "API Request", "INFO", requestPayload.getRequestId());
            return errorResponse;
        }

        // Validate that not both goalName and targetSavingsId is present
        if(goalName != null && targetSavingsId != null){
            Response errorResponse = ErrorResponse.getInstance();
            String message = genericService.getMessageOfTargetSavings("nameandidpresent");
            errorResponse.setResponseMessage(message);
            errorResponse.setResponseCode(ResponseCodes.FORMAT_EXCEPTION.getResponseCode());
            // Log the error
            genericService.generateLog("Target Savings Missed", token, message, "API Request", "INFO", requestPayload.getRequestId());
            return errorResponse;
        }

        if(goalName != null) {
            if(accountNumber == null){
                response = ErrorResponse.getInstance();
                response.setResponseCode(ResponseCodes.FAILED_MODEL.getResponseCode());
                String message = genericService
                        .getMessageOfTargetSavings("accountnumbermissingforgoalname");
                response.setResponseMessage(message);
                // Log the error
                genericService.generateLog("Target Savings Missed", token, message, "API Request", "INFO", requestPayload.getRequestId());
                return response;
            }
            targetSavings = targetSavingsRepository
                    .findTargetSavingsByGoalNameAndAccountNumber(goalName, accountNumber);
        }
        else
            targetSavings = targetSavingsRepository.findTargetSavingsById(targetSavingsId);

        // Validate the existence of the parent targetSavings
        if( !targetSavings.isPresent() ) {
            // Check if it is due to the goalName or targetSavingsId
            String message;
            if( goalName != null ){
                 message = genericService.getMessageOfTargetSavings("notexistname",
                        new String[]{requestPayload.getGoalName()});
            }else {
                message = genericService.getMessageOfTargetSavings("notexist",
                        new String[]{requestPayload.getTargetSavingsId()});
            }

            response = ErrorResponse.getInstance();
            response.setResponseMessage(message);
            // Log the error
            genericService.generateLog("Target Savings Missed", token, message, "API Request", "INFO", requestPayload.getRequestId());
            return response;
        }

        // Get the account and the customer Details
        AccountDetailsRequestPayload accountRequest = new AccountDetailsRequestPayload();
        accountRequest.setAccountNumber(targetSavings.get().getAccountNumber());
        accountRequest.setRequestId(requestPayload.getRequestId());
        String hash = genericService.encryptPayloadToString(accountRequest, token);
        accountRequest.setHash(hash);

        AccountDetailsResponsePayload accountDetails = externalService
                .getAccountDetailsFromAccountService(accountRequest, token);

        CustomerDetailsRequestPayload customerRequest = new CustomerDetailsRequestPayload();
        customerRequest.setMobileNumber(accountDetails.getMobileNumber());
        customerRequest.setRequestId(requestPayload.getRequestId());
        String hash1 = genericService.encryptPayloadToString(customerRequest, token);
        customerRequest.setHash(hash1);

        CustomerDetailsResponsePayload customerDetails = externalService
                .getCustomerDetailsFromCustomerService(customerRequest, token);

        // Get the account balance details
        AccountBalanceRequestPayload accountBalRequest = new AccountBalanceRequestPayload();
        accountBalRequest.setAccountNumber(targetSavings.get().getAccountNumber());
        accountBalRequest.setRequestId(requestPayload.getRequestId());
        String hash2 = genericService.encryptPayloadToString(accountBalRequest, token);
        accountBalRequest.setHash(hash2);

        AccountBalanceResponsePayload accountBalDetails = externalService
                .getAccountBalanceFromAccountService(accountBalRequest, token);


        // handle all the errors and validations
        Optional<ErrorResponse> errorResponseOptional = handleAllErrors(
                requestPayload, targetSavings,
                accountDetails, customerDetails,
                accountBalDetails
        );
        if (errorResponseOptional.isPresent()) {
            // Log the error
            genericService.generateLog("Target Savings Missed", token, errorResponseOptional.get().getResponseMessage(), "API Request", "INFO", requestPayload.getRequestId());
            return errorResponseOptional.get();
        }

        int graceDays = (int)(Double.parseDouble(graceOfMissedSchedules));

        // Fetch all the missed schedules within the grace period.
        List<TargetSavingSchedule> missedSchedules = targetSavingsRepository
                .findAllMissedSchedulesOfTargetSavings(targetSavings.get())
                .stream()
                .filter(schedule -> schedule.getDueAt().plusDays(Long.valueOf(graceDays))
                        .compareTo(LocalDate.now()) >= 0)
                .collect(Collectors.toList());

        // Define the list of all successful and all failed missed executions
        List<TargetSavingSchedule> success = new ArrayList<>();
        List<TargetSavingSchedule> failed = new ArrayList<>();

        Map<String, List<TargetSavingSchedule>> result;
        try{
            result = handleExecutionOfMissedGoals(token, missedSchedules, success, failed)
                    .get();
        }catch (Exception exception){
            response = ErrorResponse.getInstance();
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(exception.getMessage());
            // Log the error
            genericService.generateLog("Target Savings Missed", token, exception.getMessage(), "API Request", "INFO", requestPayload.getRequestId());
            return response;
        }

        // Get the successfully executed schedules and calculate all milestone amount and percentage
        List<TargetSavingSchedule> successSchedules = result.get("success");

        // Check if there are any successful schedules at all
        if(successSchedules.isEmpty()){
            Response response1 = ErrorResponse.getInstance();
            response1.setResponseCode(ResponseCodes.SERVICE_UNAVAILABLE.getResponseCode()); // the service for funds transfer is possibly unavailable.
            String message = genericService.getMessageOfFunds("service.unavailable");
            response1.setResponseMessage(message);
            // Log the error
            genericService.generateLog("Target Savings Missed", token, username + "-" + channel, "API Request", "INFO", requestPayload.getRequestId());
            return response1;
        }

        // Get the total contributed amount
        BigDecimal totalContribution = BigDecimal.ZERO;

        for (int i = 0; i < successSchedules.size(); i++){
            totalContribution = totalContribution
                    .add(new BigDecimal(clean(successSchedules.get(i).getAmount())));
        }

        // Update the parameters of the parent target savings
        BigDecimal oldMilestone = new BigDecimal(clean(targetSavings.get().getMilestoneAmount()));
        BigDecimal newMilestone = oldMilestone.add(totalContribution);
        BigDecimal newMilestonePercent = newMilestone
                .multiply(new BigDecimal("100"))
                .divide(new BigDecimal(clean(targetSavings.get().getTargetAmount())), 5, RoundingMode.CEILING);
        int additionalContributionCount = successSchedules.size();
        BigDecimal additionalInterestAccrued = new BigDecimal(clean(targetSavings.get()
                .getDailyInterest()))
                .multiply(new BigDecimal(String.valueOf(additionalContributionCount)));
        BigDecimal newInterestAccrued = new BigDecimal(clean(targetSavings.get()
                .getInterestAccrued()))
                .add(additionalInterestAccrued);
        int newContributionCount = targetSavings.get().getContributionCountForMonth()
                + additionalContributionCount;

        targetSavings.get().setMilestoneAmount(newMilestone.toString());
        targetSavings.get().setMilestonePercent(newMilestonePercent.toString());
        targetSavings.get().setInterestAccrued(newInterestAccrued.toString());
        targetSavings.get().setContributionCountForMonth(newContributionCount);
        targetSavingsRepository.updateTargetSavings(targetSavings.get());

        // Better still, check the milestone percent and then decide next line of action.
        // Get an instance of a successful schedule
        TargetSavingSchedule schedule = successSchedules.get(0);
        String mobileNo = accountDetails.getMobileNumber();

        Response response1 = null;

        if(newMilestonePercent.doubleValue() >= 25 && !targetSavings.get().isSms25PercentSend()){
            handleSMS25PercentMilestone(schedule, mobileNo, token);
        }
        if(newMilestonePercent.doubleValue() >= 50 && !targetSavings.get().isSms50PercentSend()){
            handleSMS50PercentMilestone(schedule, mobileNo, token);
        }
        if(newMilestonePercent.doubleValue() >= 75 && !targetSavings.get().isSms75PercentSend()){
            handleSMS75PercentMilestone(schedule, mobileNo, token);
        }
        if (newMilestonePercent.doubleValue() >= 100 && !targetSavings.get().isSms100PercentSend()) {
            response1 = handleSMS100PercentMilestone(schedule, mobileNo, token);
        }

        PayloadResponse payloadResponse = PayloadResponse.getInstance();
        payloadResponse.setResponseCode(SUCCESS_CODE.getResponseCode());
        payloadResponse.setResponseMessage("success");

        // Check if the milestone is not up to 100%, then return payload to client.
        if(response1 == null){
            // This will return an empty list if there is no successfully executed schedules for missed goals.
            List<TargetSavingsResponsePayload> responseList = new ArrayList<>();
            successSchedules.stream().forEach(schedule1 -> {
                TargetSavingsResponsePayload responsePayload = new TargetSavingsResponsePayload();
                responsePayload.setId(schedule.getTargetSavings().getId().toString());
                responsePayload.setResponseCode(SUCCESS_CODE.getResponseCode());
                responsePayload.setDueAt(schedule.getDueAt().toString());
                responsePayload.setTargetAmount(schedule.getTargetSavings().getTargetAmount());
                responsePayload.setEndDate(schedule.getTargetSavings().getEndDate().toString());
                responsePayload.setGoalName(schedule.getTargetSavings().getGoalName());
                responsePayload.setStatus(schedule.getStatus());
                responsePayload.setSavingsAmount(schedule.getAmount());
                responsePayload.setTenorInMonths(schedule.getTargetSavings().getTenorInMonth());
                responsePayload.setEarliestTerminationDate(schedule.getTargetSavings()
                        .getEarliestTerminationDate().toString());
                responsePayload.setStartDate(schedule.getTargetSavings().getStartDate().toString());
                responsePayload.setInterestRate(schedule.getTargetSavings().getInterestRate());
                responsePayload.setMilestonePercentage(schedule.getTargetSavings().getMilestonePercent());
                responsePayload.setCustomerName(
                        String.join(" ", customerDetails.getLastName(), customerDetails.getOtherName())
                );
                responsePayload.setContributionFrequency(schedule.getTargetSavings().getFrequency());
                responsePayload.setMilestoneAmount(schedule.getTargetSavings().getMilestoneAmount());
                responsePayload.setMobileNumber(accountDetails.getMobileNumber());
                responsePayload.setAccountNumber(accountDetails.getAccountNumber());
                responsePayload.setRefId(schedule.getT24TransRef());
                responseList.add(responsePayload);
            });

            TargetSavingsDataResponsePayload body = new TargetSavingsDataResponsePayload();
            body.setResponseCode(SUCCESS_CODE.getResponseCode());
            body.setData(responseList);

            payloadResponse.setResponseData(body);
            return payloadResponse;
        }

        if(response1 != null){
            if(response1 instanceof ErrorResponse){
                // At this stage, the all missed and pending goals have been executed, but funds transfer to the customer has failed.
                Response errorResponse = ErrorResponse.getInstance();
                errorResponse.setResponseCode(ResponseCodes.REQUEST_PROCESSING.getResponseCode());

                String message  = "Successful execution of missed goals, " +
                        "but service error occurred during automatic goal termination and funds transfer. Please wait " +
                        "a little while or consult the customer support: Call 07000222466, WhatsApp 07045222933";

                errorResponse.setResponseMessage(message);

                // Log the error
                genericService.generateLog("Target Savings Missed", token, "Message: " + message, "API Request", "DEBUG", requestPayload.getRequestId());
                return errorResponse;
            }
            else {
                List<TargetSavingsResponsePayload> responseList = new ArrayList<>();
                successSchedules.stream().forEach(schedule1 -> {
                    TargetSavingsResponsePayload responsePayload = new TargetSavingsResponsePayload();
                    responsePayload.setId(schedule.getTargetSavings().getId().toString());
                    responsePayload.setResponseCode(SUCCESS_CODE.getResponseCode());
                    responsePayload.setDueAt(schedule.getDueAt().toString());
                    responsePayload.setTargetAmount(schedule.getTargetSavings().getTargetAmount());
                    responsePayload.setEndDate(schedule.getTargetSavings().getEndDate().toString());
                    responsePayload.setGoalName(schedule.getTargetSavings().getGoalName());
                    responsePayload.setStatus(schedule.getStatus());
                    responsePayload.setSavingsAmount(schedule.getAmount());
                    responsePayload.setTenorInMonths(schedule.getTargetSavings().getTenorInMonth());
                    responsePayload.setEarliestTerminationDate(schedule.getTargetSavings()
                            .getEarliestTerminationDate().toString());
                    responsePayload.setStartDate(schedule.getTargetSavings().getStartDate().toString());
                    responsePayload.setInterestRate(schedule.getTargetSavings().getInterestRate());
                    responsePayload.setMilestonePercentage(schedule.getTargetSavings().getMilestonePercent());
                    responsePayload.setCustomerName(
                            String.join(" ", customerDetails.getLastName(), customerDetails.getOtherName())
                    );
                    responsePayload.setContributionFrequency(schedule.getTargetSavings().getFrequency());
                    responsePayload.setMilestoneAmount(schedule.getTargetSavings().getMilestoneAmount());
                    responsePayload.setMobileNumber(accountDetails.getMobileNumber());
                    responsePayload.setAccountNumber(accountDetails.getAccountNumber());
                    responsePayload.setRefId(schedule.getT24TransRef());
                    responsePayload.setTerminatedBy(SYSTEM.name());
                    responsePayload.setDateTerminated(LocalDate.now().toString());
                    responsePayload.setTotalMissedAmount("0");
                    responseList.add(responsePayload);
                });

                TargetSavingsDataResponsePayload body = new TargetSavingsDataResponsePayload();
                body.setResponseCode(SUCCESS_CODE.getResponseCode());
                body.setData(responseList);

                payloadResponse.setResponseData(body);
                return payloadResponse;
            }
        }

        return payloadResponse;
    }



    /**
     * This method updates the status of the schedule after the SMS. This method should be called
     * after funds transfer and termination of the parent target savings goal. Here the target savings
     * schedule status will always be SUCCESS as Funds transfer has been done. This method therefore
     * is a wrapper utility for SMS of 25%, 50%, 75% and 100%, and also to state failure reasons that
     * will be stored in the database and acted upon.
     * @param schedule : TargetSavingsSchedule
     * @param response : Response
     */
    private void handleScheduleAfterSMS
    (TargetSavingSchedule schedule, Response response, String percent)
    {
        String prefix = "SMS failure: ";
        if (response instanceof ErrorResponse){
            String message = prefix + response.getResponseMessage();
            schedule.setFailureReason(message);
        }

        TargetSavings targetSavings = schedule.getTargetSavings();

        // Regardless of possible SMS sending failure, still set the SMS sent status to true, to avoid overload for cron job.
        if(percent.equalsIgnoreCase("25"))
            targetSavings.setSms25PercentSend(true);
        else if(percent.equalsIgnoreCase("50"))
            targetSavings.setSms50PercentSend(true);
        else if(percent.equalsIgnoreCase("75"))
            targetSavings.setSms75PercentSend(true);
        else if(percent.equalsIgnoreCase("100"))
            targetSavings.setSms100PercentSend(true);

        targetSavingsRepository.updateTargetSavings(targetSavings);
        targetSavingsRepository.updateTargetSavingSchedule(schedule);
    }
    private void
    handleSMS25PercentMilestone(TargetSavingSchedule schedule, String mobileNo, String token){
        Response response = sendNotificationSMS(schedule, mobileNo, "25", token);
        handleScheduleAfterSMS(schedule, response, "25");
    }

    private void
    handleSMS50PercentMilestone(TargetSavingSchedule schedule, String mobileNo, String token){
        // Send sms to the customer.
        Response response = sendNotificationSMS(schedule, mobileNo, "50", token);
        handleScheduleAfterSMS(schedule, response, "50");
    }

    private void
    handleSMS75PercentMilestone(TargetSavingSchedule schedule, String mobileNo, String token){
        Response response = sendNotificationSMS(schedule, mobileNo, "75", token);
        handleScheduleAfterSMS(schedule, response, "75");
    }

    private Response
    handleSMS100PercentMilestone(TargetSavingSchedule schedule, String mobileNo, String token){
        Response response = sendNotificationSMS(schedule, mobileNo, "100", token);
        handleScheduleAfterSMS(schedule, response, "100");

        // Here we will send a request to the internal termination API
        TargetSavingTerminationRequestPayload requestPayload =
                new TargetSavingTerminationRequestPayload();

        requestPayload.setGoalName(schedule.getTargetSavings().getGoalName());
        requestPayload.setAccountNumber(schedule.getTargetSavings().getAccountNumber());
        requestPayload.setRequestId(generateRequestId());
        String hash = genericService.encryptPayloadToString(requestPayload, token);
        requestPayload.setHash(hash);

        Response response1 = terminateDueTargetSavings(requestPayload, token);

        String responseCode = response1.getResponseCode();

        /**
         * If there is an error, it is likely that there was an issue in the funds transfer
         * to the customer and the target saving goal is not yet terminated.
          */
        if (!responseCode.equalsIgnoreCase(SUCCESS_CODE.getResponseCode())){
            String prefix = "Funds transfer to customer failure: ";
            TargetSavings targetSavings = schedule.getTargetSavings();
            targetSavings.setFailureReason(prefix + response1.getResponseMessage());
            // Log the error
            genericService.generateLog("Target Savings Missed", token, "Message: " + prefix + response1.getResponseMessage(), "API Request", "DEBUG", requestPayload.getRequestId());
            targetSavingsRepository.updateTargetSavings(targetSavings);
        }

        // Here the transfer to the client is a success. No need to update the parent. It will all be done by the termination service.
        return response1;
    }

    private Response
    sendNotificationSMS(TargetSavingSchedule schedule, String mobileNo, String percent, String token)
    {

        NotificationPayload smsPayload = new NotificationPayload();
        smsPayload.setMobileNumber(mobileNo);
        smsPayload.setAccountNumber(schedule.getTargetSavings().getAccountNumber());
        smsPayload.setRequestId(generateRequestId());
        smsPayload.setToken(token);
        smsPayload.setSmsFor("GOAL SETTING");
        smsPayload.setTargetAmount(clean(schedule.getTargetSavings().getTargetAmount()));
        smsPayload.setCurrentAmount(clean(schedule.getTargetSavings().getMilestoneAmount()));
        smsPayload.setMilestonePercent(clean(percent));

        CompletableFuture<Response> smsCodeFuture = sendGoalSettingMilestoneSMS(smsPayload);
        return smsCodeFuture.join();

    }

    Response
    terminateDueTargetSavings(TargetSavingTerminationRequestPayload requestPayload, String token)
    {
        Response response;
        // send a request to the internal termination service

        Response responsePayload = targetSavingsTerminateService
                .processTargetSavingsTermination(requestPayload, token);

        String responseCode = responsePayload.getResponseCode();

        if(responsePayload instanceof ErrorResponse)
        {
            response = ErrorResponse.getInstance();
            response.setResponseCode(responseCode);
            response.setResponseMessage(responsePayload.getResponseMessage());
            genericService.generateLog("Target savings Details retrieval", token, responsePayload.getResponseMessage(), "API Request", "DEBUG", requestPayload.getRequestId());
            return response;
        }

        // The termination is successful and it is a PayloadResponse instance
        return responsePayload;
    }

    public CompletableFuture<Response> sendGoalSettingMilestoneSMS(NotificationPayload requestPayload)
    {
        Response response;

        StringBuilder smsMessage = new StringBuilder();
        if (requestPayload.getMilestonePercent().equalsIgnoreCase("25")) {
            smsMessage.append("Weldone! The hardest part is taking the first step. You've saved N")
                    .append(requestPayload.getCurrentAmount())
                    .append(" out of your N")
                    .append(requestPayload.getTargetAmount())
                    .append(" Goal. Your future is bright! Dial *572*11# to track your savings");
        }

        if (requestPayload.getMilestonePercent().equalsIgnoreCase("50")) {
            smsMessage.append("Wow! You’re half way. Don’t stop, your goal is achievable! You reached N")
                    .append(requestPayload.getCurrentAmount())
                    .append(" out of your N")
                    .append(requestPayload.getTargetAmount())
                    .append(" Goal. Dial *572*11# to track your savings or call 07000222466");
        }
        if (requestPayload.getMilestonePercent().equalsIgnoreCase("75")) {
            smsMessage.append("You’re almost there. You've saved N")
                    .append(requestPayload.getCurrentAmount())
                    .append(" out of your N")
                    .append(requestPayload.getTargetAmount())
                    .append(" Goal. Good Job! Keep going. Dial *572*11# to track your savings. Call 07000222466 for support");
        }

        if (requestPayload.getMilestonePercent().equalsIgnoreCase("100")) {
            smsMessage.append("You did it! Now relax and enjoy your achievement. Your SaveBrighta account will be credited shortly. "
                    + "We celebrate you. Support: Call 07000222466, WhatsApp 07045222933");
        }

        SMSRequestPayload smsRequest = new SMSRequestPayload();
        smsRequest.setMobileNumber(requestPayload.getMobileNumber());
        smsRequest.setAccountNumber(requestPayload.getAccountNumber());
        smsRequest.setMessage(smsMessage.toString());
        smsRequest.setSmsFor(requestPayload.getSmsFor());
        smsRequest.setSmsType("N");
        smsRequest.setRequestId(requestPayload.getRequestId());
        smsRequest.setToken(requestPayload.getToken());
        String hash = genericService.encryptPayloadToString(smsRequest, smsRequest.getToken());
        smsRequest.setHash(hash);

        SMSResponsePayload smsResponsePayload;
        try{
            smsResponsePayload = externalService
                    .sendSMS(requestPayload.getToken(), smsRequest);
        }catch (Exception exception){
            Response errorResponse = ErrorResponse.getInstance();
            errorResponse.setResponseCode(ResponseCodes.SERVICE_UNAVAILABLE.getResponseCode());
            String prefix = "SMS sending failure: ";
            errorResponse.setResponseMessage(prefix + exception.getMessage());
            // Log the error
            genericService.generateLog("Target savings Details retrieval", smsRequest.getToken(), prefix + exception.getMessage(), "API Request", "INFO", requestPayload.getRequestId());
            return CompletableFuture.completedFuture(errorResponse);
        }

        String responseCode = smsResponsePayload.getResponseCode();

        if(!responseCode.equalsIgnoreCase(SUCCESS_CODE.getResponseCode())){
            response = ErrorResponse.getInstance();
            response.setResponseCode(responseCode);
            response.setResponseMessage(smsResponsePayload.getResponseMessage());
            // Log the error
            genericService.generateLog("Target savings Details retrieval", smsRequest.getToken(), smsResponsePayload.getResponseMessage(), "API Request", "INFO", requestPayload.getRequestId());
            return CompletableFuture.completedFuture(response);
        }

        // In this stage, the sms was sent successfully.
        PayloadResponse payloadResponse = PayloadResponse.getInstance();
        payloadResponse.setResponseCode(SUCCESS_CODE.getResponseCode());
        payloadResponse.setResponseData(null);
        return CompletableFuture.completedFuture(payloadResponse);

    }

    private Optional<ErrorResponse>
    handleAllErrors(
            TargetSavingsMissedRequestPayload requestPayload,
            Optional<TargetSavings> targetSavings,
            AccountDetailsResponsePayload accountDetails,
            CustomerDetailsResponsePayload customerDetails,
            AccountBalanceResponsePayload accountBalDetails
    )
    {

        ErrorResponse errorResponse = ErrorResponse.getInstance();
        errorResponse.setResponseCode(ResponseCodes.FAILED_MODEL.getResponseCode());
        String message;

        String goalName = requestPayload.getGoalName();

        // Check that the requestId is unique
        if (targetSavingsRepository.findTargetSavingsByRequestId(requestPayload.getRequestId())
                .isPresent())
        {
            message = genericService.getMessageOfRequest("sameid",
                    new String[]{requestPayload.getRequestId()});
            errorResponse.setResponseMessage(message);
            return Optional.ofNullable(errorResponse);
        }

        String accountResponseCode = accountDetails.getResponseCode();
        String customerResponseCode = customerDetails.getResponseCode();
        String accountBalResponseCode = accountBalDetails.getResponseCode();

        // Validate that the customer exists
        if(!customerResponseCode.equalsIgnoreCase(SUCCESS_CODE.getResponseCode()))
        {
            message = customerDetails.getResponseMessage();
            errorResponse.setResponseMessage(message);
            return Optional.ofNullable(errorResponse);
        }

        // Validate the account existence
        if(!accountResponseCode.equalsIgnoreCase(SUCCESS_CODE.getResponseCode()))
        {
            message = accountDetails.getResponseMessage();
            errorResponse.setResponseMessage(message);
            return Optional.ofNullable(errorResponse);
        }

        // Validate the activeness of the account
        if (!accountDetails.getStatus().equalsIgnoreCase(AccountStatus.SUCCESS.name()))
        {
            message = genericService.getMessageOfAccount("inactive",
                    new String[]{accountDetails.getAccountNumber()});
            errorResponse.setResponseMessage(message);
            return Optional.ofNullable(errorResponse);
        }

        // Validate the activeness of the customer
        if(!customerDetails.getStatus().equalsIgnoreCase(CustomerStatus.ACTIVE.name()))
        {
            message = genericService.getMessageOfCustomer("inactive",
                    new String[]{customerDetails.getMobileNumber()});
            errorResponse.setResponseMessage(message);
            return Optional.ofNullable(errorResponse);
        }

        // Validate the account is saveBrighta
        if(!accountDetails.getCategory().equalsIgnoreCase(AccountProductType.SAVEBRIGHTA.categoryCode))
        {
            message = genericService.getMessageOfAccount("notsavebrighta");
            errorResponse.setResponseMessage(message);
            Optional.ofNullable(errorResponse);
            return Optional.ofNullable(errorResponse);
        }

        // Validate that the target saving is not yet terminated
        if(targetSavings.get().getStatus().equalsIgnoreCase(TargetSavingStatus.TERMINATED.name()))
        {
            if(goalName != null){
                message = genericService.getMessageOfTargetSavings("terminatedname",
                        new String[]{targetSavings.get().getGoalName()});
            }else {
                message = genericService.getMessageOfTargetSavings("terminatedid",
                        new String[]{requestPayload.getTargetSavingsId()});
            }

            errorResponse.setResponseMessage(message);
            return Optional.ofNullable(errorResponse);
        }

        // Validate that there is at least one missed goal executable.
        int missedScheduleCount =
                findAllExecutableMissedScheduleOfTargetSavings(targetSavings.get());

        if(missedScheduleCount < 1){
            message = genericService.getMessageOfTargetSavings("nomissedgoal",
                    new String[]{targetSavings.get().getGoalName(), graceOfMissedSchedules});
            errorResponse.setResponseMessage(message);
            return Optional.ofNullable(errorResponse);
        }

//         Validate that there is sufficient amount now for all the missed goals
        if(!accountBalResponseCode.equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()))
        {
            message = accountBalDetails.getResponseMessage();
            errorResponse.setResponseMessage(message);
            return Optional.ofNullable(errorResponse);
        }

        BigDecimal accountBal = new BigDecimal(clean(accountBalDetails.getAvailableBalance()));
        BigDecimal totalCost = getTotalCostOfMissedSchedules(targetSavings.get());

        if(!isAccountBalanceSufficient(accountBal, totalCost) ){
            message = genericService.getMessageOfAccount("insufficientbalancemissed");
            errorResponse.setResponseMessage(message);
            return Optional.ofNullable(errorResponse);
        }

        return Optional.empty();
    }


    public int findAllExecutableMissedScheduleOfTargetSavings(TargetSavings targetSavings)
    {
        int graceDays = (int)(Double.parseDouble(graceOfMissedSchedules));
        return targetSavingsRepository
                .findAllMissedSchedulesOfTargetSavings(targetSavings)
                .stream()
                .filter(schedule -> schedule.getDueAt().plusDays(graceDays)
                        .compareTo(LocalDate.now()) >= 0 )
                .collect(Collectors.toList())
                .size();
    }

    private BigDecimal getTotalCostOfMissedSchedules(TargetSavings targetSavings)
    {

        int graceDays = (int)(Double.parseDouble(graceOfMissedSchedules));

        int missedScheduleCount = targetSavingsRepository
                .findAllMissedSchedulesOfTargetSavings(targetSavings)
                .stream()
                .filter(schedule -> schedule.getDueAt().plusDays(graceDays)
                        .compareTo(LocalDate.now()) >= 0)
                .collect(Collectors.toList())
                .size();

        TargetSavingSchedule schedule = targetSavingsRepository
                .findAllMissedSchedulesOfTargetSavings(targetSavings).get(0);

        String singleCost = schedule.getAmount();

        BigDecimal totalCost = new BigDecimal(clean(singleCost))
                .multiply(new BigDecimal(String.valueOf(missedScheduleCount)))
                .setScale(5, RoundingMode.CEILING);
        return totalCost;
    }

    private boolean isAccountBalanceSufficient(BigDecimal accountBalance, BigDecimal cost)
    {
        return accountBalance.compareTo(cost) > 0;
    }


    public CompletableFuture<Map<String, List<TargetSavingSchedule>>>
    handleExecutionOfMissedGoals(
            String token,
            List<TargetSavingSchedule> missedSchedules,
            List<TargetSavingSchedule> successfulExecutedSchedules,
            List<TargetSavingSchedule> failedExecutedSchedules
    )
    {

        Map<String, List<TargetSavingSchedule>> map = new HashMap<>();

        missedSchedules.stream()
                .forEach(schedule -> {
                    schedule.setExecutedAt(LocalDate.now());

                    Response response = executeFundsTransferForSchedule(schedule, token);

                    if( response instanceof  ErrorResponse ){

                        failedExecutedSchedules.add(schedule);
                        String code = response.getResponseCode();

                        /**
                         * Check if it is due to insufficient balance. Though, this code will
                         * never be reached as it will be blocked in 'handleAllErrors() validator
                         */
                        if(code.equalsIgnoreCase(INSUFFICIENT_BALANCE.getResponseCode())){
                            schedule.setStatus(TargetSavingStatus.MISSED.name());
                        }else{
                            schedule.setStatus(TargetSavingStatus.FAILED.name());
                        }
                        schedule.setFailureReason(response.getResponseMessage());
                        targetSavingsRepository.updateTargetSavingSchedule(schedule);
                    }
                    else {
                        successfulExecutedSchedules.add(schedule);
                        schedule.setStatus(TargetSavingStatus.SUCCESS.name());
                        schedule.setFailureReason(Strings.EMPTY);
                        targetSavingsRepository.updateTargetSavingSchedule(schedule);
                    }
                });

        map.put("success", successfulExecutedSchedules);
        map.put("failed", failedExecutedSchedules);
        return CompletableFuture.completedFuture(map);
    }

    private Response
    executeFundsTransferForSchedule(TargetSavingSchedule schedule, String token)
    {
        // Get the account details of the customer
        TargetSavings parent = schedule.getTargetSavings();
        String accountNumber = clean(parent.getAccountNumber());

        String prefix;

        AccountDetailsRequestPayload requestPayload = new AccountDetailsRequestPayload();
        requestPayload.setAccountNumber(accountNumber);
        requestPayload.setRequestId(generateRequestId());
        String hash = genericService.encryptPayloadToString(requestPayload, token);
        requestPayload.setHash(hash);

        AccountDetailsResponsePayload accountDetails = externalService
                .getAccountDetailsFromAccountService(requestPayload, token);


        String detailsResponseCode = accountDetails.getResponseCode();

        // Check if the details call was successful
        if(!detailsResponseCode.equalsIgnoreCase(SUCCESS_CODE.getResponseCode())){
            Response errorResponse = ErrorResponse.getInstance();
            errorResponse.setResponseCode(detailsResponseCode);
            prefix = "Account details retrieval failure: ";
            errorResponse.setResponseMessage(prefix + accountDetails.getResponseMessage());
            genericService.generateLog("Target savings Details retrieval", token, prefix = accountDetails.getResponseMessage(), "API Request", "INFO", requestPayload.getRequestId());
            return errorResponse;
        }


        // Call the FundsTransfer service
        String username = jwtTokenUtil.getUsernameFromToken(token);
        String narration = schedule.getTargetSavings().getGoalName()
                + "-CONTRIBUTION-" + schedule.getTargetSavings().getAccountNumber();

        // Create the fund transfer payload
        FundsTransferPayload fundsTransferPayload = new FundsTransferPayload();
        fundsTransferPayload.setMobileNumber(accountDetails.getMobileNumber());
        fundsTransferPayload.setAmount(schedule.getAmount());
        fundsTransferPayload.setCreditAccount(schedule.getCreditAccount());
        fundsTransferPayload.setDebitAccount(schedule.getDebitAccount());
        fundsTransferPayload.setTransType("ACTF");
        fundsTransferPayload.setBranchCode("NG0010068"); // Defaulted to the Digital Branch
        fundsTransferPayload.setInputter(username + "-" + accountDetails.getMobileNumber());
        fundsTransferPayload.setAuthorizer(username + "-" + accountDetails.getMobileNumber());
        fundsTransferPayload.setNoOfAuthorizer("0");
        fundsTransferPayload.setRequestId(genericService.generateTransRef("TS"));
        fundsTransferPayload.setToken(token);
        fundsTransferPayload.setNarration(narration);
        String hash2 = genericService.encryptFundTransferPayload(fundsTransferPayload, token);
        fundsTransferPayload.setHash(hash2);

        String requestJson = gson.toJson(fundsTransferPayload);

//         Call the funds transfer microservice.
        FundsTransferResponsePayload fundsTransferResponsePayload = externalService
                .getFundsTransferResponse(token, requestJson);

//         Check that the fund transfer was a success
        String fundResponseCode = fundsTransferResponsePayload.getResponseCode();

        if( !fundResponseCode.equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())){
            Response errorResponse = ErrorResponse.getInstance();
            errorResponse.setResponseCode(fundResponseCode);
            prefix = "Funds transfer failure: ";
            errorResponse.setResponseMessage(prefix + fundsTransferResponsePayload.getResponseMessage());
            genericService.generateLog("Target savings Details retrieval", token, prefix + fundsTransferResponsePayload.getResponseMessage(), "API Request", "INFO", requestPayload.getRequestId());
            return errorResponse;
        }

        // Now in this stage, the transfer is successful.
        PayloadResponse payloadResponse = PayloadResponse.getInstance();
        payloadResponse.setResponseCode(SUCCESS_CODE.getResponseCode());
        payloadResponse.setResponseData(fundsTransferResponsePayload);

        return payloadResponse;
    }

    private void handleSuccessfulExecutionOfSchedule
            (TargetSavingSchedule schedule, String mobileNo, String transRef)
    {

        // Get the parent target savings
        TargetSavings targetSavings = schedule.getTargetSavings();
        int newContributionCount = targetSavings.getContributionCountForMonth() + 1;
        targetSavings.setContributionCountForMonth(newContributionCount);

        // Calculate and set the interest after the contribution
        double newInterestAccrued = newContributionCount * Double.parseDouble(targetSavings.getDailyInterest());
        targetSavings.setInterestAccrued(String.valueOf(newInterestAccrued));

        // Update the milestone amount and percentage
        BigDecimal oldMilestoneAmount = new BigDecimal(clean(targetSavings.getMilestoneAmount()));
        BigDecimal newMilestoneAmount = oldMilestoneAmount.add(new BigDecimal(clean(schedule.getAmount())));
        targetSavings.setMilestoneAmount(newMilestoneAmount.toString());

        BigDecimal newMilestonePercent = newMilestoneAmount
                .multiply(new BigDecimal("100"))
                .divide(new BigDecimal(clean(targetSavings.getTargetAmount())), 5, RoundingMode.CEILING);
        targetSavings.setMilestonePercent(newMilestonePercent.toString());
        targetSavings.setTransRef(transRef);

        // Update the parent target savings
        targetSavingsRepository.updateTargetSavings(targetSavings);

    }
}
