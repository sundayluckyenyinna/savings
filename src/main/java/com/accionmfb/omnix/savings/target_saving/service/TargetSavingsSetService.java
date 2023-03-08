package com.accionmfb.omnix.savings.target_saving.service;

import com.accionmfb.omnix.savings.target_saving.constant.*;
import com.accionmfb.omnix.savings.target_saving.dto.ErrorResponse;
import com.accionmfb.omnix.savings.target_saving.dto.PayloadResponse;
import com.accionmfb.omnix.savings.target_saving.dto.Response;
import com.accionmfb.omnix.savings.target_saving.jwt.JwtTokenUtil;
import com.accionmfb.omnix.savings.target_saving.model.TargetSavingSchedule;
import com.accionmfb.omnix.savings.target_saving.model.TargetSavings;
import com.accionmfb.omnix.savings.target_saving.payload.request.FundsTransferPayload;
import com.accionmfb.omnix.savings.target_saving.payload.request.TargetSavingsRequestPayload;
import com.accionmfb.omnix.savings.target_saving.payload.response.*;
import com.accionmfb.omnix.savings.target_saving.repository.TargetSavingsRepository;
import com.google.gson.Gson;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.accionmfb.omnix.savings.target_saving.util.TargetSavingsUtils.clean;

/**
 * This service class only handles the request from the client to set a target saving goal.
 * Once the goal is set, automatic funds transfer is done from the customer's account to the Accion MFB
 * pool account and further automatic monitoring, orchestration and execution is done underground.
 */
