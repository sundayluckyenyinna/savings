package com.accionmfb.omnix.savings.target_saving.service;

import com.accionmfb.omnix.savings.target_saving.constant.ResponseCodes;
import com.accionmfb.omnix.savings.target_saving.constant.TargetSavingStatus;
import com.accionmfb.omnix.savings.target_saving.constant.TargetSavingsFrequency;
import com.accionmfb.omnix.savings.target_saving.dto.ErrorResponse;
import com.accionmfb.omnix.savings.target_saving.dto.PayloadResponse;
import com.accionmfb.omnix.savings.target_saving.dto.Response;
import com.accionmfb.omnix.savings.target_saving.dto.TargetSavingsListByAccountPayload;
import com.accionmfb.omnix.savings.target_saving.jwt.JwtTokenUtil;
import com.accionmfb.omnix.savings.target_saving.model.TargetSavingSchedule;
import com.accionmfb.omnix.savings.target_saving.model.TargetSavings;
import com.accionmfb.omnix.savings.target_saving.payload.request.*;
import com.accionmfb.omnix.savings.target_saving.payload.response.*;
import com.accionmfb.omnix.savings.target_saving.repository.TargetSavingsRepository;
import com.google.gson.Gson;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.accionmfb.omnix.savings.target_saving.constant.ResponseCodes.INSUFFICIENT_BALANCE;
import static com.accionmfb.omnix.savings.target_saving.constant.ResponseCodes.SUCCESS_CODE;
import static com.accionmfb.omnix.savings.target_saving.constant.TargetSavingStatus.*;
import static com.accionmfb.omnix.savings.target_saving.constant.TargetSavingsFrequency.DAILY;
import static com.accionmfb.omnix.savings.target_saving.constant.TargetSavingsFrequency.WEEKLY;
import static com.accionmfb.omnix.savings.target_saving.util.TargetSavingsUtils.clean;
import static com.accionmfb.omnix.savings.target_saving.util.TargetSavingsUtils.generateRequestId;


@Configuration
@EnableScheduling
@EnableAsync
public class CronJobs
{
    // Generate a token that will last forever.
    @Autowired
    private TargetSavingsRepository targetSavingsRepository;

    @Autowired
    private GenericService genericService;

    @Autowired
    private ExternalService externalService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private TargetSavingsTerminateService targetSavingsTerminateService;

    @Autowired
    private Gson gson;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ApplicationContext applicationContext;

    @Bean(destroyMethod = "shutdown")
    public Executor executorService(){
        return Executors.newScheduledThreadPool(4);
    }


