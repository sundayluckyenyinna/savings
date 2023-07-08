package com.accionmfb.omnix.savings.target_saving.service;

import com.accionmfb.omnix.savings.target_saving.constant.Constants;
import com.accionmfb.omnix.savings.target_saving.dto.CurrentDateTime;
import com.accionmfb.omnix.savings.target_saving.jwt.JwtTokenUtil;
import com.accionmfb.omnix.savings.target_saving.payload.request.*;
import com.google.gson.Gson;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * This provides the core implementation of the Generic service interface.
 */
@Slf4j
@Service
public class GenericServiceImpl implements GenericService
{
    private static SecretKeySpec secretKey;
    private static byte[] key;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private MessageSource messageSource;

    @Value("${time.url}")
    private String timeUrl;

    @Value("${omnix.start.morning}")
    private String startMorning;
    @Value("${omnix.end.morning}")
    private String endMorning;
    @Value("${omnix.start.afternoon}")
    private String startAfternoon;
    @Value("${omnix.end.afternoon}")
    private String endAfternoon;
    @Value("${omnix.start.evening}")
    private String startEvening;
    @Value("${omnix.end.evening}")
    private String endEvening;
    @Value("${omnix.start.night}")
    private String startNight;
    @Value("${omnix.end.night}")
    private String endNight;

    private static final Gson gson = new Gson();

    Logger logger = LoggerFactory.getLogger(GenericServiceImpl.class);

    /** LOGGING AND USER ACTIVITY */

    /** TOKEN AND AUTH GENERIC/UTILITY SERVICES */