@Service
public class TargetSavingsSetService
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

    @Value("${targetSavings.contribution.minimum.daily}")
    private BigDecimal minimumDailyContribution;

    @Value("${targetSavings.contribution.minimum.weekly}")
    private BigDecimal minimumWeeklyContribution;

    @Value("${targetSavings.contribution.minimum.weekly}")
    private BigDecimal minimumMonthlyContribution;

    @Value("${omnix.target.savings.poolaccount}")
    private String targetSavingsPoolAccount;

    @Value("${targetSavings.interestRate.lessOrThreeMonth}")
    private String interestLessThanOrThreeMonth;

    @Value("${targetSavings.interestRate.fourToSixMonth}")
    private String interestFourToSixMonth;

    @Value("${targetSavings.interestRate.sevenToNine}")
    private String interestSevenToNine;

    @Value("${targetSavings.interestRate.tenToTwelve}")
    private String interestTenToTwelve;


    public Response processSetTargetSavings(String token, TargetSavingsRequestPayload requestPayload) {

        Response response;

        String username = jwtTokenUtil.getUsernameFromToken(token);
        String channel = jwtTokenUtil.getChannelFromToken(token);

        // Generate log for the request.
        genericService.generateLog("Target Savings Set", token, channel, "API Request", "INFO", requestPayload.getRequestId());

        // Check for errors and return to the client if there be any.
        Optional<ErrorResponse> errorResponses = this.handleAllErrors(requestPayload, token);
        if(errorResponses.isPresent()){
            // Log the error
            genericService.generateLog("Target Savings Set Payload error", token, username + ": " + errorResponses.get().getResponseMessage(), "API Request", "DEBUG", requestPayload.getRequestId());
            return errorResponses.get();
        }

        // Create the target saving model from the request payload.
        TargetSavings targetSavings =
                createTargetSavingsModelFromTargetSavingsRequest(requestPayload);

        // Asynchronously save the target saving goal and all schedules and execute first debit transfer
        TargetSavings createdTargetSavings = targetSavingsRepository.saveTargetSavings(targetSavings);

        try{
           response = createAllSchedulesForTargetSavings(createdTargetSavings)
                    .whenCompleteAsync((firstSchedule, error) -> {
                        if(error != null)
                            genericService.generateLog("Target Savings Set", token, error.getMessage(), "API Request", "DEBUG", requestPayload.getRequestId());
                    })
                    .thenApplyAsync((firstSchedule) ->
                            executeFirstContribution
                                    (firstSchedule, requestPayload.getMobileNumber(), token))
                   .get().get();
        }catch (Exception exception){
            // Log the error
            genericService.generateLog("Target Savings Set error", token, "Program error", "API Request", "DEBUG", requestPayload.getRequestId());
            exception.printStackTrace();
            response = ErrorResponse.getInstance();
            response.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            response.setResponseMessage(exception.getMessage());
            return response;
        }

        // Check that the response is not an error. The first execution funds transfer must not fail.
        if (response instanceof ErrorResponse){
            // Log the error
            genericService.generateLog("Target Savings Set", token, response.getResponseMessage(), "API Request", "DEBUG", requestPayload.getRequestId());
            targetSavingsRepository.revertFromTargetSavingsSetup(createdTargetSavings);
            return response;
        }

        // Now, the response is surely a payload response.
        FundsTransferResponsePayload transferResponsePayload =
                (FundsTransferResponsePayload) ((PayloadResponse)response).getResponseData();

        String transactionRef = transferResponsePayload.getTransRef();

        // Get the customer details
        CustomerDetailsResponsePayload customerDetails = externalService
                .getCustomerDetailsFromCustomerService(requestPayload, token);

        // Get the currently saved target details
        TargetSavings currentSavings = targetSavingsRepository
                .findTargetSavingsByRequestId(requestPayload.getRequestId()).get();

        // Create the target savings response payload
        TargetSavingsResponsePayload responsePayload =
                createTargetSavingsResponsePayload(transactionRef, currentSavings, customerDetails);

        PayloadResponse res = PayloadResponse.getInstance();
        res.setResponseCode(responsePayload.getResponseCode());
        res.setResponseData(responsePayload);
        return res;
    }


    public  TargetSavingsResponsePayload
    createTargetSavingsResponsePayload(
            String transactionRef,
            TargetSavings targetSavings,
            CustomerDetailsResponsePayload customerDetails
    )
    {
        TargetSavingsResponsePayload responsePayload = new TargetSavingsResponsePayload();
        responsePayload.setId(targetSavings.getId().toString());
        responsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
        responsePayload.setSavingsAmount(targetSavings.getSavingsAmount());
        responsePayload.setTargetAmount(targetSavings.getTargetAmount());
        responsePayload.setDueAt(targetSavings.getEndDate().toString());
        responsePayload.setEndDate(targetSavings.getEndDate().toString());
        responsePayload.setGoalName(targetSavings.getGoalName());
        responsePayload.setAccountNumber(targetSavings.getAccountNumber());
        responsePayload.setInterestRate(targetSavings.getInterestRate());
        responsePayload.setContributionFrequency(targetSavings.getFrequency());
        responsePayload.setCustomerName(String
                .join(" ", customerDetails.getLastName(), customerDetails.getOtherName()));
        responsePayload.setMilestoneAmount(targetSavings.getMilestoneAmount());
        responsePayload.setMilestonePercentage(targetSavings.getMilestonePercent());
        responsePayload.setStatus(targetSavings.getStatus());
        responsePayload.setMobileNumber(customerDetails.getMobileNumber());
        responsePayload.setRefId(transactionRef);
        responsePayload.setTenorInMonths(targetSavings.getTenorInMonth());
        responsePayload.setStartDate(targetSavings.getStartDate().toString());
        responsePayload.setTotalMissedAmount("0");
        responsePayload.setInterestRate(targetSavings.getInterestRate());
        responsePayload.setEarliestTerminationDate(targetSavings
                .getEarliestTerminationDate().toString());

        return responsePayload;
    }

    // Validators
    private boolean isValidateSavingsAmountByFrequency(String savingsAmount, String frequency){
        BigDecimal minimumContribution = this.getMinimumContributionByFrequency(frequency);
        BigDecimal incomingSavingsAmount = new BigDecimal(savingsAmount);
        return incomingSavingsAmount.compareTo(minimumContribution) >= 0;
    }

    private boolean isValidStartDate(String startDateString){
        LocalDate startDate = LocalDate.parse(startDateString);
        LocalDate now = LocalDate.now();
        return startDate.compareTo(now) >= 0;
    }

    private boolean isUniqueRequestId(String requestId){
        boolean isUniqueId = targetSavingsRepository.findTargetSavingsByRequestId(requestId).isEmpty();
        return isUniqueId;
    }

    private String isCustomerExist(TargetSavingsRequestPayload requestPayload, String token){
        CustomerDetailsResponsePayload customerDetails = externalService
                .getCustomerDetailsFromCustomerService(requestPayload, token);

        String responseCode = customerDetails.getResponseCode();
        String responseMessage = customerDetails.getResponseMessage();

        String result;

        if(responseCode.equalsIgnoreCase(ResponseCodes.SERVICE_UNAVAILABLE.getResponseCode()))
            result = String.join("#", "false", responseMessage);
        else if(responseCode.equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()))
            result = String.join("#", "true", "Customer exists");
        else
            result = String.join("#", "false", responseMessage);

        return result;
    }


    private boolean isCustomerActive(TargetSavingsRequestPayload requestPayload, String token){
        String status = externalService.getCustomerStatus(requestPayload, token);
        return status.equalsIgnoreCase(CustomerStatus.ACTIVE.name());
    }

    private String isAccountExist(TargetSavingsRequestPayload requestPayload, String token){
        AccountDetailsResponsePayload accountDetails = externalService
                .getAccountDetailsFromAccountService(requestPayload, token);

        String responseCode = accountDetails.getResponseCode();
        String responseMessage = accountDetails.getResponseMessage();

        String result;

        if( responseCode.equalsIgnoreCase(ResponseCodes.SERVICE_UNAVAILABLE.getResponseCode()) )
            result = String.join("#", "false", responseMessage);
        else if (responseCode.equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) )
            result = String.join("#", "true", "Account exists");
        else
            result = String.join("#", "false", responseMessage);

        return result;
    }

    private boolean isAccountActive(TargetSavingsRequestPayload requestPayload, String token){
        String status = externalService.getAccountStatus(requestPayload, token);
        return status.equalsIgnoreCase(AccountStatus.SUCCESS.name());
    }

    private boolean isAccountBalanceSufficient(TargetSavingsRequestPayload requestPayload, String token){

        // Get the account balance details
        AccountBalanceResponsePayload balanceResponsePayload =
                externalService.getAccountBalanceFromAccountService(requestPayload, token);

        String balanceString = clean(balanceResponsePayload.getAvailableBalance());

        if (balanceString == null)
            return false;

        BigDecimal availableBalance = new BigDecimal(balanceString);
        BigDecimal requiredStartSaving = new BigDecimal(requestPayload.getSavingsAmount());

        return availableBalance.compareTo(requiredStartSaving) >= 0;
    }

    private boolean isTargetSavingsAlreadyExisting(String goalName, String accountNumber){
        return targetSavingsRepository.findTargetSavingsByGoalNameAndAccountNumber(goalName, accountNumber)
                .isPresent();
    }

    // Utility methods

    private BigDecimal getMinimumContributionByFrequency(String frequency){
        frequency = frequency.trim().toUpperCase();
        if (frequency.equalsIgnoreCase(TargetSavingsFrequency.DAILY.name()))
            return minimumDailyContribution;
        else if (frequency.equalsIgnoreCase(TargetSavingsFrequency.WEEKLY.name()))
            return minimumWeeklyContribution;
        else if (frequency.equalsIgnoreCase(TargetSavingsFrequency.MONTHLY.name()))
            return minimumMonthlyContribution;
        else
            return BigDecimal.ZERO;
    }

    private static int getContributionCountPerMonth(String frequency){

        if (frequency.equalsIgnoreCase(TargetSavingsFrequency.DAILY.name()))
            return 30;
        else if(frequency.equalsIgnoreCase(TargetSavingsFrequency.WEEKLY.name()))
            return 4;
        else if (frequency.equalsIgnoreCase(TargetSavingsFrequency.MONTHLY.name()))
            return 1;
        else
            return 0;
    }

    private static LocalDate getEndDate(LocalDate startDate, String tenor){
        return startDate.plusMonths(Long.valueOf(tenor));
    }

    private static Long getEarliestTerminationPeriod(String tenor){
        return Math.round((0.333 * Long.valueOf(tenor)));
    }

    private static LocalDate getEarliestTerminationDate(LocalDate startDate, String tenor){
        return startDate.plusMonths(getEarliestTerminationPeriod(tenor));
    }

    private static int getTotalContributionTimes(String frequency, int tenor){
        return tenor * getContributionCountPerMonth(frequency);
    }

    private String getInterestRate(String tenorString){
        int tenor = Integer.parseInt(tenorString);
        String interestRate = "";
        if (tenor <= 3) {
            interestRate = interestLessThanOrThreeMonth;
        } else if (tenor >= 4 && tenor <= 6) {
            interestRate = interestFourToSixMonth;                                         // set all to properties file.
        } else if (tenor >= 7 && tenor <= 9) {
            interestRate = interestSevenToNine;
        } else if (tenor >= 10 && tenor <= 12) {
            interestRate = interestTenToTwelve;
        }
        return interestRate;
    }

    private static String getTotalAmountSavable(TargetSavingsRequestPayload payload){
        int contributionCountPerMonth = getContributionCountPerMonth(payload.getFrequency());
        BigDecimal contribution = new BigDecimal(clean(payload.getSavingsAmount()));
        return contribution
                .multiply(BigDecimal.valueOf(Long.valueOf(contributionCountPerMonth)))
                .multiply(BigDecimal.valueOf(Long.valueOf(clean(payload.getTenor()))))
                .setScale(2, RoundingMode.FLOOR)
                .toString();
    }

    private static LocalDate getDueDateOfScheduleByFrequencyAndPosition
            (LocalDate startDate, String frequency, int position)
    {
        if(frequency.equalsIgnoreCase(TargetSavingsFrequency.DAILY.name())){
            if (position == 1)
                return startDate;   // this schedule will not be affected by the scheduler. it will be done right away.
            else
                return startDate.plusDays(position - 1);
        }
        else if(frequency.equalsIgnoreCase(TargetSavingsFrequency.WEEKLY.name())){
            if (position == 1)
                return startDate;
            else
                return startDate.plusWeeks(position - 1);
        }
        else if (frequency.equalsIgnoreCase(TargetSavingsFrequency.MONTHLY.name())){
            if(position == 1)
                return startDate;
            else
                return startDate.plusMonths(position - 1);
        }
        else
            return startDate.plusYears(0);   // For now, this code will not be reached.
    }


    // Error handling
    /**
     * This method handles all the business logical errors that might occur from a request to set
     * the target saving. If there is a specific error, it returns it in an enclosed Optional object.
     * If there are no errors, it simply returns an empty Optional.
     * @return
     */
    Optional<ErrorResponse> handleAllErrors(TargetSavingsRequestPayload requestPayload, String token)
    {
        ErrorResponse errorResponse = ErrorResponse.getInstance();
        errorResponse.setResponseCode(ResponseCodes.FAILED_MODEL.getResponseCode());
        String message;

        // Validate the customer existence
        String existenceResult = isCustomerExist(requestPayload, token);
        String[] existenceArray = existenceResult.split("#");
        boolean existenceBoolean = Boolean.parseBoolean(existenceArray[0]);
        String reason = existenceArray[1];
        if(!existenceBoolean){
            errorResponse.setResponseMessage(reason);
            return Optional.ofNullable(errorResponse);
        }

        // Validate the account existence
        String accountExistenceResult = isAccountExist(requestPayload, token);
        String[] accountExistenceArray = accountExistenceResult.split("#");
        boolean accountExistenceBoolean = Boolean.parseBoolean(accountExistenceArray[0]);
        String accountReason = accountExistenceArray[1];
        if(!accountExistenceBoolean){
            errorResponse.setResponseMessage(accountReason);
            Optional.ofNullable(errorResponse);
            return Optional.ofNullable(errorResponse);
        }

        // Get the Account details
        AccountDetailsResponsePayload accountDetails =
                externalService.getAccountDetailsFromAccountService(requestPayload, token);

        // Validate the start date.
        if(!isValidStartDate(requestPayload.getStartDate())){
            message = genericService.getMessageOfTargetSavings("pastdays");
            errorResponse.setResponseMessage(message);
            Optional.ofNullable(errorResponse);
            return Optional.ofNullable(errorResponse);
        }

        // Validate the savings amount
        if (!isValidateSavingsAmountByFrequency(
                requestPayload.getSavingsAmount(), requestPayload.getFrequency())
        )
        {
            message = genericService
                    .getMessageOfTargetSavings("amount.required",
                            new String[]{requestPayload.getFrequency(),
                                    getMinimumContributionByFrequency(requestPayload.getFrequency()).toString()});
            errorResponse.setResponseMessage(message);
            Optional.ofNullable(errorResponse);
            return Optional.ofNullable(errorResponse);
        }

        // Validate the requestId is unique
        if( !isUniqueRequestId(requestPayload.getRequestId())){
            message = genericService.getMessageOfRequest(
                    "sameid", new String[]{requestPayload.getRequestId()});
            errorResponse.setResponseMessage(message);
            Optional.ofNullable(errorResponse);
            return Optional.ofNullable(errorResponse);
        }

        // Validate the activeness of the customer
        if( !isCustomerActive(requestPayload, token) ){
            message = genericService.getMessageOfCustomer("inactive");
            errorResponse.setResponseMessage(message);
            Optional.ofNullable(errorResponse);
            return Optional.ofNullable(errorResponse);
        }

        // Validate the activeness of the account
        if( !isAccountActive(requestPayload, token) ){
            message = genericService.getMessageOfAccount("inactive",
                    new String[]{requestPayload.getAccountNumber()});
            errorResponse.setResponseMessage(message);
            Optional.ofNullable(errorResponse);
            return Optional.ofNullable(errorResponse);
        }

        // Validate the type of the account. The account category should point to 'Save Brighta'.
        if( !accountDetails.getCategory().equalsIgnoreCase(AccountProductType.SAVEBRIGHTA.categoryCode) ){
            message = genericService.getMessageOfAccount("notsavebrighta");
            errorResponse.setResponseMessage(message);
            Optional.ofNullable(errorResponse);
            return Optional.ofNullable(errorResponse);
        }

        if (
                LocalDate.parse(requestPayload.getStartDate()).getDayOfWeek().name()
                        .equalsIgnoreCase(DaysOfWeek.SATURDAY.name())
                || LocalDate.parse(requestPayload.getStartDate()).getDayOfWeek().name()
                        .equalsIgnoreCase(DaysOfWeek.SUNDAY.name())
        ){
            message = genericService.getMessageOfTargetSavings("weekend");
            errorResponse.setResponseMessage(message);
            Optional.ofNullable(errorResponse);
            return Optional.ofNullable(errorResponse);
        }

        // Check that the target savings has not been saved by the same account number.
        if(isTargetSavingsAlreadyExisting(requestPayload.getGoalName(), requestPayload.getAccountNumber())){
            message = genericService.getMessageOfTargetSavings("exist",
                    new String[]{requestPayload.getGoalName(), requestPayload.getAccountNumber()});
            errorResponse.setResponseMessage(message);
            Optional.ofNullable(errorResponse);
            return Optional.ofNullable(errorResponse);
        }

        // Validate that there is sufficient account balance to start the savings
        if (!isAccountBalanceSufficient(requestPayload, token)) {
            message = genericService.getMessageOfAccount("insufficientbalance",
                    new String[]{requestPayload.getFrequency()});
            errorResponse.setResponseMessage(message);
            return Optional.ofNullable(errorResponse);
        }

        // If all the above passes, return an empty Optional.
        return Optional.empty();
    }


    public TargetSavings
    createTargetSavingsModelFromTargetSavingsRequest(TargetSavingsRequestPayload requestPayload)
    {
        TargetSavings targetSavings = new TargetSavings();
        targetSavings.setCreatedAt(LocalDateTime.now());                        // createdAt
        targetSavings.setAccountNumber(requestPayload.getAccountNumber());      // accountNumber
        targetSavings.setFrequency(requestPayload.getFrequency());              // frequency
        targetSavings.setSavingsAmount(requestPayload.getSavingsAmount());      // savings amount
        targetSavings.setEarliestTerminationDate(getEarliestTerminationDate(
                LocalDate.parse(requestPayload.getStartDate()), requestPayload.getTenor()
        ));
        targetSavings.setEndDate(getEndDate(
                LocalDate.parse(requestPayload.getStartDate()), requestPayload.getTenor())
        );
        targetSavings.setGoalName(requestPayload.getGoalName());
        targetSavings.setInterestRate(getInterestRate(requestPayload.getTenor()));
        targetSavings.setInterestAccrued("0");
        targetSavings.setInterestPaid(false);
        targetSavings.setInterestPaidAt(LocalDate.parse("1900-01-01"));
        targetSavings.setMilestoneAmount("0");
        targetSavings.setMilestonePercent("0");
        targetSavings.setRequestId(requestPayload.getRequestId());
        targetSavings.setStartDate(LocalDate.parse(requestPayload.getStartDate()));
        targetSavings.setStatus(TargetSavingStatus.SUCCESS.name());
        targetSavings.setTargetAmount(getTotalAmountSavable(requestPayload));
        targetSavings.setTenorInMonth(requestPayload.getTenor());
        targetSavings.setTerminatedBy(Strings.EMPTY);
        targetSavings.setTerminationDate(LocalDate.parse("1900-01-01"));
        targetSavings.setTimePeriod(genericService.getTimePeriod());
        targetSavings.setTransRef(Strings.EMPTY);
        targetSavings.setTotalInterest("");
        targetSavings.setDailyInterest("");

        return targetSavings;
    }

    // CREATION OF SCHEDULES

    /**
     * This method creates all target savings schedules associated with a given target saving goal.
     * This creation might be a long process and is therefore ran under the hood in another thread in
     * a CompletableFuture instance and then its result is awaited.
     * The result of this execution is the first TargetSavingSchedule object that will be created.
     * The first target saving schedule is returned to the main thread so that it can be executed
     * for the first funds transfer from customer account (SaveBrighta) to the Omnix pool account.
     *
     * @param targetSavings
     * @return firstTargetSavingSchedule : TargetSavingSchedule.
     */
    private CompletableFuture<TargetSavingSchedule>
    createAllSchedulesForTargetSavings(TargetSavings targetSavings)
    {
        // Get the total contribution times
        String frequency = targetSavings.getFrequency();
        String tenor = targetSavings.getTenorInMonth();
        int totalContributionTimes = getTotalContributionTimes(frequency, Integer.parseInt(tenor));

        TargetSavingSchedule firstSavingSchedule = null;

        // Create the schedule model table for all the contribution times.
        for (int i = 1; i <= totalContributionTimes; i++){

            // Get the due date for the schedule
            LocalDate dueDate = getDueDateOfScheduleByFrequencyAndPosition(
                    targetSavings.getStartDate(), targetSavings.getFrequency(), i
            );

            TargetSavingSchedule targetSavingSchedule = new TargetSavingSchedule();
            targetSavingSchedule.setCreatedAt(LocalDateTime.now());
            targetSavingSchedule.setAmount(targetSavings.getSavingsAmount());
            targetSavingSchedule.setCreditAccount(targetSavingsPoolAccount);
            targetSavingSchedule.setDebitAccount(targetSavings.getAccountNumber());
            targetSavingSchedule.setDueAt(dueDate);
            targetSavingSchedule.setExecutedAt(LocalDate.parse("1900-01-01")); // hard coding
            targetSavingSchedule.setFailureReason("");
            targetSavingSchedule.setSmsDueDate(dueDate.minusDays(2)); // hard coding
            targetSavingSchedule.setSmsDueSend(false);
            targetSavingSchedule.setStatus(TargetSavingStatus.PENDING.name());
            targetSavingSchedule.setT24TransRef("");
            targetSavings.setTimePeriod(genericService.getTimePeriod());
            targetSavingSchedule.setTargetSavings(targetSavings);

            // Persist the schedule.
            TargetSavingSchedule currentSchedule =
                    targetSavingsRepository.saveTargetSavingSchedule(targetSavingSchedule);

            // Return the first schedule to be executed automatically.
            if( i == 1 ){
                firstSavingSchedule = currentSchedule;
            }
        }

        return CompletableFuture.completedFuture(firstSavingSchedule);
    }

    // Execute first debit transfer contribution
    private CompletableFuture<Response>
    executeFirstContribution(TargetSavingSchedule schedule, String mobileNo, String token)
    {

        String username = jwtTokenUtil.getUsernameFromToken(token);
        String requestId = genericService.generateTransRef("TCF");
        String narration = schedule.getTargetSavings().getGoalName()
                + "-CONTRIBUTION-" + schedule.getTargetSavings().getAccountNumber();

        // Create the fund transfer payload
        FundsTransferPayload fundsTransferPayload = new FundsTransferPayload();
        fundsTransferPayload.setMobileNumber(mobileNo);
        fundsTransferPayload.setAmount(clean(schedule.getAmount()));
        fundsTransferPayload.setCreditAccount(schedule.getCreditAccount());
        fundsTransferPayload.setDebitAccount(schedule.getDebitAccount());
        fundsTransferPayload.setTransType("ACTF");
        fundsTransferPayload.setBranchCode("NG0010068"); // Defaulted to the Digital Branch
        fundsTransferPayload.setInputter(username + "-" + mobileNo);
        fundsTransferPayload.setAuthorizer(username + "-" + mobileNo);
        fundsTransferPayload.setNoOfAuthorizer("0");
        fundsTransferPayload.setRequestId(genericService.generateTransRef("TS"));
        fundsTransferPayload.setToken(token);
        fundsTransferPayload.setNarration(narration);
        String hash = genericService.encryptFundTransferPayload(fundsTransferPayload, token);
        fundsTransferPayload.setHash(hash);

        String requestJson = gson.toJson(fundsTransferPayload);

        // Call the funds transfer microservice.
        FundsTransferResponsePayload fundsTransferResponsePayload = externalService
                .getFundsTransferResponse(token, requestJson);

        // Check that the service was even reached in the first place.
        String responseCode = fundsTransferResponsePayload.getResponseCode();

        // Now check if the transfer was not successful.
        if( !responseCode.equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())){
            Response errorResponse = ErrorResponse.getInstance();
            errorResponse.setResponseCode(responseCode);
            String prefix = "Funds transfer failure: ";
            errorResponse.setResponseMessage(prefix + fundsTransferResponsePayload.getResponseMessage());
            schedule.setFailureReason(prefix + fundsTransferResponsePayload.getResponseMessage());
            // Log the error
            genericService.generateLog("Target Savings Set", token, prefix + fundsTransferResponsePayload.getResponseCode(), "API Funds transfer request", "DEBUG", requestId);
            targetSavingsRepository.updateTargetSavingSchedule(schedule);
            return CompletableFuture.completedFuture(errorResponse);
        }

        // Now the transaction was successful
        PayloadResponse payloadResponse = PayloadResponse.getInstance();
        payloadResponse.setResponseCode(responseCode);
        payloadResponse.setResponseData(fundsTransferResponsePayload);

        // Update the parent target savings
        TargetSavings parent = schedule.getTargetSavings();
        parent.setMilestoneAmount(clean(schedule.getAmount()));

        BigDecimal totalSavable = new BigDecimal(clean(schedule.getAmount()))
                .multiply(new BigDecimal(getContributionCountPerMonth(parent.getFrequency())))
                .multiply(new BigDecimal(clean(parent.getTenorInMonth())))
                .setScale(5, RoundingMode.FLOOR);

        BigDecimal firstSaved = new BigDecimal(clean(schedule.getAmount()));
        BigDecimal milestonePercent = firstSaved
                .multiply(new BigDecimal("100"))
                .divide(totalSavable, 5, RoundingMode.FLOOR)
                .setScale(5, RoundingMode.FLOOR);

        parent.setMilestonePercent(milestonePercent.toString());

        double[] interestComponents = genericService.calculateInterestComponentsForTargetSavings(
                        clean(parent.getSavingsAmount()),
                getInterestRate(clean(parent.getTenorInMonth())),
                        parent.getFrequency(),
                parent.getTenorInMonth()
        );

        double dailyInterest = interestComponents[1];
        double totalInterest = interestComponents[0];

        parent.setContributionCountForMonth(1);
        parent.setInterestAccrued(String.valueOf(dailyInterest * 1));
        parent.setDailyInterest(String.valueOf(dailyInterest));
        parent.setTotalInterest(String.valueOf(totalInterest));
        targetSavingsRepository.updateTargetSavings(parent);

        // Update the TargetSavingSchedule.
        schedule.setExecutedAt(LocalDate.now());
        schedule.setStatus(TargetSavingStatus.SUCCESS.name());
        schedule.setT24TransRef(fundsTransferResponsePayload.getT24TransRef());
        targetSavingsRepository.updateTargetSavingSchedule(schedule);

        return CompletableFuture.completedFuture(payloadResponse);
    }


}