    /**
     * The cron Job to handle the automatic execution of pending target saving goals.
     * This cron job is triggered at every 24 hours the server is up and running every day.
     */
    @Async
    @Scheduled(fixedDelay = 24 * 60 * 60 * 1000, initialDelay = 1000)
    public void executeContributionAndSMSForAllDueSchedules()
    {

        System.out.println("Scheduler starting...");

        String tokenString = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJncnVwcCIsInJvbGVzIjoiW0dSVVBQLCBMT0NBTF9GVU5EU19UUkFOU0ZFUiwgQUlSVElNRV9TRUxGLCBBSVJUSU1FX09USEVSUywgQ0FCTEVfVFZfU1VCU0NSSVBUSU9OLCBFTEVDVFJJQ0lUWV9CSUxMX1BBWU1FTlQsIFNNU19OT1RJRklDQVRJT04sIElOVEVSX0JBTktfRlVORFNfVFJBTlNGRVIsIEFDQ09VTlRfREVUQUlMUywgQUNDT1VOVF9CQUxBTkNFUywgTE9DQUxfRlVORFNfVFJBTlNGRVJfV0lUSF9DSEFSR0UsIEFDQ09VTlRfQkFMQU5DRSwgTklQX05BTUVfRU5RVUlSWV0iLCJhdXRoIjoibWsvdnQ2OVBXMUVVaEpTVUhnZE0rQT09IiwiQ2hhbm5lbCI6IkFHRU5DWSIsIklQIjoiMDowOjA6MDowOjA6MDoxIiwiaXNzIjoiQWNjaW9uIE1pY3JvZmluYW5jZSBCYW5rIiwiaWF0IjoxNjU5MzQ2NDMyLCJleHAiOjYyNTE0Mjg0NDAwfQ.Q6aeZeZtT6IeDNjFa5Sc7gAt0vKLqFjERPy02zS7aTg";

        genericService.generateLog("Target Savings Scheduler", tokenString, "Scheduler starting", "API Scheduler", "INFO", "Omnix Savings API scheduler");

        // Create a queue to hold all schedules
        List<List<TargetSavingSchedule>> queue = new ArrayList<>();

        // Find all the target savings in the repository that are not terminated
        List<TargetSavings> targetSavingsList = targetSavingsRepository
                .findAllTargetSavingsNotTerminated();

        for (TargetSavings targetSavings : targetSavingsList) {
            List<TargetSavingSchedule> scheduleList = targetSavingsRepository
                    .findAllPendingAndFailedSchedulesByParent(targetSavings);
            queue.add(scheduleList);
        }

        // Get the map that maps the unique account numbers and the corresponding account details.
        Map<String, AccountDetailsResponsePayload> accountDetailsMap =
                getAllCustomersAccountDetailsForTargetSavings(targetSavingsList, tokenString);

        for (List<TargetSavingSchedule> individualScheduleList : queue) {

            //Start a new thread for execution and wait for it to complete.
            CompletableFuture
                    .supplyAsync(() -> accountDetailsMap)
                    .thenApply(map -> {
                        System.out.println("Executing Automatic SMS notification push...");
                        executeSMSDue(individualScheduleList, map, tokenString);
                        return map;
                    })
                    .thenApply(map -> {
                        System.out.println("Executing Automatic Schedules...");
                        executeScheduleQueue(individualScheduleList, map, tokenString);
                        return map;
                    })
                    .join(); // Wait for completion.
        }

        // Execute automatic payout.
        try{
            CompletableFuture
                    .runAsync(() -> {
                        System.out.println("Executing Automatic Payout...");
                        executeAutomaticTargetSavingsTerminationAndPayout(targetSavingsList, tokenString);
                    }).join();
        }catch (Exception exception){
            String errorMessage = exception.getMessage();
            genericService.generateLog("Target savings automatic payout execution", tokenString, "Error while doing automatic payout: ".concat(errorMessage), "DEBUB", "SEVERE", "");
        }

        System.out.println("Scheduler done for today...");
    }

    private void
    executeSMSDue(List<TargetSavingSchedule> schedules, Map<String, AccountDetailsResponsePayload> map, String token) {

        // Only those schedules that are pending and SMS due should send SMS
        schedules = schedules.stream()
                .filter(schedule -> !schedule.getTargetSavings().getFrequency().equalsIgnoreCase(DAILY.name())) // no sms for daily schedules.
                .filter(schedule -> !schedule.isSmsDueSend()) // schedules that SMS is not yet sent.
                .filter(schedule -> schedule.getStatus().equalsIgnoreCase(PENDING.name()))   // schedules that are still pending.
                .filter(schedule -> schedule.getSmsDueDate().compareTo(LocalDate.now().minusDays(1)) > 0 &&
                        schedule.getSmsDueDate().compareTo(LocalDate.now().plusDays(1)) < 0) // schedule that SMS due date is today.
                .collect(Collectors.toList());

                schedules.forEach(schedule -> {
                    TargetSavings targetSavings = schedule.getTargetSavings();

                    // Get the customer's account details
                    AccountDetailsResponsePayload accountDetails =
                            getAccountDetailsForSchedule(schedule, map);

                    String accountResponseCode = accountDetails.getResponseCode();

                    // Log out the details
                    System.out.println("Account details: " + gson.toJson(accountDetails));

                    if(!accountResponseCode
                            .equalsIgnoreCase(SUCCESS_CODE.getResponseCode()))
                    {
                        schedule.setSmsDueSend(false);
                        schedule.setFailureReason("Account details retrieval failure for SMS sending: "
                                + accountDetails
                                .getResponseMessage());

                        // Log out the error
                        System.out.println("Response: " + gson.toJson(accountDetails));
                        targetSavingsRepository.updateTargetSavingSchedule(schedule);
                        return;
                    }

                    // Create the notification payload
                    NotificationPayload smsPayload = new NotificationPayload();
                    smsPayload.setMobileNumber(accountDetails.getMobileNumber());
                    smsPayload.setAccountNumber(schedule.getTargetSavings().getAccountNumber());
                    smsPayload.setRequestId(generateRequestId());
                    smsPayload.setToken(token);
                    smsPayload.setSmsFor("GOAL SETTING");
                    smsPayload.setTargetAmount(clean(schedule.getTargetSavings().getTargetAmount()));
                    smsPayload.setCurrentAmount(clean(schedule.getTargetSavings().getMilestoneAmount()));
                    smsPayload.setMilestonePercent(clean(targetSavings.getMilestonePercent()));

                    Response response = sendGoalSettingDueSMS(smsPayload);

                    // log out the SMS response details
                    System.out.println("SMS: " + gson.toJson(response));
                    if ( !response.getResponseCode().equalsIgnoreCase(SUCCESS_CODE.getResponseCode())){
                        schedule.setSmsDueSend(false);
                        schedule.setFailureReason("SMS failure due to: " + response
                                .getResponseMessage());
                        targetSavingsRepository.updateTargetSavingSchedule(schedule);
                    }else{
                        schedule.setSmsDueSend(true);
                        schedule.setFailureReason(Strings.EMPTY);
                        targetSavingsRepository.updateTargetSavingSchedule(schedule);
                    }

                });
    }

