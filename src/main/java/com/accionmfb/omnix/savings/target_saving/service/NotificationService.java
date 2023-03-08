package com.accionmfb.omnix.savings.target_saving.service;

import com.accionmfb.omnix.savings.target_saving.constant.Constants;
import com.accionmfb.omnix.savings.target_saving.payload.request.SMSRequestPayload;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "omnix-notification", url = "${zuul.routes.notificationService.url}")
public interface NotificationService
{
    @PostMapping(
            value = Constants.SMS_NOTIFICATION,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    String smsNotification(
            @RequestHeader("Authorization") String bearerToken,
            @RequestBody SMSRequestPayload requestPayload
    );
}
