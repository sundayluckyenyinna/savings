package com.accionmfb.omnix.savings.target_saving.controller;


import com.accionmfb.omnix.savings.target_saving.constant.ApiPaths;
import com.accionmfb.omnix.savings.target_saving.constant.ResponseCodes;
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
import com.google.gson.Gson;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Locale;

import static com.accionmfb.omnix.savings.target_saving.constant.ApiPaths.HEADER_STRING;
import static com.accionmfb.omnix.savings.target_saving.constant.ApiPaths.TOKEN_PREFIX;

@RestController
@RequestMapping(value = ApiPaths.PROXY_CONTROLLER_BASE_URL)
public class TargetSavingProxyController
{
    @Autowired
    private TargetSavingsService targetSavingsService;

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


    /* Internal validation system for checking the validity of the user role by channel and the request format */
    private ValidationPayload validateChannelAndRequest(String role, GenericPayload requestPayload, String token) {
        ExceptionResponse exResponse = new ExceptionResponse();
        boolean userHasRole = jwtToken.userHasRole(token, role);
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));
            String exceptionJson = gson.toJson(exResponse);
            logService.logInfo("Create Individual CustomerW ith Bvn", token, messageSource.getMessage("appMessages.user.hasnorole", new Object[]{0}, Locale.ENGLISH), "API Response", exceptionJson);
            ValidationPayload validatorPayload = new ValidationPayload();
            if (securityOption.equalsIgnoreCase("AES")) {
                validatorPayload.setResponse(aesService.encryptFlutterString(exceptionJson, aesEncryptionKey));
            } else {
                validatorPayload.setResponse(pgpService.encryptString(exceptionJson, recipientPublicKeyFile));
            }
        }
        if (securityOption.equalsIgnoreCase("AES")) {
            return aesService.validateRequest(requestPayload);
        }
        return pgpService.validateRequest(requestPayload);
    }


    /* Request handler for the setting of a new target saving goal */
    @Operation(summary = "Set target savings")
    @PostMapping(value = ApiPaths.TARGET_SAVINGS_SET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> handleNewTargetSavingsRequest(@Valid @RequestBody GenericPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "").trim();

        //Check if the user has role
        ValidationPayload oValidatorPayload = validateChannelAndRequest("MINI_ACCOUNT_STATEMENT", requestPayload, token);
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
            TargetSavingsRequestPayload tRequest = gson.fromJson(oValidatorPayload.getPlainTextPayload(), TargetSavingsRequestPayload.class);
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


    /* Request handler to handle missed target savings */
    @Operation(summary = "Handle missed target savings")
    @PostMapping(value = ApiPaths.TARGET_SAVINGS_MISSED, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> handleMissedTargetSavingsRequest(@Valid @RequestBody GenericPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "").trim();

        //Check if the user has role
        ValidationPayload oValidatorPayload = validateChannelAndRequest("MINI_ACCOUNT_STATEMENT", requestPayload, token);
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
            TargetSavingsMissedRequestPayload tRequest = gson.fromJson(oValidatorPayload.getPlainTextPayload(), TargetSavingsMissedRequestPayload.class);
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


    /* Request handler to handle the termination of a target saving goal */
    @Operation(summary = "Terminate target saving goal")
    @PostMapping(value = ApiPaths.TARGET_SAVINGS_TERMINATION, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> handleTargetSavingTerminationRequest(@Valid @RequestBody GenericPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "").trim();

        //Check if the user has role
        ValidationPayload oValidatorPayload = validateChannelAndRequest("MINI_ACCOUNT_STATEMENT", requestPayload, token);
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
            TargetSavingTerminationRequestPayload tRequest = gson.fromJson(oValidatorPayload.getPlainTextPayload(), TargetSavingTerminationRequestPayload.class);
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


    /* Request handler to fetch the details of a target saving goal */
    @Operation(summary = "Get details of target savings")
    @PostMapping(value = ApiPaths.TARGET_SAVINGS_DETAILS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> handleTargetSavingDetailsRequest(@Valid @RequestBody GenericPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "").trim();

        //Check if the user has role
        ValidationPayload oValidatorPayload = validateChannelAndRequest("MINI_ACCOUNT_STATEMENT", requestPayload, token);
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
            TargetSavingsDetailsRequestPayload tRequest = gson.fromJson(oValidatorPayload.getPlainTextPayload(), TargetSavingsDetailsRequestPayload.class);
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


    /* Request handler to fetch the list of all target saving goal associated with an account number. */
    @Operation(summary = "List all target saving associated with an account")
    @PostMapping(value = ApiPaths.TARGET_SAVINGS_LIST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> handleTargetSavingListByAccountRequest(@Valid @RequestBody GenericPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "").trim();

        //Check if the user has role
        ValidationPayload oValidatorPayload = validateChannelAndRequest("MINI_ACCOUNT_STATEMENT", requestPayload, token);
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
            TargetSavingsAccountPayload tRequest = gson.fromJson(oValidatorPayload.getPlainTextPayload(), TargetSavingsAccountPayload.class);
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

}
