package com.accionmfb.omnix.savings.target_saving.controller;

import com.accionmfb.omnix.savings.target_saving.constant.ApiPaths;
import com.accionmfb.omnix.savings.target_saving.constant.Constants;
import com.accionmfb.omnix.savings.target_saving.dto.ErrorResponse;
import com.accionmfb.omnix.savings.target_saving.dto.PayloadResponse;
import com.accionmfb.omnix.savings.target_saving.dto.Response;
import com.accionmfb.omnix.savings.target_saving.model.TargetSavings;
import com.accionmfb.omnix.savings.target_saving.payload.request.*;
import com.accionmfb.omnix.savings.target_saving.payload.response.TargetSavingsDataResponsePayload;
import com.accionmfb.omnix.savings.target_saving.payload.response.TargetSavingsResponsePayload;
import com.accionmfb.omnix.savings.target_saving.service.GenericService;
import com.accionmfb.omnix.savings.target_saving.service.TargetSavingsService;
import com.accionmfb.omnix.savings.target_saving.validation.TargetSavingsValidator;
import com.google.gson.Gson;
import io.swagger.v3.oas.annotations.Operation;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import java.util.List;

import static com.accionmfb.omnix.savings.target_saving.constant.ApiPaths.*;

@RestController
@RequestMapping
public class TargetSavingsController
{
    @Autowired
    private TargetSavingsService targetSavingsService;

    @Autowired
    private Gson gson;

    @Autowired
    private GenericService genericService;

    @Autowired
    private TargetSavingsValidator validator;