    private void
    executeScheduleQueue(List<TargetSavingSchedule> scheduleQueue, Map<String, AccountDetailsResponsePayload> map, String token) {
        // Only those schedules that are PENDING and FAILED earlier to be executed.
        scheduleQueue.stream()
                .filter(schedule -> !schedule.getStatus().equalsIgnoreCase(TERMINATED.name())) // already terminated
                .filter(schedule -> !schedule.getStatus().equalsIgnoreCase(SUCCESS.name())) // already executed
                .filter(schedule -> !schedule.getStatus().equalsIgnoreCase(MISSED.name())) // already missed. The user will initiate the action
                .filter(schedule -> schedule.getDueAt().compareTo(LocalDate.now()) <= 0) // schedules that are due
                .forEachOrdered(schedule -> {

                    schedule.setExecutedAt(LocalDate.now());

                    Response response = executeFundsTransferForSchedule(schedule, map, token);

                    // Log out the funds transfer response details
                    System.out.println("Funds transfer response: " + gson.toJson(response));

                    String prefix = "Funds transfer failure: ";

                    // If the operation succeeds, set the status of the target savings.
                    String responseCode = response.getResponseCode();
                    if(responseCode.equalsIgnoreCase(SUCCESS_CODE.getResponseCode())){
                        FundsTransferResponsePayload fundsPayload =
                                (FundsTransferResponsePayload)((PayloadResponse)response).getResponseData();

                        String customerMobile = fundsPayload.getMobileNumber();
                        String transactionRef = fundsPayload.getTransRef();
                        schedule.setStatus(SUCCESS.name());
                        schedule.setFailureReason(Strings.EMPTY);
                        targetSavingsRepository.updateTargetSavingSchedule(schedule);
                        handleSuccessfulExecutionOfSchedule(schedule, customerMobile, transactionRef, token);
                    }

                    // If the operation fails due to insufficient balance, set the status of the target savings to MISSED.
                    else if(responseCode.equalsIgnoreCase(INSUFFICIENT_BALANCE.getResponseCode())){
                        schedule.setStatus(MISSED.name());
                        schedule.setFailureReason(prefix + response.getResponseMessage());
                        targetSavingsRepository.updateTargetSavingSchedule(schedule);
                    }

                    // If the operation fails due to some unknown server failures, set the status to FAILED.
                    else {
                        schedule.setFailureReason(prefix + response.getResponseMessage());
                        schedule.setStatus(FAILED.name());
                        targetSavingsRepository.updateTargetSavingSchedule(schedule);
                    }
                });

    }


