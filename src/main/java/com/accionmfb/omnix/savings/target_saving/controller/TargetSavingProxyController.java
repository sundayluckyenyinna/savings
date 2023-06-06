package com.accionmfb.omnix.savings.target_saving.controller;


import com.accionmfb.omnix.savings.target_saving.constant.ApiPaths;
import com.accionmfb.omnix.savings.target_saving.constant.ResponseCodes;
import com.accionmfb.omnix.savings.target_saving.constant.SecurityOption;
import com.accionmfb.omnix.savings.target_saving.dto.ErrorResponse;
import com.accionmfb.omnix.savings.target_saving.dto.PayloadResponse;
import com.accionmfb.omnix.savings.target_saving.dto.Response;
import com.accionmfb.omnix.savings.target_saving.payload.request.*;
import com.accionmfb.omnix.savings.target_saving.payload.response.ExceptionResponse;
import com.accionmfb.omnix.savings.target_saving.payload.response.GenericPayload;
import com.accionmfb.omnix.savings.target_saving.payload.response.ValidationPayload;
import com.accionmfb.omnix.savings.target_saving.security.AesService;
import com.accionmfb.omnix.savings.target_saving.security.JwtTokenUtil;
import com.accionmfb.omnix.savings.target_saving.security.LogService;
import com.accionmfb.omnix.savings.target_saving.security.PgpService;
import com.accionmfb.omnix.savings.target_saving.service.TargetSavingsService;
import com.accionmfb.omnix.savings.target_saving.service.TransactionSavingsService;
import com.accionmfb.omnix.savings.target_saving.validation.TargetSavingsValidator;
import com.google.gson.Gson;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Locale;

import static com.accionmfb.omnix.savings.target_saving.constant.ApiPaths.HEADER_STRING;
import static com.accionmfb.omnix.savings.target_saving.constant.ApiPaths.TOKEN_PREFIX;

@RestController
@RequestMapping(value = ApiPaths.PROXY_CONTROLLER_BASE_URL)
@Api(tags = "Target Savings Proxy Service")
public class TargetSavingProxyController
{
    @Autowired
    private TargetSavingsService targetSavingsService;

    @Autowired
    private TransactionSavingsService transactionSavingsService;
    @Autowired
    MessageSource messageSource;

    @Autowired
    LogService logService;

    @Autowired
    Gson gson;

    @Autowired
    JwtTokenUtil jwtToken;

    @Autowired
    PgpService pgpService;

    @Autowired
    AesService aesService;

    @Value("${security.pgp.encryption.publicKey}")
    private String recipientPublicKeyFile;

    @Value("${security.aes.encryption.key}")
    private String aesEncryptionKey;

    @Value("${security.option:AES}")
    private String securityOption;

    @Autowired
    private TargetSavingsValidator validator;