    @PostMapping(
            value = ApiPaths.TARGET_SAVINGS_SET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Set Target Savings")
    public ResponseEntity<Object> setTargetSavings(
            @Valid @RequestBody TargetSavingsRequestPayload requestPayload,
            @RequestHeader(Constants.AUTH_STRING) String authString
    )
    {
        String token = authString.replace(Constants.TOKEN_PREFIX, Strings.EMPTY).trim();

        String hash = genericService.encryptPayloadToString(requestPayload, token);
        requestPayload.setHash(hash);

        // Validate the token.
        validator.validateToken(token, Constants.TARGET_SAVINGS_ROLE);

        // Validate the payload.
        validator.validatePayload(requestPayload, requestPayload.getHash(), token);

        // Call the service
        Response response = targetSavingsService.processSetTargetSavings(token, requestPayload);

        if (response instanceof PayloadResponse)
            return ResponseEntity.ok(((PayloadResponse) response).getResponseData());

        return ResponseEntity.ok(response);

    }

    @PostMapping(
            value = ApiPaths.TARGET_SAVINGS_TERMINATION,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Terminate Target savings")
    public ResponseEntity<Object> terminateTargetSavings(
            @Valid @RequestBody TargetSavingTerminationRequestPayload requestPayload,
            @RequestHeader(Constants.AUTH_STRING) String authString
    )
    {
        String token = authString.replace(Constants.TOKEN_PREFIX, Strings.EMPTY).trim();
        String hash = genericService.encryptPayloadToString(requestPayload, token);
        requestPayload.setHash(hash);

        // Validate the token.
        validator.validateToken(token, Constants.TARGET_SAVINGS_ROLE);

        // Validate the payload.
        validator.validatePayload(requestPayload, requestPayload.getHash(), token);

         // call the service
        Response response = targetSavingsService.processTerminateTargetSavings(token, requestPayload);

        if (response instanceof ErrorResponse)
            return ResponseEntity.ok(response);

        PayloadResponse res = (PayloadResponse)response;
        return ResponseEntity.ok(res.getResponseData());

    }


    @PostMapping(
            value = ApiPaths.TARGET_SAVINGS_MISSED,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Object> processMissedTargetSavings(
            @Valid @RequestBody TargetSavingsMissedRequestPayload requestPayload,
            @RequestHeader(Constants.AUTH_STRING) String authString
    )
    {
        String token = authString.replace(Constants.TOKEN_PREFIX, Strings.EMPTY).trim();

        String hash = genericService.encryptPayloadToString(requestPayload, token);
        requestPayload.setHash(hash);

        // Validate the token.
        validator.validateToken(token, Constants.TARGET_SAVINGS_ROLE);

        // Validate the payload.
        validator.validatePayload(requestPayload, requestPayload.getHash(), token);

        // Call the service
        Response response = targetSavingsService.processMissedTargetSavings(token, requestPayload);

        // If there is an error, send the error message.
        if(response instanceof ErrorResponse)
            return ResponseEntity.ok(response);

        // Now the operation is a success, return the payload.
        TargetSavingsDataResponsePayload dataResponsePayload =
                (TargetSavingsDataResponsePayload) ((PayloadResponse)response).getResponseData();

        return ResponseEntity.ok(dataResponsePayload);

    }

    @PostMapping(
            value = TARGET_SAVINGS_DETAILS,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Object> getTargetSavingsDetails(
           @Valid @RequestBody TargetSavingsDetailsRequestPayload requestPayload,
           @RequestHeader(Constants.AUTH_STRING) String authString
    )
    {
        String token = authString.replace(Constants.TOKEN_PREFIX, Strings.EMPTY).trim();

        String hash = genericService.encryptPayloadToString(requestPayload, token);
        requestPayload.setHash(hash);

        // Validate the token.
        validator.validateToken(token, Constants.TARGET_SAVINGS_ROLE);

        // Validate the payload.
        validator.validatePayload(requestPayload, requestPayload.getHash(), token);

        // Call the service
        Response response = targetSavingsService.processTargetServiceDetails(token, requestPayload);

        if( response instanceof ErrorResponse )
            return ResponseEntity.ok(response);

        TargetSavingsResponsePayload responsePayload =
                (TargetSavingsResponsePayload) ((PayloadResponse)response).getResponseData();

        return ResponseEntity.ok(responsePayload);
    }

    @PostMapping(
        value = TARGET_SAVINGS_LIST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Object> getAllTargetSavingsByAccount(
                    @Valid @RequestBody TargetSavingsAccountPayload requestPayload,
                    @RequestHeader(Constants.AUTH_STRING) String authString,
                    HttpServletRequest request,
                    HttpServletResponse response
    )
    {
        String token = authString.replace(Constants.TOKEN_PREFIX, Strings.EMPTY).trim();

        String hash = genericService.encryptPayloadToString(requestPayload, token);
        requestPayload.setHash(hash);

        // Validate the token.
        validator.validateToken(token, Constants.TARGET_SAVINGS_ROLE);

        // Validate the payload.
        validator.validatePayload(requestPayload, requestPayload.getHash(), token);

        // Call the service
        Response response1 = targetSavingsService
                .getAllTargetSavingsByAccount(token, requestPayload);

        if(response1 instanceof ErrorResponse)
            return ResponseEntity.ok(response1);

        TargetSavingsDataResponsePayload dataResponsePayload = (TargetSavingsDataResponsePayload)
                ((PayloadResponse) response1).getResponseData();

        return ResponseEntity.ok(dataResponsePayload);

    }


    @GetMapping(
            value = TARGET_SAVINGS_ALL,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Object> getAllTargetSavingsSchedule(
            @RequestHeader(Constants.AUTH_STRING) String authString,
            HttpServletRequest request,
            HttpServletResponse response
    )
    {
        String token = authString.replace(Constants.TOKEN_PREFIX, Strings.EMPTY).trim();

        // Validate the token.
        validator.validateToken(token, Constants.TARGET_SAVINGS_ROLE);

        // Call the service
        PayloadResponse payloadResponse = (PayloadResponse) targetSavingsService.getAllTargetSavings(token);

        List<TargetSavings> body = (List<TargetSavings>) payloadResponse.getResponseData();

        return ResponseEntity.ok(body);

    }
}