    /**
     * This method runs in a cron job. It automatically executes the target savings that are due to be terminated.
     * The termination condition is such that:
     * 1) The due date of the target savings goal has expired.
     * @param targetSavingsList: List<TargetSavings>
     */
    private void executeAutomaticTargetSavingsTerminationAndPayout(List<TargetSavings> targetSavingsList, String token){

        // Get the map of the unique account numbers and their associated target savings.
        Map<String, List<TargetSavings>> mapList = this.getTargetSavingsListMap(targetSavingsList);

        List<Map.Entry<String, List<TargetSavings>>> entries = new ArrayList<>(mapList.entrySet());

        for (Map.Entry<String, List<TargetSavings>> entry : entries) {
            String accountNumber = entry.getKey();
            List<TargetSavings> targetSavings = entry.getValue();

            // For each of the target savings, perform execution of the termination.
            targetSavings = targetSavings.stream()
                    .filter(targetSaving -> !targetSaving.getStatus().equalsIgnoreCase(TERMINATED.name()))  // Those savings not terminated.
                    .filter(targetSaving -> targetSavingsRepository.findAllPendingSchedulesOfTargetSavings(targetSaving).size() == 0) // no more pending schedules.
                    .filter(targetSaving -> targetSaving.getEndDate().plusDays(1).compareTo(LocalDate.now()) < 0) // Those savings that are due.
                    .filter(targetSaving -> new BigDecimal(clean(targetSaving.getMilestoneAmount())).compareTo(BigDecimal.ONE) > 0) // those schedules that at least have a milestone amount.
                    .collect(Collectors.toList());

            targetSavings.forEach(targetSaving -> {
                // Create the termination request.
                TargetSavingTerminationRequestPayload requestPayload = new TargetSavingTerminationRequestPayload();
                requestPayload.setAccountNumber(accountNumber);
                requestPayload.setRequestId(generateRequestId());
                requestPayload.setGoalName(targetSaving.getGoalName());
                String hash = this.genericService.encryptPayloadToString(requestPayload, token);
                requestPayload.setHash(hash);

                // Terminate the target saving goal internally.
                Response response = terminateDueTargetSavings(requestPayload, token);
                if (response instanceof ErrorResponse || !response.getResponseCode().equalsIgnoreCase(SUCCESS_CODE.getResponseCode())) {
                    targetSaving.setFailureReason("Termination Failure: ".concat(response.getResponseMessage()));
                } else {
                    genericService.generateLog("Target Saving Scheduler", token, "Target saving goal terminated successfully", "INFO", "SEVERE", requestPayload.getRequestId());
                }

            });

        }

    }

    private Map<String, AccountDetailsResponsePayload>
    getAllCustomersAccountDetailsForTargetSavings(List<TargetSavings> targetSavingsList, String token) {
        // Create a map of all the target savings and the respective account details
        Map<String, AccountDetailsResponsePayload> accountDetails = new HashMap<>();

        // Create a set to hold all distinct account numbers.
        Set<String> accountNumberSet = new TreeSet<>();
        targetSavingsList
                .forEach(targetSavings -> accountNumberSet.add(targetSavings.getAccountNumber()));

        List<String> distinctAccountNumberList = new ArrayList<>(accountNumberSet);

        for(int i = 0; i < accountNumberSet.size(); i++){

            String accountNumber = distinctAccountNumberList.get(i);

            // Get the customer's account details
            AccountDetailsRequestPayload accountRequest = new AccountDetailsRequestPayload();
            accountRequest.setAccountNumber(accountNumber);
            accountRequest.setRequestId(generateRequestId());
            String hash = genericService.encryptPayloadToString(accountRequest, token);
            accountRequest.setHash(hash);

            AccountDetailsResponsePayload details = externalService
                    .getAccountDetailsFromAccountService(accountRequest, token);

            accountDetails.put(accountNumber, details);
        }

        return accountDetails;
    }

    private AccountDetailsResponsePayload getAccountDetailsForSchedule(TargetSavingSchedule schedule, Map<String, AccountDetailsResponsePayload> map) {
        // Get the target savings
        TargetSavings targetSavings = schedule.getTargetSavings();

        // Get the accountNumber of the parent target savings for use as a key.
        String accountNumber = targetSavings.getAccountNumber();

        return map.get(accountNumber);
    }

