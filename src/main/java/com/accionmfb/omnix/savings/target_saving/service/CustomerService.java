package com.accionmfb.omnix.savings.target_saving.service;

import com.accionmfb.omnix.savings.target_saving.constant.ApiPaths;
import com.accionmfb.omnix.savings.target_saving.constant.Constants;
import com.accionmfb.omnix.savings.target_saving.payload.request.CustomerDetailsRequestPayload;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Locale;

@FeignClient(name = "omnix-customer", url = "${zuul.routes.customerService.url}")
public interface CustomerService
{
    @PostMapping(
            value = ApiPaths.CUSTOMER_DETAILS,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<String> customerDetails(
            @RequestHeader(Constants.AUTH_STRING) String bearerToken,
            @RequestBody CustomerDetailsRequestPayload customerDetailsRequestPayload
    );

}
