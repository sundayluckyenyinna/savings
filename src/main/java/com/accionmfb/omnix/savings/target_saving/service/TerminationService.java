package com.accionmfb.omnix.savings.target_saving.service;

import com.accionmfb.omnix.savings.target_saving.constant.ApiPaths;
import com.accionmfb.omnix.savings.target_saving.constant.Constants;
import com.accionmfb.omnix.savings.target_saving.payload.request.TargetSavingTerminationRequestPayload;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "omnix-savings", url = "${zuul.routes.savingsService.url}")
public interface TerminationService
{
    @PostMapping(
            value = ApiPaths.TARGET_SAVINGS_TERMINATION,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<String> terminateTargetSavings(
                    @RequestHeader(Constants.AUTH_STRING) String bearerToken,
                    @RequestBody TargetSavingTerminationRequestPayload requestPayload
    );

}