    @Operation(summary = "Set target savings")
    @PostMapping(value = ApiPaths.TARGET_SAVINGS_SET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> handleNewTargetSavingsRequest(@RequestBody GenericPayload requestPayload, HttpServletRequest httpRequest) {

        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "").trim();

        //Check if the user has role
        ValidationPayload oValidatorPayload = validateChannelAndRequest("ACCOUNT_STATEMENT", requestPayload, token);
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
            TargetSavingsRequestPayload tRequest = gson.fromJson(oValidatorPayload.getPlainTextPayload(), TargetSavingsRequestPayload.class);
            ValidationPayload validationPayload = validator.doModelValidation(tRequest);
            if(validationPayload.isError()){
                if(securityOption.equalsIgnoreCase(SecurityOption.AES.name()))
                    return ResponseEntity.ok(aesService.encryptFlutterString(validationPayload.getPlainTextPayload(), aesEncryptionKey));
                return ResponseEntity.ok(pgpService.encryptString(validationPayload.getResponse(), recipientPublicKeyFile));
            }
            String response;
            Response res = targetSavingsService.processSetTargetSavings(token, tRequest);
            if(res instanceof PayloadResponse)
                response = gson.toJson(((PayloadResponse)res).getResponseData());
            else
                response = gson.toJson((ErrorResponse)res);
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
        }
    }


    @Operation(summary = "Handle missed target savings")
    @PostMapping(value = ApiPaths.TARGET_SAVINGS_MISSED, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> handleMissedTargetSavingsRequest(@Valid @RequestBody GenericPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "").trim();

        //Check if the user has role
        ValidationPayload oValidatorPayload = validateChannelAndRequest("ACCOUNT_STATEMENT", requestPayload, token);
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
            TargetSavingsMissedRequestPayload tRequest = gson.fromJson(oValidatorPayload.getPlainTextPayload(), TargetSavingsMissedRequestPayload.class);
            ValidationPayload validationPayload = validator.doModelValidation(tRequest);
            if(validationPayload.isError()){
                if(securityOption.equalsIgnoreCase(SecurityOption.AES.toString()))
                    return ResponseEntity.ok(aesService.encryptFlutterString(validationPayload.getPlainTextPayload(), aesEncryptionKey));
                return ResponseEntity.ok(pgpService.encryptString(validationPayload.getResponse(), recipientPublicKeyFile));
            }
            String response;
            Response res = targetSavingsService.processMissedTargetSavings(token, tRequest);
            if(res instanceof PayloadResponse)
                response = gson.toJson(((PayloadResponse)res).getResponseData());
            else
                response = gson.toJson((ErrorResponse)res);
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
        }
    }


    @Operation(summary = "Terminate target saving goal")
    @PostMapping(value = ApiPaths.TARGET_SAVINGS_TERMINATION, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> handleTargetSavingTerminationRequest(@Valid @RequestBody GenericPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "").trim();

        //Check if the user has role
        ValidationPayload oValidatorPayload = validateChannelAndRequest("ACCOUNT_STATEMENT", requestPayload, token);
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
            TargetSavingTerminationRequestPayload tRequest = gson.fromJson(oValidatorPayload.getPlainTextPayload(), TargetSavingTerminationRequestPayload.class);
            ValidationPayload validationPayload = validator.doModelValidation(tRequest);
            if(validationPayload.isError()){
                if(securityOption.equalsIgnoreCase(SecurityOption.AES.toString()))
                    return ResponseEntity.ok(aesService.encryptFlutterString(validationPayload.getPlainTextPayload(), aesEncryptionKey));
                return ResponseEntity.ok(pgpService.encryptString(validationPayload.getResponse(), recipientPublicKeyFile));
            }
            String response;
            Response res = targetSavingsService.processTerminateTargetSavings(token, tRequest);
            if(res instanceof PayloadResponse)
                response = gson.toJson(((PayloadResponse)res).getResponseData());
            else
                response = gson.toJson((ErrorResponse)res);
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
        }
    }


    @Operation(summary = "Get details of target savings")
    @PostMapping(value = ApiPaths.TARGET_SAVINGS_DETAILS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> handleTargetSavingDetailsRequest(@Valid @RequestBody GenericPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "").trim();

        //Check if the user has role
        ValidationPayload oValidatorPayload = validateChannelAndRequest("ACCOUNT_STATEMENT", requestPayload, token);
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
            TargetSavingsDetailsRequestPayload tRequest = gson.fromJson(oValidatorPayload.getPlainTextPayload(), TargetSavingsDetailsRequestPayload.class);
            ValidationPayload validationPayload = validator.doModelValidation(tRequest);
            if(validationPayload.isError()){
                if(securityOption.equalsIgnoreCase(SecurityOption.AES.toString()))
                    return ResponseEntity.ok(aesService.encryptFlutterString(validationPayload.getPlainTextPayload(), aesEncryptionKey));
                return ResponseEntity.ok(pgpService.encryptString(validationPayload.getResponse(), recipientPublicKeyFile));
            }
            String response;
            Response res = targetSavingsService.processTargetServiceDetails(token, tRequest);
            if(res instanceof PayloadResponse)
                response = gson.toJson(((PayloadResponse)res).getResponseData());
            else
                response = gson.toJson((ErrorResponse)res);
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
        }
    }


    @Operation(summary = "List all target saving associated with an account")
    @PostMapping(value = ApiPaths.TARGET_SAVINGS_LIST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> handleTargetSavingListByAccountRequest(@Valid @RequestBody GenericPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "").trim();

        //Check if the user has role
        ValidationPayload oValidatorPayload = validateChannelAndRequest("ACCOUNT_STATEMENT", requestPayload, token);
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
            TargetSavingsAccountPayload tRequest = gson.fromJson(oValidatorPayload.getPlainTextPayload(), TargetSavingsAccountPayload.class);
            ValidationPayload validationPayload = validator.doModelValidation(tRequest);
            if(validationPayload.isError()){
                if(securityOption.equalsIgnoreCase(SecurityOption.AES.toString()))
                    return ResponseEntity.ok(aesService.encryptFlutterString(validationPayload.getPlainTextPayload(), aesEncryptionKey));
                return ResponseEntity.ok(pgpService.encryptString(validationPayload.getResponse(), recipientPublicKeyFile));
            }
            String response;
            Response res = targetSavingsService.getAllTargetSavingsByAccount(token, tRequest);
            if(res instanceof PayloadResponse)
                response = gson.toJson(((PayloadResponse)res).getResponseData());
            else
                response = gson.toJson((ErrorResponse)res);
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
        }
    }


    // TRANSACTION SAVING
    @Operation(summary = "Setup a transaction saving goal for a particular account number")
    @PostMapping(value = ApiPaths.TRANSACTION_SAVING_SETUP, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> handleTransactionSavingSetup(@Valid @RequestBody GenericPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "").trim();

        //Check if the user has role
        ValidationPayload oValidatorPayload = validateChannelAndRequest("ACCOUNT_STATEMENT", requestPayload, token);
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
            TransactionSavingSetupRequestPayload tRequest = gson.fromJson(oValidatorPayload.getPlainTextPayload(), TransactionSavingSetupRequestPayload.class);
            ValidationPayload validationPayload = validator.doModelValidation(tRequest);
            if(validationPayload.isError()){
                if(securityOption.equalsIgnoreCase(SecurityOption.AES.toString()))
                    return ResponseEntity.ok(aesService.encryptFlutterString(validationPayload.getPlainTextPayload(), aesEncryptionKey));
                return ResponseEntity.ok(pgpService.encryptString(validationPayload.getResponse(), recipientPublicKeyFile));
            }
            String response;
            Response res = transactionSavingsService.processTransactionSavingSetupSaveOrUpdate(tRequest, token);
            if(res instanceof PayloadResponse)
                response = gson.toJson(((PayloadResponse)res).getResponseData());
            else
                response = gson.toJson((ErrorResponse)res);
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
        }
    }

    @Operation(summary = "Terminate a transaction saving goal for a particular transaction type. If the transaction type is not specified, all transaction saving goal will be terminated.")
    @DeleteMapping(value = ApiPaths.TRANSACTION_SAVING_SETUP_TERMINATE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> handleTransactionSavingSetupTermination(@RequestParam("accountNumber") String accountNumber, @RequestParam(value = "transactionType", required = false)String transactionType, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "").trim();

        //Check if the user has role
        ValidationPayload oValidatorPayload = validateChannelAndRequest("ACCOUNT_STATEMENT", null, token);
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
            String response;
            Response res = transactionSavingsService.processTerminateTransactionSavingSetup(accountNumber, transactionType);
            if(res instanceof PayloadResponse)
                response = gson.toJson(((PayloadResponse)res).getResponseData());
            else
                response = gson.toJson((ErrorResponse)res);
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
        }
    }

    @Operation(summary = "Get a list of all the transaction saving goal for a particular transaction type. If the transaction type is not specified, all transaction saving goal will be returned.")
    @GetMapping(value = ApiPaths.TRANSACTION_SAVING_SETUP_LIST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> handleTransactionSavingSetupList(@RequestParam("accountNumber") String accountNumber, @RequestParam(value = "transactionType", required = false)String transactionType, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "").trim();

        //Check if the user has role
        ValidationPayload oValidatorPayload = validateChannelAndRequest("ACCOUNT_STATEMENT", null, token);
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
            String response;
            Response res = transactionSavingsService.processGetTransactionSavingSetup(accountNumber, transactionType);
            if(res instanceof PayloadResponse)
                response = gson.toJson(((PayloadResponse)res).getResponseData());
            else
                response = gson.toJson((ErrorResponse)res);
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
        }
    }

    private ValidationPayload validateChannelAndRequest(String role, GenericPayload requestPayload, String token) {
        ExceptionResponse exResponse = new ExceptionResponse();

        // Validate the role of the application user.
        boolean userHasRole = jwtToken.userHasRole(token, role);
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));
            String exceptionJson = gson.toJson(exResponse);
            logService.logInfo("Target/Transaction savings validation", token, messageSource.getMessage("appMessages.user.hasnorole", new Object[]{0}, Locale.ENGLISH), "API Response", exceptionJson);
            ValidationPayload validatorPayload = new ValidationPayload();
            validatorPayload.setError(true);
            if (securityOption.equalsIgnoreCase("AES")) {
                validatorPayload.setResponse(aesService.encryptFlutterString(exceptionJson, aesEncryptionKey));
            } else {
                validatorPayload.setResponse(pgpService.encryptString(exceptionJson, recipientPublicKeyFile));
            }
            return validatorPayload;
        }

        // Validate the encryption validity of the request.
        if(requestPayload != null) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return aesService.validateRequest(requestPayload);
            }
            return pgpService.validateRequest(requestPayload);
        }

        ValidationPayload validationPayload = new ValidationPayload();
        validationPayload.setError(false);
        validationPayload.setPlainTextPayload(Strings.EMPTY);
        validationPayload.setResponse(Strings.EMPTY);
        return validationPayload;
    }

}
