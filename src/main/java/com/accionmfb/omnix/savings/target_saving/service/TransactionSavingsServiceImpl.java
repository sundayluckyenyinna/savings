package com.accionmfb.omnix.savings.target_saving.service;

import com.accionmfb.omnix.savings.target_saving.constant.ModelStatus;
import com.accionmfb.omnix.savings.target_saving.constant.ResponseCodes;
import com.accionmfb.omnix.savings.target_saving.constant.TransactionType;
import com.accionmfb.omnix.savings.target_saving.dto.ErrorResponse;
import com.accionmfb.omnix.savings.target_saving.dto.PayloadResponse;
import com.accionmfb.omnix.savings.target_saving.dto.Response;
import com.accionmfb.omnix.savings.target_saving.jwt.JwtTokenUtil;
import com.accionmfb.omnix.savings.target_saving.model.TransactionSavingSetup;
import com.accionmfb.omnix.savings.target_saving.payload.request.TransactionSavingSetupRequestPayload;
import com.accionmfb.omnix.savings.target_saving.payload.response.TransactionSavingSetupListResponsePayload;
import com.accionmfb.omnix.savings.target_saving.payload.response.TransactionSavingsSetupResponsePayload;
import com.accionmfb.omnix.savings.target_saving.repository.TransactionSavingsRepository;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.accionmfb.omnix.savings.target_saving.constant.TransactionType.*;

@Slf4j
@Service
public class TransactionSavingsServiceImpl implements TransactionSavingsService
{

    @Autowired
    private TransactionSavingsRepository transactionSavingsRepository;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private Gson gson;


    @Override
    public Response processTransactionSavingSetupSaveOrUpdate(TransactionSavingSetupRequestPayload requestPayload, String token) {
        String accountNumber = requestPayload.getAccountNumber();
        String transactionType = requestPayload.getTransactionType();
        String channel = jwtTokenUtil.getChannelFromToken(token);

        String action;
        TransactionSavingSetup setup = transactionSavingsRepository.getTransactionSavingSetupByAccountAndType(accountNumber, transactionType);
        if(setup == null){
            setup = new TransactionSavingSetup();
            setup.setCreatedAt(LocalDateTime.now().toString());
            setup.setCreatedBy("USER");
            action = "S";
        }else{
            action = "U";
        }

        setup.setSavePercent(Double.parseDouble(requestPayload.getSavePercent()));
        setup.setUpdatedAt(LocalDateTime.now().toString());
        setup.setTransactionType(transactionType);
        setup.setUpdatedBy("USER");
        setup.setAccountNumber(accountNumber);
        setup.setStatus(ModelStatus.ACTIVE.name());
        setup.setRequestChannel(channel);
        setup.setRequestId(requestPayload.getRequestId());

        if(action.equalsIgnoreCase("S")) {
            TransactionSavingSetup saved = transactionSavingsRepository.saveTransactionSavingSetup(setup);
            log.info("Transaction setup saved: {}", gson.toJson(saved));
        }else{
            TransactionSavingSetup updated = transactionSavingsRepository.updatedTransactionSavingSetup(setup);
            log.info("Transaction setup updated: {}", gson.toJson(updated));
        }

        // Return success response to client.
        TransactionSavingsSetupResponsePayload responsePayload = new TransactionSavingsSetupResponsePayload();
        responsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
        responsePayload.setResponseMessage("success");
        responsePayload.setData(setup);
        PayloadResponse payloadResponse = PayloadResponse.getInstance();
        payloadResponse.setResponseCode(responsePayload.getResponseCode());
        payloadResponse.setResponseMessage(responsePayload.getResponseMessage());
        payloadResponse.setResponseData(responsePayload);
        return payloadResponse;
    }

    @Override
    public Response processGetTransactionSavingSetup(String accountNumber, String transactionTypes) {
        List<String> validTransType = Arrays.asList(FT_TRANSFER.name(), AIRTIME_PURCHASE.name(), DATA_PURCHASE.name(), BILL_PAYMENT.name());

        List<TransactionSavingSetup> transactionSavingSetupList = transactionSavingsRepository.getTransactionSavingSetupByAccount(accountNumber);
        if(transactionTypes != null && !transactionTypes.isBlank() && !transactionTypes.isEmpty()){
            List<String> transTypes = Arrays.stream(transactionTypes.trim().split(" "))
                    .filter(s -> !s.isEmpty() && !s.isBlank())
                    .collect(Collectors.toList());

            for(String s : transTypes){
                if(!validTransType.contains(s)){
                    ErrorResponse errorResponse = ErrorResponse.getInstance();
                    errorResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                    errorResponse.setResponseMessage("Bad request due to invalid transaction types passed in request.");
                    return errorResponse;
                }
            }

            transactionSavingSetupList = transactionSavingSetupList.stream()
                    .filter(ts -> transTypes.contains(ts.getTransactionType()))
                    .collect(Collectors.toList());
        }

        TransactionSavingSetupListResponsePayload responsePayload = new TransactionSavingSetupListResponsePayload();
        responsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
        responsePayload.setResponseMessage("Success");
        responsePayload.setData(transactionSavingSetupList);

        PayloadResponse payloadResponse = PayloadResponse.getInstance();
        payloadResponse.setResponseCode(responsePayload.getResponseCode());
        payloadResponse.setResponseMessage(responsePayload.getResponseMessage());
        payloadResponse.setResponseData(responsePayload);
        return payloadResponse;
    }

    @Override
    public Response processTerminateTransactionSavingSetup(String accountNumber, String transactionTypes) {
        List<String> validTransType = Arrays.asList(FT_TRANSFER.name(), AIRTIME_PURCHASE.name(), DATA_PURCHASE.name(), BILL_PAYMENT.name());

        List<TransactionSavingSetup> transactionSavingSetupList = transactionSavingsRepository.getTransactionSavingSetupByAccount(accountNumber);
        if(transactionTypes != null && !transactionTypes.isBlank() && !transactionTypes.isEmpty()){
          List<String> transTypes = Arrays.stream(transactionTypes.trim().split(" "))
                  .filter(s -> !s.isEmpty() && !s.isBlank())
                  .collect(Collectors.toList());

          for(String s : transTypes){
              if(!validTransType.contains(s)){
                  ErrorResponse errorResponse = ErrorResponse.getInstance();
                  errorResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                  errorResponse.setResponseMessage("Bad request due to invalid transaction types passed in request.");
                  return errorResponse;
              }
          }

          transactionSavingSetupList = transactionSavingSetupList.stream()
                  .filter(ts -> transTypes.contains(ts.getTransactionType()))
                  .collect(Collectors.toList());
        }

        transactionSavingSetupList.forEach(setup ->{
            setup.setStatus(ModelStatus.TERMINATED.name());
            setup.setUpdatedAt(LocalDateTime.now().toString());
            setup.setUpdatedBy("USER");

            TransactionSavingSetup updated = transactionSavingsRepository.updatedTransactionSavingSetup(setup);
            log.info("Transaction setup updated: {}", gson.toJson(updated));
        });

        TransactionSavingSetupListResponsePayload responsePayload = new TransactionSavingSetupListResponsePayload();
        responsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
        responsePayload.setResponseMessage("Success");
        responsePayload.setData(transactionSavingSetupList);

        PayloadResponse payloadResponse = PayloadResponse.getInstance();
        payloadResponse.setResponseCode(responsePayload.getResponseCode());
        payloadResponse.setResponseMessage(responsePayload.getResponseMessage());
        payloadResponse.setResponseData(responsePayload);
        return payloadResponse;
    }
}