    private void handleSuccessfulExecutionOfSchedule(TargetSavingSchedule schedule, String mobileNo, String transRef, String token) {

        // Get the parent target savings
        TargetSavings targetSavings = schedule.getTargetSavings();

        int newContributionCount = targetSavings.getContributionCountForMonth() + 1;
        targetSavings.setContributionCountForMonth(newContributionCount);

        // Calculate and set the interest after the contribution
        double newInterestAccrued = newContributionCount * Double.parseDouble(targetSavings.getDailyInterest());
        targetSavings.setInterestAccrued(String.valueOf(newInterestAccrued));

        // Update the milestone amount and percentage
        BigDecimal oldMilestoneAmount = new BigDecimal(clean(targetSavings.getMilestoneAmount()));
        BigDecimal newMilestoneAmount = oldMilestoneAmount
                .add(new BigDecimal(clean(schedule.getAmount())));

        targetSavings.setMilestoneAmount(newMilestoneAmount.toString());

        BigDecimal newMilestonePercent = newMilestoneAmount
                .multiply(new BigDecimal("100"))
                .divide(new BigDecimal(clean(targetSavings.getTargetAmount())), 5, RoundingMode.CEILING);
        targetSavings.setMilestonePercent(newMilestonePercent.toString());
        targetSavings.setTransRef(transRef);

        // Update the parent target savings
        targetSavingsRepository.updateTargetSavings(targetSavings);

        // Check if the SMS is due, based on the milestone percentage
        if(newMilestonePercent.doubleValue() >= 25 && !targetSavings.isSms25PercentSend()){
            handleSMS25PercentMilestone(schedule, mobileNo, token);
        }
        if(newMilestonePercent.doubleValue() >= 50 && !targetSavings.isSms50PercentSend()){
            handleSMS50PercentMilestone(schedule, mobileNo, token);
        }
        if(newMilestonePercent.doubleValue() >= 75 && !targetSavings.isSms75PercentSend()){
            handleSMS75PercentMilestone(schedule, mobileNo, token);
        }
        if (newMilestonePercent.doubleValue() >= 100 && !targetSavings.isSms100PercentSend()) {
            handleSMS100PercentMilestone(schedule, mobileNo, token);
        }

    }

    private void handleSMS25PercentMilestone(TargetSavingSchedule schedule, String mobileNo, String token) {
        Response response = sendNotificationSMS(schedule, mobileNo, "25", token);
        handleScheduleAfterSMS(schedule, response, "25");
    }

    private void handleSMS50PercentMilestone(TargetSavingSchedule schedule, String mobileNo, String token) {
        // Send sms to the customer.
        Response response = sendNotificationSMS(schedule, mobileNo, "50", token);
        handleScheduleAfterSMS(schedule, response, "50");
    }

    private void handleSMS75PercentMilestone(TargetSavingSchedule schedule, String mobileNo, String token) {
        Response response = sendNotificationSMS(schedule, mobileNo, "75", token);
        handleScheduleAfterSMS(schedule, response, "75");
    }

