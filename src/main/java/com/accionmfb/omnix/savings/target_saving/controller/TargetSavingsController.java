package com.accionmfb.omnix.savings.target_saving.controller;

import com.accionmfb.omnix.savings.target_saving.constant.ApiPaths;
import com.accionmfb.omnix.savings.target_saving.constant.Constants;
import com.accionmfb.omnix.savings.target_saving.dto.ErrorResponse;
import com.accionmfb.omnix.savings.target_saving.dto.PayloadResponse;
import com.accionmfb.omnix.savings.target_saving.dto.Response;
import com.accionmfb.omnix.savings.target_saving.payload.request.*;
import com.accionmfb.omnix.savings.target_saving.payload.response.*;
import com.accionmfb.omnix.savings.target_saving.service.GenericService;
import com.accionmfb.omnix.savings.target_saving.service.TargetSavingsService;
import com.accionmfb.omnix.savings.target_saving.service.TransactionSavingsService;
import com.accionmfb.omnix.savings.target_saving.validation.TargetSavingsValidator;
import com.google.gson.Gson;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import static com.accionmfb.omnix.savings.target_saving.constant.ApiPaths.*;

@RestController
@RequestMapping
@Api(tags = "Target Savings Service")
public class TargetSavingsController {
    @Autowired
    private TargetSavingsService targetSavingsService;

    @Autowired
    private TransactionSavingsService transactionSavingsService;

    @Autowired
    private Gson gson;

    @Autowired
    private GenericService genericService;

    @Autowired
    private TargetSavingsValidator validator;



