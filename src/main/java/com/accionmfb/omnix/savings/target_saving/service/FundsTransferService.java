package com.accionmfb.omnix.savings.target_saving.service;

import com.accionmfb.omnix.savings.target_saving.constant.Constants;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import static com.accionmfb.omnix.savings.target_saving.constant.Constants.*;

@FeignClient(name = "omnix-fundstransfer", url = "${zuul.routes.fundstransferService.url}")
public interface FundsTransferService {

    @PostMapping(value = LOCAL_TRANSFER_WITH_PL_INTERNAL, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<String> localTransfer(@RequestHeader(Constants.AUTH_STRING) String bearerToken, String requestPayload);

    @PostMapping(value = LOCAL_TRANSFER_WITH_CHARGE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<String> localTransferWithCharges(@RequestHeader(AUTH_STRING) String bearerToken, String requestPayload);

    @PostMapping(value = LOCAL_TRANSFER_INTERNAL_DEBIT_WITH_CHARGE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<String> localTransferWithInternalDebitCharges(@RequestHeader(AUTH_STRING) String bearerToken, String requestPayload);

}
