package com.accionmfb.omnix.savings.target_saving.service;

import com.accionmfb.omnix.savings.target_saving.constant.ApiPaths;
import com.accionmfb.omnix.savings.target_saving.constant.Constants;
import com.accionmfb.omnix.savings.target_saving.payload.request.AccountBalanceRequestPayload;
import com.accionmfb.omnix.savings.target_saving.payload.request.AccountDetailsRequestPayload;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Service
@FeignClient(name = "omnix-account", url = "${zuul.routes.accountService.url}")
public interface AccountService
{
    @PostMapping(
            value = ApiPaths.ACCOUNT_DETAILS,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<String> accountDetails(
            @RequestHeader(Constants.AUTH_STRING) String bearerToken,
            @RequestBody AccountDetailsRequestPayload accountNumberPayload
    );



    @PostMapping(
            value = ApiPaths.ACCOUNT_BALANCE,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<String> accountBalance(
            @RequestHeader(Constants.AUTH_STRING) String bearerToken,
            @RequestBody AccountBalanceRequestPayload accountBalanceRequestPayload
    );

}