    @PostMapping(value = ApiPaths.TARGET_SAVINGS_SET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Set Target Savings")
    public ResponseEntity<TargetSavingsResponsePayload> handleSetTargetSavings(@RequestBody TargetSavingsRequestPayload requestPayload, @RequestHeader(Constants.AUTH_STRING) String authString) {

        String token = authString.replace(Constants.TOKEN_PREFIX, Strings.EMPTY).trim();
        String hash = genericService.encryptPayloadToString(requestPayload, token);
        requestPayload.setHash(hash);

        // Validate the token.
        // validator.validateToken(token, Constants.TARGET_SAVINGS_ROLE);

        ValidationPayload validationPayload = validator.doModelValidation(requestPayload);
        if(validationPayload.isError()){
            String error = validationPayload.getPlainTextPayload();
            TargetSavingsResponsePayload errorResponse = gson.fromJson(error, TargetSavingsResponsePayload.class);
            return ResponseEntity.ok(errorResponse);
        }

        // Call the service
        Response response = targetSavingsService.processSetTargetSavings(token, requestPayload);
        if (response instanceof PayloadResponse) {
            TargetSavingsResponsePayload errorResponse = (TargetSavingsResponsePayload) ((PayloadResponse) response).getResponseData();
            return ResponseEntity.ok(errorResponse);
        }
        return ResponseEntity.ok(gson.fromJson(gson.toJson(response), TargetSavingsResponsePayload.class));
    }


    @PostMapping(value = ApiPaths.TARGET_SAVINGS_TERMINATION, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Terminate Target savings")
    public ResponseEntity<TargetSavingsResponsePayload> handleTerminateTargetSavings(@RequestBody TargetSavingTerminationRequestPayload requestPayload, @RequestHeader(Constants.AUTH_STRING) String authString) {

        String token = authString.replace(Constants.TOKEN_PREFIX, Strings.EMPTY).trim();
        String hash = genericService.encryptPayloadToString(requestPayload, token);
        requestPayload.setHash(hash);

        // Validate the token.
        //validator.validateToken(token, Constants.TARGET_SAVINGS_ROLE);

        ValidationPayload validationPayload = validator.doModelValidation(requestPayload);
        if(validationPayload.isError()){
            String error = validationPayload.getPlainTextPayload();
            TargetSavingsResponsePayload errorResponse = gson.fromJson(error, TargetSavingsResponsePayload.class);
            return ResponseEntity.ok(errorResponse);
        }

        // call the service
        Response response = targetSavingsService.processTerminateTargetSavings(token, requestPayload);
        if (response instanceof ErrorResponse) {
            return ResponseEntity.ok(gson.fromJson(gson.toJson(response), TargetSavingsResponsePayload.class));
        }

        PayloadResponse res = (PayloadResponse)response;
        return ResponseEntity.ok((TargetSavingsResponsePayload) res.getResponseData());
    }


    @PostMapping(value = ApiPaths.TARGET_SAVINGS_MISSED, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Process missed target savings")
    public ResponseEntity<TargetSavingsDataResponsePayload> handleMissedTargetSavings(@RequestBody TargetSavingsMissedRequestPayload requestPayload, @RequestHeader(Constants.AUTH_STRING) String authString) {

        String token = authString.replace(Constants.TOKEN_PREFIX, Strings.EMPTY).trim();
        String hash = genericService.encryptPayloadToString(requestPayload, token);
        requestPayload.setHash(hash);

        // Validate the token.
//        validator.validateToken(token, Constants.TARGET_SAVINGS_ROLE);

        ValidationPayload validationPayload = validator.doModelValidation(requestPayload);
        if(validationPayload.isError()){
            String error = validationPayload.getPlainTextPayload();
            TargetSavingsDataResponsePayload errorResponse = gson.fromJson(error, TargetSavingsDataResponsePayload.class);
            return ResponseEntity.ok(errorResponse);
        }

        // Call the service
        Response response = targetSavingsService.processMissedTargetSavings(token, requestPayload);
        if(response instanceof ErrorResponse) {
            return ResponseEntity.ok(gson.fromJson(gson.toJson(response), TargetSavingsDataResponsePayload.class));
        }

        TargetSavingsDataResponsePayload dataResponsePayload = (TargetSavingsDataResponsePayload) ((PayloadResponse)response).getResponseData();
        return ResponseEntity.ok(dataResponsePayload);
    }


    @PostMapping(value = TARGET_SAVINGS_DETAILS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get details of a particular saving goal for a particular account number")
    public ResponseEntity<TargetSavingsResponsePayload> handleTargetSavingsDetails(@RequestBody TargetSavingsDetailsRequestPayload requestPayload, @RequestHeader(Constants.AUTH_STRING) String authString) {

        String token = authString.replace(Constants.TOKEN_PREFIX, Strings.EMPTY).trim();
        String hash = genericService.encryptPayloadToString(requestPayload, token);
        requestPayload.setHash(hash);

        // Validate the token.
        //validator.validateToken(token, Constants.TARGET_SAVINGS_ROLE);

        ValidationPayload validationPayload = validator.doModelValidation(requestPayload);
        if(validationPayload.isError()){
            String error = validationPayload.getPlainTextPayload();
            TargetSavingsResponsePayload errorResponse = gson.fromJson(error, TargetSavingsResponsePayload.class);
            return ResponseEntity.ok(errorResponse);
        }

        // Call the service
        Response response = targetSavingsService.processTargetServiceDetails(token, requestPayload);
        if( response instanceof ErrorResponse ) {
            return ResponseEntity.ok(gson.fromJson(gson.toJson(response), TargetSavingsResponsePayload.class));
        }

        TargetSavingsResponsePayload responsePayload = (TargetSavingsResponsePayload) ((PayloadResponse)response).getResponseData();
        return ResponseEntity.ok(responsePayload);
    }


    @PostMapping(value = TARGET_SAVINGS_LIST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get the list of saving goal for a particular account")
    public ResponseEntity<TargetSavingsDataResponsePayload> getAllTargetSavingsByAccount(@RequestBody TargetSavingsAccountPayload requestPayload, @RequestHeader(Constants.AUTH_STRING) String authString, HttpServletRequest request) {

        String token = authString.replace(Constants.TOKEN_PREFIX, Strings.EMPTY).trim();
        String hash = genericService.encryptPayloadToString(requestPayload, token);
        requestPayload.setHash(hash);

        // Validate the token.
        // validator.validateToken(token, Constants.TARGET_SAVINGS_ROLE);

        ValidationPayload validationPayload = validator.doModelValidation(requestPayload);
        if(validationPayload.isError()){
            String error = validationPayload.getPlainTextPayload();
            TargetSavingsDataResponsePayload errorResponse = gson.fromJson(error, TargetSavingsDataResponsePayload.class);
            return ResponseEntity.ok(errorResponse);
        }

        // Call the service
        Response response = targetSavingsService.getAllTargetSavingsByAccount(token, requestPayload);
        if(response instanceof ErrorResponse) {
            return ResponseEntity.ok(gson.fromJson(gson.toJson(response), TargetSavingsDataResponsePayload.class));
        }
        TargetSavingsDataResponsePayload dataResponsePayload = (TargetSavingsDataResponsePayload) ((PayloadResponse) response).getResponseData();
        return ResponseEntity.ok(dataResponsePayload);
    }


    // Transaction Savings
    @PostMapping(value = TRANSACTION_SAVING_SETUP, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Setup or update a transaction saving goal")
    public ResponseEntity<TransactionSavingsSetupResponsePayload> handleTransactionSavingSetup(@RequestBody TransactionSavingSetupRequestPayload requestPayload, @RequestHeader(Constants.AUTH_STRING) String authString, HttpServletRequest request) {

        String token = authString.replace(Constants.TOKEN_PREFIX, Strings.EMPTY).trim();
        String hash = genericService.encryptPayloadToString(requestPayload, token);

        // Validate the token.
        // validator.validateToken(token, Constants.TARGET_SAVINGS_ROLE);

        ValidationPayload validationPayload = validator.doModelValidation(requestPayload);
        if(validationPayload.isError()){
            String error = validationPayload.getPlainTextPayload();
            TransactionSavingsSetupResponsePayload errorResponse = gson.fromJson(error, TransactionSavingsSetupResponsePayload.class);
            return ResponseEntity.ok(errorResponse);
        }

        // Call the service
        Response response = transactionSavingsService.processTransactionSavingSetupSaveOrUpdate(requestPayload, token);
        if(response instanceof ErrorResponse) {
            return ResponseEntity.ok(gson.fromJson(gson.toJson(response), TransactionSavingsSetupResponsePayload.class));
        }
        TransactionSavingsSetupResponsePayload dataResponsePayload = (TransactionSavingsSetupResponsePayload) ((PayloadResponse) response).getResponseData();
        return ResponseEntity.ok(dataResponsePayload);
    }


    @DeleteMapping(value = TRANSACTION_SAVING_SETUP_TERMINATE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Terminate a particular transaction saving goal. The transaction types for which the transaction saving must be terminated should be passed as a request parameter. If not transaction type is specified, all the transaction saving goal associated with the account will be terminated.")
    public ResponseEntity<TransactionSavingSetupListResponsePayload> handleTransactionSavingTermination(@RequestParam("accountNumber") String accountNumber, @RequestParam(value = "transactionType", required = false) String transactionType, @RequestHeader(Constants.AUTH_STRING) String authString, HttpServletRequest request) {

        String token = authString.replace(Constants.TOKEN_PREFIX, Strings.EMPTY).trim();

        // Validate the token.
        // validator.validateToken(token, Constants.TARGET_SAVINGS_ROLE);

        // Call the service
        Response response = transactionSavingsService.processTerminateTransactionSavingSetup(accountNumber, transactionType);
        if(response instanceof ErrorResponse) {
            return ResponseEntity.ok(gson.fromJson(gson.toJson(response), TransactionSavingSetupListResponsePayload.class));
        }
        TransactionSavingSetupListResponsePayload dataResponsePayload = (TransactionSavingSetupListResponsePayload) ((PayloadResponse) response).getResponseData();
        return ResponseEntity.ok(dataResponsePayload);
    }


    @GetMapping(value = TRANSACTION_SAVING_SETUP_LIST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get details of a transaction saving setup list for a particular account. The list can be filtered by passing an optional transaction type. If no transaction type is specified, the system returns all transaction saving goal associated with the account.")
    public ResponseEntity<TransactionSavingSetupListResponsePayload> handleTransactionSavingList(@RequestParam("accountNumber") String accountNumber, @RequestParam(value = "transactionType", required = false) String transactionType, @RequestHeader(Constants.AUTH_STRING) String authString, HttpServletRequest request) {

        String token = authString.replace(Constants.TOKEN_PREFIX, Strings.EMPTY).trim();

        // Validate the token.
        // validator.validateToken(token, Constants.TARGET_SAVINGS_ROLE);

        // Call the service
        Response response = transactionSavingsService.processGetTransactionSavingSetup(accountNumber, transactionType);
        if(response instanceof ErrorResponse) {
            System.out.println(gson.toJson(response));
            return ResponseEntity.ok(gson.fromJson(gson.toJson(response), TransactionSavingSetupListResponsePayload.class));
        }
        TransactionSavingSetupListResponsePayload dataResponsePayload = (TransactionSavingSetupListResponsePayload) ((PayloadResponse) response).getResponseData();
        return ResponseEntity.ok(dataResponsePayload);
    }
}