    @Override
    public String generateTransRef(String transType) {
        long number = (long) Math.floor(Math.random() * 9_000_000_000L) + 1_000_000_000L;
        return transType + number;
    }
    @Override
    public String decryptString(String textToDecrypt, String encryptionKey) {
        try {
            String secret = encryptionKey.trim();
            setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            String decryptedResponse = new String(cipher.doFinal(java.util.Base64.getDecoder().decode(textToDecrypt.trim())));
            String[] splitString = decryptedResponse.split(":");
            StringJoiner rawString = new StringJoiner(":");
            for (String str : splitString) {
                rawString.add(str.trim());
            }
            return rawString.toString();
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    @Override
    public String encryptString(String textToEncrypt, String token) {
        String encryptionKey = jwtTokenUtil.getEncryptionKeyFromToken(token);
        try {
            String secret = encryptionKey.trim();
            setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return java.util.Base64.getEncoder().encodeToString(cipher.doFinal(textToEncrypt.trim().getBytes("UTF-8")));
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    @Override
    public String encryptPayloadToString(Object payload, String token){
        String rawString = this.getJoinedPayloadValues(payload);
        String encryptedString = this.encryptString(rawString, token);
        return encryptedString;
    }

    @Override
    public String getJoinedPayloadValues(Object payload) {
        Field[] fields = payload.getClass().getDeclaredFields();

        // Remove those fields whose names are 'hash' and 'token' and 'smsType'.
        String concatValues = Arrays.stream(fields)
                .filter(field -> !field.getName().equalsIgnoreCase("hash"))
                .filter(field -> !field.getName().equalsIgnoreCase("token"))
                .filter(field -> !field.getName().equalsIgnoreCase("smsType"))
                .filter(field -> !field.getName().equalsIgnoreCase("imei"))// for SMS payload
                .map(field -> {
                    field.setAccessible(true);
                    String fieldValue = Strings.EMPTY;
                    try { fieldValue = String.valueOf(field.get(payload)).trim(); }
                    catch (IllegalAccessException e) {}
                    return fieldValue;
                })
                .filter(value -> value != null )
                .filter(value -> !value.equalsIgnoreCase("null"))
                .collect(Collectors.joining(Constants.RAW_STRING_DELIMITER));
        return concatValues;
    }

    public static void setKey(String myKey) {
        MessageDigest sha = null;
        try {
            key = myKey.getBytes("UTF-8");
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            secretKey = new SecretKeySpec(key, "AES");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public char getTimePeriod() {
        char timePeriod = 'M';
        int hour = LocalDateTime.now().getHour();
        int morningStart = Integer.valueOf(startMorning);
        int morningEnd = Integer.valueOf(endMorning);
        int afternoonStart = Integer.valueOf(startAfternoon);
        int afternoonEnd = Integer.valueOf(endAfternoon);
        int eveningStart = Integer.valueOf(startEvening);
        int eveningEnd = Integer.valueOf(endEvening);
        int nightStart = Integer.valueOf(startNight);
        int nightEnd = Integer.valueOf(endNight);

        //Check the period of the day
        if (hour >= morningStart && hour <= morningEnd) {
            timePeriod = 'M';
        }
        if (hour >= afternoonStart && hour <= afternoonEnd) {
            timePeriod = 'A';
        }
        if (hour >= eveningStart && hour <= eveningEnd) {
            timePeriod = 'E';
        }
        if (hour >= nightStart && hour <= nightEnd) {
            timePeriod = 'N';
        }
        return timePeriod;
    }



    /** COMMON MESSAGE SOURCES SERVICES  */

    @Override
    public String getMessageOfAccount(String suffix){
        return messageSource.getMessage(Constants.MESSAGE_BASE_ROUTE_ACCOUNT + suffix,
                new Object[0], Locale.ENGLISH);
    }

    @Override
    public String getMessageOfAccount(String suffix, Object[] parameters){
        return messageSource.getMessage(Constants.MESSAGE_BASE_ROUTE_ACCOUNT + suffix,
                parameters, Locale.ENGLISH);
    }

    @Override
    public String getMessageOfCustomer(String suffix){
        return messageSource.getMessage(Constants.MESSAGE_BASE_ROUTE_CUSTOMER + suffix,
                new Object[0], Locale.ENGLISH);
    }

    @Override
    public String getMessageOfCustomer(String suffix, Object[] parameters){
        return messageSource.getMessage(Constants.MESSAGE_BASE_ROUTE_CUSTOMER + suffix,
                parameters, Locale.ENGLISH);
    }

    @Override
    public String getMessageOfFunds(String suffix){
        return messageSource.getMessage(Constants.MESSAGE_BASE_ROUTE_FUNDS + suffix,
                new Object[0], Locale.ENGLISH);
    }

    @Override
    public String getMessageOfFunds(String suffix, Object[] parameters){
        return messageSource.getMessage(Constants.MESSAGE_BASE_ROUTE_FUNDS + suffix,
                parameters, Locale.ENGLISH);
    }

    @Override
    public String getMessageOfNotification(String suffix){
        return messageSource.getMessage(Constants.MESSAGE_BASE_ROUTE_NOTIFICATION + suffix,
                new Object[0], Locale.ENGLISH);
    }

    @Override
    public String getMessageOfNotification (String suffix, Object[] parameters){
        return messageSource.getMessage(Constants.MESSAGE_BASE_ROUTE_NOTIFICATION + suffix,
                parameters, Locale.ENGLISH);
    }

    @Override
    public String getMessageOfRequest(String suffix) {
        return messageSource.getMessage(Constants.MESSAGE_BASE_ROUTE_REQUEST + suffix,
                new Object[0], Locale.ENGLISH);
    }

    @Override
    public String getMessageOfRequest(String suffix, Object[] parameters) {
        return messageSource.getMessage(Constants.MESSAGE_BASE_ROUTE_REQUEST + suffix,
                parameters, Locale.ENGLISH);
    }

    @Override
    public String getMessageOfTargetSavings(String suffix){
        return messageSource.getMessage(Constants.MESSAGE_BASE_ROUTE_TARGET + suffix,
                new Object[0], Locale.ENGLISH);
    }

    @Override
    public String getMessageOfTargetSavings(String suffix, Object[] parameters){
        return messageSource.getMessage(Constants.MESSAGE_BASE_ROUTE_TARGET + suffix,
                parameters, Locale.ENGLISH);
    }

    @Override
    public double[] calculateInterestComponentsForTargetSavings(
            String contributionAmount, String interestRate,
            String frequency, String tenor
    )
    {
        double[] response = {0, 0};
        try {
            double rate = Double.parseDouble(interestRate) / 100;
            double rateByTime = 0D;
            double rateByTimePlusOne = 0D;
            double rateByTimeExponential = 0D;
            double rateByTimeExpoMinusOne = 0D;
            double rateByTimeExpoDivide = 0D;
            double futureValue = 0D;
            double totalContribution = 0D;
            double totalInterest = 0D;
            double dailyInterest = 0D;
            if (frequency.equalsIgnoreCase("Monthly")) {
                rateByTime = rate / 12;
                totalContribution = Double.parseDouble(contributionAmount) * Double.parseDouble(tenor);
            } else if (frequency.equalsIgnoreCase("Weekly")) {
                rateByTime = rate / 52;
                totalContribution = Double.parseDouble(contributionAmount) * Double.parseDouble(tenor) * 4; // Assume 4weeks;
            } else {
                rateByTime = rate / 365;
                totalContribution = Double.parseDouble(contributionAmount) * Double.parseDouble(tenor) * 30;  //Assume 30 days
            }

            rateByTimePlusOne = rateByTime + 1;
            if (frequency.equalsIgnoreCase("Monthly")) {
                rateByTimeExponential = Math.pow(rateByTimePlusOne, Double.parseDouble(tenor));
            } else if (frequency.equalsIgnoreCase("Weekly")) {
                double n = 0;
                if (Double.parseDouble(tenor) == 3) {
                    n = Double.parseDouble(tenor) * 4D + 1; //3 months * 4 Weeks + 1 Week = 13 Weeks
                } else if (Double.parseDouble(tenor) == 6) {
                    n = Double.parseDouble(tenor) * 4D + 2; //6 months * 4 Weeks + 2 Week = 26 Weeks
                } else if (Double.parseDouble(tenor) == 9) {
                    n = Double.parseDouble(tenor) * 4D + 3; //9 months * 4 Weeks + 3 Week = 39 Weeks
                } else if (Double.parseDouble(tenor) == 12) {
                    n = Double.parseDouble(tenor) * 4D + 4; //12 months * 4 Weeks + 4 Week = 52 Weeks
                }
                rateByTimeExponential = Math.pow(rateByTimePlusOne, n);
            } else {
                double n = 0;
                if (Double.parseDouble(tenor) == 3) {
                    n = Double.parseDouble(tenor) * 30D + 1; //3 months * 30 Days + 1 Day = 91 Days
                } else if (Double.parseDouble(tenor) == 6) {
                    n = Double.parseDouble(tenor) * 30D + 2; //6 months * 30 Days + 2 Days = 182 Days
                } else if (Double.parseDouble(tenor) == 9) {
                    n = Double.parseDouble(tenor) * 30D + 3; //9 months * 30 Days + 3 Days = 273 Days
                } else if (Double.parseDouble(tenor) == 12) {
                    n = Double.parseDouble(tenor) * 30D + 4; //12 months * 30 Days + 4 Days = 364 Days
                }
                rateByTimeExponential = Math.pow(rateByTimePlusOne, n);
            }

            rateByTimeExpoMinusOne = rateByTimeExponential - 1D;
            rateByTimeExpoDivide = rateByTimeExpoMinusOne / rateByTime;
            futureValue = rateByTimeExpoDivide * Double.parseDouble(contributionAmount);
            totalInterest = futureValue - totalContribution;
            dailyInterest = totalInterest / (Double.parseDouble(tenor) * 30);
            response[0] = new BigDecimal(String.valueOf(totalInterest))
                    .setScale(15, RoundingMode.CEILING).doubleValue();
            response[1] = new BigDecimal(String.valueOf(dailyInterest))
                    .setScale(15, RoundingMode.CEILING).doubleValue();
            return response;
        } catch (Exception ex) {
            return response;
        }
    }



    /** TRANSFORMATION SERVICES */
    @Override
    public CustomerDetailsRequestPayload
    createCustomerDetailsRequestPayloadFromTargetSavingsRequest
            (TargetSavingsRequestPayload targetSavingsRequestPayload, String token)
    {
        CustomerDetailsRequestPayload customerDetailsRequestPayload = new CustomerDetailsRequestPayload();
        customerDetailsRequestPayload.setMobileNumber(targetSavingsRequestPayload.getMobileNumber());
        customerDetailsRequestPayload.setRequestId(targetSavingsRequestPayload.getRequestId());
        customerDetailsRequestPayload.setImei(targetSavingsRequestPayload.getImei());
        String hash = this.encryptPayloadToString(customerDetailsRequestPayload, token);
        customerDetailsRequestPayload.setHash(hash);
        return customerDetailsRequestPayload;
    }

    @Override
    public AccountDetailsRequestPayload
    createAccountDetailsRequestPayloadFromTargetSavingsRequest(
            TargetSavingsRequestPayload requestPayload,
            String token
    ){
        AccountDetailsRequestPayload payload = new AccountDetailsRequestPayload();
        payload.setAccountNumber(requestPayload.getAccountNumber());
        payload.setRequestId(requestPayload.getRequestId());
        String hash = this.encryptPayloadToString(payload, token);
        payload.setHash(hash);
        return payload;
    }

    @Override
    public AccountBalanceRequestPayload
    createAccountBalanceRequestPayloadFromTargetSavingsRequest(
            TargetSavingsRequestPayload requestPayload, String token
    )
    {
        AccountBalanceRequestPayload accountBalanceRequestPayload = new AccountBalanceRequestPayload();
        accountBalanceRequestPayload.setAccountNumber(requestPayload.getAccountNumber());
        accountBalanceRequestPayload.setRequestId(requestPayload.getRequestId());
        accountBalanceRequestPayload.setImei(requestPayload.getImei());
        String hash = encryptPayloadToString(accountBalanceRequestPayload, token);
        accountBalanceRequestPayload.setHash(hash);
        return accountBalanceRequestPayload;
    }

    public String encryptFundTransferPayload
            (FundsTransferPayload requestPayload, String token)
    {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber());
        rawString.add(requestPayload.getDebitAccount());
        rawString.add(requestPayload.getCreditAccount().trim());
        rawString.add(requestPayload.getAmount().trim());
        rawString.add(requestPayload.getNarration().trim());
        rawString.add(requestPayload.getTransType().trim());
        rawString.add(requestPayload.getBranchCode().trim());
        rawString.add(requestPayload.getInputter().trim());
        rawString.add(requestPayload.getAuthorizer().trim());
        rawString.add(requestPayload.getNoOfAuthorizer().trim());
        if (requestPayload.getChargeTypes() != null) {
            for (ChargeInfo ch : requestPayload.getChargeTypes()) {
                rawString.add(ch.getChargeType());
                rawString.add(ch.getChargeAmount());
            }
        }

        rawString.add(requestPayload.getRequestId().trim());

        return encryptString(rawString.toString(), token);
    }

    @Override
    public void generateLog(String app, String token, String logMessage, String logType, String logLevel, String requestId) {
        try {
            String requestBy = jwtTokenUtil.getUsernameFromToken(token);
            String remoteIP = jwtTokenUtil.getIPFromToken(token);
            String channel = jwtTokenUtil.getChannelFromToken(token);

            StringBuilder strBuilder = new StringBuilder();
            strBuilder.append(logType.toUpperCase(Locale.ENGLISH));
            strBuilder.append(" - ");
            strBuilder.append("[").append(remoteIP).append(":").append(channel.toUpperCase(Locale.ENGLISH)).append(":").append(requestBy.toUpperCase(Locale.ENGLISH)).append("]");
            strBuilder.append("[").append(app.toUpperCase(Locale.ENGLISH).toUpperCase(Locale.ENGLISH)).append(":").append(requestId.toUpperCase(Locale.ENGLISH)).append("]");
            strBuilder.append("[").append(logMessage).append("]");

            if ("INFO".equalsIgnoreCase(logLevel.trim())) {
                if (logger.isInfoEnabled()) {
                    logger.info(strBuilder.toString());
                }
            }

            if ("DEBUG".equalsIgnoreCase(logLevel.trim())) {
                if (logger.isDebugEnabled()) {
                    logger.error(strBuilder.toString());
                }
            }

        } catch (Exception ex) {
            if (logger.isDebugEnabled()) {
                logger.debug(ex.getMessage());
            }
        }
    }

    @Override
    public LocalDateTime getCurrentDateTime() {
        String response;
        HttpResponse<String> httpResponse;
        Unirest.config().verifySsl(false);
        try{

            log.info("Google Time Url: {}", timeUrl);

            httpResponse = Unirest.get(timeUrl)
                    .header("Content-Type", "application/json")
                    .asString();
            response = httpResponse.getBody();
            CurrentDateTime currentDateTime = gson.fromJson(response, CurrentDateTime.class);
            LocalDateTime dateTime = LocalDateTime.parse(currentDateTime.getDateTime());

            return dateTime;
        }
        catch (UnirestException exception){
            log.error("Error occurred while getting date-time from google. Reason: {}, Cause: {}", exception.getMessage(), exception.getCause().getMessage());
            return LocalDateTime.now();
        }
    }
}
