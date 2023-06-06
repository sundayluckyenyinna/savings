package com.accionmfb.omnix.savings.target_saving.service;

import com.accionmfb.omnix.savings.target_saving.constant.ModelStatus;
import com.accionmfb.omnix.savings.target_saving.constant.ResponseCodes;
import com.accionmfb.omnix.savings.target_saving.model.TransactionSavingSetup;
import com.accionmfb.omnix.savings.target_saving.model.TransactionSavings;
import com.accionmfb.omnix.savings.target_saving.payload.request.FundsTransferPayload;
import com.accionmfb.omnix.savings.target_saving.payload.response.FundsTransferResponsePayload;
import com.accionmfb.omnix.savings.target_saving.repository.TransactionSavingsRepository;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;


@Slf4j
@EnableScheduling
//@Configuration
public class TransactionSavingsCronJob {

    @Value("${transactionSavings.poolAccount}")
    private String transactionSavingsPoolAccount;

    @Autowired
    private TransactionSavingsRepository transactionSavingsRepository;

    @Autowired
    private GenericService genericService;

    @Autowired
    private Gson gson;

    @Autowired
    private ExternalService externalService;



    @Scheduled(fixedDelay = 5 * 60 * 1000, initialDelay = 1000)
    public void runTransactionSavingsJob(){

        String tokenString = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJncnVwcCIsInJvbGVzIjoiW0dSVVBQLCBMT0NBTF9GVU5EU19UUkFOU0ZFUiwgQUlSVElNRV9TRUxGLCBBSVJUSU1FX09USEVSUywgQ0FCTEVfVFZfU1VCU0NSSVBUSU9OLCBFTEVDVFJJQ0lUWV9CSUxMX1BBWU1FTlQsIFNNU19OT1RJRklDQVRJT04sIElOVEVSX0JBTktfRlVORFNfVFJBTlNGRVIsIEFDQ09VTlRfREVUQUlMUywgQUNDT1VOVF9CQUxBTkNFUywgTE9DQUxfRlVORFNfVFJBTlNGRVJfV0lUSF9DSEFSR0UsIEFDQ09VTlRfQkFMQU5DRSwgTklQX05BTUVfRU5RVUlSWV0iLCJhdXRoIjoibWsvdnQ2OVBXMUVVaEpTVUhnZE0rQT09IiwiQ2hhbm5lbCI6IkFHRU5DWSIsIklQIjoiMDowOjA6MDowOjA6MDoxIiwiaXNzIjoiQWNjaW9uIE1pY3JvZmluYW5jZSBCYW5rIiwiaWF0IjoxNjU5MzQ2NDMyLCJleHAiOjYyNTE0Mjg0NDAwfQ.Q6aeZeZtT6IeDNjFa5Sc7gAt0vKLqFjERPy02zS7aTg";

        // Fetch the transaction saving records that are Pending.
        List<TransactionSavings> transactionSavings = transactionSavingsRepository.getPendingTransactionSavings();
        transactionSavings.forEach(ts -> {

            String narration = String.format("Transaction saving for account number: %s, for transaction type: %s", ts.getDebitAccount(), ts.getTransactionType());


            TransactionSavingSetup setup = transactionSavingsRepository.getTransactionSavingSetupByAccountAndType(ts.getDebitAccount(), ts.getTransactionType().toUpperCase());

            if(setup != null && setup.getStatus().equalsIgnoreCase(ModelStatus.ACTIVE.name())){
                double amountToSave = (setup.getSavePercent()/100) * Double.parseDouble(ts.getTransactionAmount());
                String formattedAmountToSave = new BigDecimal(amountToSave).setScale(2, RoundingMode.CEILING).toString();

                // Create the fund transfer payload
                FundsTransferPayload fundsTransferPayload = new FundsTransferPayload();
                fundsTransferPayload.setMobileNumber(ts.getMobileNumber());
                fundsTransferPayload.setAmount(formattedAmountToSave);
                fundsTransferPayload.setCreditAccount(transactionSavingsPoolAccount);
                fundsTransferPayload.setDebitAccount(ts.getDebitAccount());
                fundsTransferPayload.setRequestId(genericService.generateTransRef("TCF"));
                fundsTransferPayload.setTransType("ACTF");
                fundsTransferPayload.setBranchCode("NG0010068"); // Defaulted to the Digital Branch
                fundsTransferPayload.setInputter("");
                fundsTransferPayload.setAuthorizer("");
                fundsTransferPayload.setNoOfAuthorizer("0");
                fundsTransferPayload.setToken(tokenString);
                fundsTransferPayload.setNarration(narration);
                String hash1 = genericService.encryptFundTransferPayload(fundsTransferPayload, tokenString);
                fundsTransferPayload.setHash(hash1);

                String requestJson = gson.toJson(fundsTransferPayload);

                // Call the funds transfer microservice.
                FundsTransferResponsePayload fundsTransferResponsePayload = externalService.getFundsTransferResponse(tokenString, requestJson);
                try{
                    String responseCode = fundsTransferResponsePayload.getResponseCode();
                    if(responseCode.equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())){
                        ts.setStatus(ModelStatus.SUCCESS.name());
                        ts.setSavingT24Ref(fundsTransferResponsePayload.getT24TransRef());
                        ts.setSavingExecutedDate(LocalDateTime.now());
                        ts.setFailureReason(Strings.EMPTY);
                        TransactionSavings updated = transactionSavingsRepository.updateTransactionSavings(ts);

                        log.info("Updated transaction savings: {}", gson.toJson(updated));
                    }
                    else if(responseCode.equalsIgnoreCase(ResponseCodes.INSUFFICIENT_BALANCE.getResponseCode())){
                        ts.setStatus(ModelStatus.MISSED.name());
                        ts.setSavingExecutedDate(LocalDateTime.now());
                        ts.setFailureReason(fundsTransferResponsePayload.getResponseMessage());
                        TransactionSavings updated = transactionSavingsRepository.updateTransactionSavings(ts);

                        log.info("Updated transaction savings: {}", gson.toJson(updated));
                    }
                    else{
                        ts.setStatus(ModelStatus.FAILED.name());
                        ts.setSavingExecutedDate(LocalDateTime.now());
                        ts.setFailureReason(fundsTransferResponsePayload.getResponseMessage());
                        TransactionSavings updated = transactionSavingsRepository.updateTransactionSavings(ts);

                        log.info("Updated transaction savings: {}", gson.toJson(updated));
                    }
                }catch (Exception ex){
                    log.error("Exception while using FT response: {}", ex.getMessage());
                    ex.printStackTrace();
                }
            }

        });
    }


}