    private void handleSMS100PercentMilestone(TargetSavingSchedule schedule, String mobileNo, String token) {
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

        if (!responseCode.equalsIgnoreCase(SUCCESS_CODE.getResponseCode())){
            String prefix = "Termination failure: ";
            TargetSavings targetSavings = schedule.getTargetSavings();
            targetSavings.setFailureReason(prefix + response1.getResponseMessage());
            targetSavingsRepository.updateTargetSavings(targetSavings);
        }

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
    private void handleScheduleAfterSMS(TargetSavingSchedule schedule, Response response, String percent) {
        String prefix = "SMS failure: ";
        schedule.setStatus(TargetSavingStatus.SUCCESS.name());
        if (response instanceof ErrorResponse){
            String message = prefix + response.getResponseMessage();
            schedule.setFailureReason(message);
        }

        TargetSavings targetSavings = schedule.getTargetSavings();

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

    private Response
    sendNotificationSMS(TargetSavingSchedule schedule, String mobileNo, String percent, String token){

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
    terminateDueTargetSavings(TargetSavingTerminationRequestPayload requestPayload, String token) {
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
            return response;
        }

       // The termination is successful and it is a PayloadResponse instance
       return responsePayload;
    }


    public Response
    sendGoalSettingDueSMS(NotificationPayload requestPayload) {

        Response response;

        String smsMessage = "Your goal is valid, you can smash it. Remember to make your contribution in 2 days time. "
                + "#GoalGetter. For Support:Call 07000222466 or WhatsApp 07045222933.";

        SMSRequestPayload smsRequest = new SMSRequestPayload();
        smsRequest.setMobileNumber(requestPayload.getMobileNumber());
        smsRequest.setAccountNumber(requestPayload.getAccountNumber());
        smsRequest.setMessage(smsMessage);
        smsRequest.setSmsFor(requestPayload.getSmsFor());
        smsRequest.setSmsType("N");
        smsRequest.setRequestId(generateRequestId());
        smsRequest.setToken(requestPayload.getToken());
        String hash = genericService.encryptPayloadToString(smsRequest, smsRequest.getToken());
        smsRequest.setHash(hash);

        SMSResponsePayload smsResponsePayload;
        String prefix;
                try{
                    smsResponsePayload= externalService
                            .sendSMS(requestPayload.getToken(), smsRequest);
                }catch (Exception exception){
                    prefix = "SMS notification failure: ";
                    response = ErrorResponse.getInstance();
                    response.setResponseCode(ResponseCodes.SERVICE_UNAVAILABLE.getResponseCode());
                    response.setResponseMessage(prefix + exception.getMessage());
                    return response;
                }

        String responseCode = smsResponsePayload.getResponseCode();

        if(responseCode.equalsIgnoreCase(ResponseCodes.SERVICE_UNAVAILABLE.getResponseCode())){
            response = ErrorResponse.getInstance();
            response.setResponseCode(responseCode);
            response.setResponseMessage(smsResponsePayload.getResponseMessage());
            return response;
        }

        if(!responseCode.equalsIgnoreCase(SUCCESS_CODE.getResponseCode())){
            response = ErrorResponse.getInstance();
            response.setResponseCode(responseCode);
            response.setResponseMessage(smsResponsePayload.getResponseMessage());
            return response;
        }

        // In this stage, the sms was sent successfully.
        PayloadResponse payloadResponse = PayloadResponse.getInstance();
        payloadResponse.setResponseCode(responseCode);
        payloadResponse.setResponseData(smsResponsePayload);
        return payloadResponse;

    }


    public CompletableFuture<Response> sendGoalSettingMilestoneSMS(NotificationPayload requestPayload) {
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
                    + "We celebrate you. Support; Call 07000222466, WhatsApp 07045222933");
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

        SMSResponsePayload smsResponsePayload = new SMSResponsePayload();
        try{
            smsResponsePayload = externalService
                    .sendSMS(requestPayload.getToken(), smsRequest);
        }catch (Exception exception){
            response = ErrorResponse.getInstance();
            String prefix = "SMS sending failure: ";
            response.setResponseCode(smsResponsePayload.getResponseCode());
            response.setResponseMessage(prefix + smsResponsePayload.getResponseMessage());
            return CompletableFuture.completedFuture(response);
        }

        String responseCode = smsResponsePayload.getResponseCode();

        if(!responseCode.equalsIgnoreCase(SUCCESS_CODE.getResponseCode())){
            response = ErrorResponse.getInstance();
            response.setResponseCode(responseCode);
            response.setResponseMessage(smsResponsePayload.getResponseMessage());
            return CompletableFuture.completedFuture(response);
        }

        // In this stage, the sms was sent successfully.
        PayloadResponse payloadResponse = PayloadResponse.getInstance();
        payloadResponse.setResponseCode(responseCode);
        payloadResponse.setResponseData(smsResponsePayload);
        return CompletableFuture.completedFuture(payloadResponse);

    }

    private Response
    executeFundsTransferForSchedule(TargetSavingSchedule schedule, Map<String, AccountDetailsResponsePayload> map, String token) {
        // Get the account details of the customer
        TargetSavings parent = schedule.getTargetSavings();
        String accountNumber = clean(parent.getAccountNumber());

        AccountDetailsResponsePayload accountDetails =
                getAccountDetailsForSchedule(schedule, map);

        String detailsResponseCode = accountDetails.getResponseCode();

        // Check if the details call was successful
        if(!detailsResponseCode.equalsIgnoreCase(SUCCESS_CODE.getResponseCode())){
            Response errorResponse = ErrorResponse.getInstance();
            errorResponse.setResponseCode(detailsResponseCode);
            errorResponse.setResponseMessage(accountDetails.getResponseMessage());
            return errorResponse;
        }

        // Call the FundsTransfer service
        String username = jwtTokenUtil.getUsernameFromToken(token);
        String requestId = genericService.generateTransRef("TCF");
        String narration = schedule.getTargetSavings().getGoalName()
                + "-CONTRIBUTION-" + schedule.getTargetSavings().getAccountNumber();

        // Create the fund transfer payload
        FundsTransferPayload fundsTransferPayload = new FundsTransferPayload();
        fundsTransferPayload.setMobileNumber(accountDetails.getMobileNumber());
        fundsTransferPayload.setAmount(schedule.getAmount());
        fundsTransferPayload.setCreditAccount(schedule.getCreditAccount());
        fundsTransferPayload.setDebitAccount(schedule.getDebitAccount());
        fundsTransferPayload.setRequestId(requestId);
        fundsTransferPayload.setTransType("ACTF");
        fundsTransferPayload.setBranchCode("NG0010068"); // Defaulted to the Digital Branch
        fundsTransferPayload.setInputter(username + "-" + accountDetails.getMobileNumber());
        fundsTransferPayload.setAuthorizer(username + "-" + accountDetails.getMobileNumber());
        fundsTransferPayload.setNoOfAuthorizer("0");
        fundsTransferPayload.setToken(token);
        fundsTransferPayload.setNarration(narration);
        String hash1 = genericService.encryptFundTransferPayload(fundsTransferPayload, token);
        fundsTransferPayload.setHash(hash1);

        String requestJson = gson.toJson(fundsTransferPayload);

        // Call the funds transfer microservice.
        FundsTransferResponsePayload fundsTransferResponsePayload = externalService
                .getFundsTransferResponse(token, requestJson);

        String prefix = "Funds transfer failure: ";

        // Check that the fund transfer was a success
        String fundResponseCode = fundsTransferResponsePayload.getResponseCode();
        if( !fundResponseCode.equalsIgnoreCase(SUCCESS_CODE.getResponseCode())){
            Response errorResponse = ErrorResponse.getInstance();
            errorResponse.setResponseCode(fundResponseCode);
            errorResponse.setResponseMessage(prefix + fundsTransferResponsePayload.getResponseMessage());
            return errorResponse;
        }

        // Now in this stage, the transfer is successful.
        PayloadResponse payloadResponse = PayloadResponse.getInstance();
        payloadResponse.setResponseCode(SUCCESS_CODE.getResponseCode());
        payloadResponse.setResponseData(fundsTransferResponsePayload);

        return payloadResponse;
    }

    private static List<TargetSavingSchedule>
    filterByDueDateAndStatus(List<TargetSavingSchedule> targetSavingSchedules) {
        return targetSavingSchedules.stream()
                .filter(schedule -> schedule.getStatus()
                        .equalsIgnoreCase(PENDING.name()) || schedule.getStatus()  // pending
                        .equalsIgnoreCase(FAILED.name()))        // failed initially
                .filter(pendingSchedule -> pendingSchedule.getDueAt()
                        .compareTo(LocalDate.now()) <= 0)
                .collect(Collectors.toList());
    }


    /**
     * The CompletableFuture to execute the missed goals and interest for those whose target savings
     * were terminated.
      */
    private void processOldSchedule(
            Map<String, AccountDetailsResponsePayload> map, String token
    ){

        // Get all the target savings that are not yet terminated.
        List<TargetSavings> targetSavingsList = targetSavingsRepository.findAllTargetSavings();

        targetSavingsList = targetSavingsList.stream()
                .filter(ts -> !ts.getStatus().equalsIgnoreCase(TERMINATED.name()))
                .collect(Collectors.toList());

        // Create a map for each of the account numbers and corresponding target savings.
        Map<String, List<TargetSavings>> accountNumberToTargetSavingsMap = getTargetSavingsListMap(targetSavingsList);

        // For each of the account numbers(corresponding to each customer, execute the schedules)
        List<String> accountNumberSet = new ArrayList<>(accountNumberToTargetSavingsMap.keySet());

        List<TargetSavingsListByAccountPayload> allTargetSavingsOfCustomer = new ArrayList<>();

        for (String accountNumber : accountNumberSet) {
            List<TargetSavings> targetSavings = accountNumberToTargetSavingsMap.get(accountNumber);
            TargetSavingsListByAccountPayload payload = new TargetSavingsListByAccountPayload();
            payload.setAccountNumber(accountNumber);
            payload.setTargetSavingsList(targetSavings);

            allTargetSavingsOfCustomer.add(payload);
        }

        allTargetSavingsOfCustomer.forEach(payload -> executeAutomaticSavingsPayout(payload, map, token));
    }


    // This method executes the pending schedules of a particular customer corresponding to the accountNumber
    private void executeAutomaticSavingsPayout(
            TargetSavingsListByAccountPayload entry,
            Map<String, AccountDetailsResponsePayload> map,
            String token
    ){
        List<TargetSavings> targetSavingsList = entry.getTargetSavingsList();
        for(TargetSavings targetSavings : targetSavingsList){
            executeMaturedTargetSavingsSchedulesPayout(targetSavings, map, token);
        }
    }

    private void executeMaturedTargetSavingsSchedulesPayout(
            TargetSavings targetSavings,
            Map<String, AccountDetailsResponsePayload> map,
            String token
    ){

        // Get all the target savings schedules for the target savings that are due for execution.
        List<TargetSavingSchedule> schedules = targetSavingsRepository
                .findAllTargetSavingSchedulesByParent(targetSavings)
                .stream()
                .filter(schedule -> schedule.getStatus().equalsIgnoreCase(PENDING.name()))
                .filter(schedule -> schedule.getDueAt().compareTo(LocalDate.now()) < 0)
                .collect(Collectors.toList());

        // A placeholder to hold the total number of all the pending schedules remaining.
        int totalPendingScheduleRemaining = schedules.size();
        System.out.println("Schedule size: " + totalPendingScheduleRemaining);

        // Perform the execution of the schedule
        for(TargetSavingSchedule schedule : schedules){
            Response response = executeFundsTransferForSchedule(schedule, map, token);

            // Log out the funds transfer response details
            System.out.println("Funds transfer response: " + gson.toJson(response));

            String prefix = "Funds transfer failure: ";

            // If the operation succeeds, set the status of the target savings.
            String responseCode = response.getResponseCode();
            if(responseCode.equalsIgnoreCase(SUCCESS_CODE.getResponseCode())){
                FundsTransferResponsePayload fundsPayload =
                        (FundsTransferResponsePayload)((PayloadResponse)response).getResponseData();

                String customerMobile = fundsPayload.getMobileNumber();
                String transactionRef = fundsPayload.getTransRef();

                // Handle the successful schedule execution (send SMS and check for termination)
                handleSuccessfulExecutionOfSchedule(schedule, customerMobile, transactionRef, token);
            }

            // If the operation fails due to insufficient balance, set the status of the target savings.
            else if(responseCode.equalsIgnoreCase(INSUFFICIENT_BALANCE.getResponseCode())){
                schedule.setStatus(MISSED.name());
                schedule.setFailureReason(prefix + response.getResponseMessage());
                targetSavingsRepository.updateTargetSavingSchedule(schedule);
            }

            // In this case, the operation failed due to internal errors.
            else {
                schedule.setFailureReason(prefix + response.getResponseMessage());
                schedule.setStatus(FAILED.name());
                targetSavingsRepository.updateTargetSavingSchedule(schedule);
            }

            totalPendingScheduleRemaining = totalPendingScheduleRemaining - 1;
        }

        BigDecimal milestoneAmount = new BigDecimal(targetSavings.getMilestoneAmount());
        if(totalPendingScheduleRemaining <= 0
                && milestoneAmount.compareTo(BigDecimal.ZERO) > 0
                && !targetSavings.getStatus().equalsIgnoreCase(TERMINATED.name())
                && targetSavings.getEndDate().compareTo(LocalDate.now()) < 0) {

            // Prepare the termination request payload.
            TargetSavingTerminationRequestPayload requestPayload =
                    new TargetSavingTerminationRequestPayload();

            requestPayload.setGoalName(targetSavings.getGoalName());
            requestPayload.setAccountNumber(targetSavings.getAccountNumber());
            requestPayload.setRequestId(generateRequestId());
            String hash = genericService.encryptPayloadToString(requestPayload, token);
            requestPayload.setHash(hash);

            Response response1 = terminateDueTargetSavings(requestPayload, token);

            System.out.println("Termination stage: " + gson.toJson(response1));

            String responseCode = response1.getResponseCode();

            if (!responseCode.equalsIgnoreCase(SUCCESS_CODE.getResponseCode())) {
                String prefix = "Termination failure: ";
                targetSavings.setFailureReason(prefix + response1.getResponseMessage());
                targetSavingsRepository.updateTargetSavings(targetSavings);
            }
        }
    }


    private Map<String, List<TargetSavings>> getTargetSavingsListMap(List<TargetSavings> allTargetSavings){

        // Create a set to hold the unique account numbers
        Set<String> accountNumbers = new TreeSet<>();

        allTargetSavings.forEach(targetSavings -> {
            accountNumbers.add(targetSavings.getAccountNumber());
        });

        List<String> uniqueAccountNumbers = accountNumbers.stream().collect(Collectors.toList());

        // For each of the unique account numbers, get all the target savings associated with it
        Map<String, List<TargetSavings>> result = new HashMap<>();
        uniqueAccountNumbers
                .forEach(accountNumber -> {
                    List<TargetSavings> targetSavingsListForAccountNumber = targetSavingsRepository
                            .findAllTargetSavingsByAccountNumber(accountNumber);
                    result.put(accountNumber, targetSavingsListForAccountNumber);
                });

        return result;
    }


}

