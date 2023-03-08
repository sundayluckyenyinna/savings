package com.accionmfb.omnix.savings.target_saving.service;

import com.accionmfb.omnix.savings.target_saving.dto.Response;
import com.accionmfb.omnix.savings.target_saving.payload.request.*;
import org.springframework.stereotype.Service;

@Service
public interface TargetSavingsService
{
    Response
    processSetTargetSavings(String token, TargetSavingsRequestPayload requestPayload);

    Response
    processTerminateTargetSavings(String token, TargetSavingTerminationRequestPayload requestPayload);

    Response
    processMissedTargetSavings(String token, TargetSavingsMissedRequestPayload requestPayload);

    Response
    processTargetServiceDetails(String token, TargetSavingsDetailsRequestPayload requestPayload);


    Response getAllTargetSavings(String token);

    Response getAllTargetSavingsByAccount(String token, TargetSavingsAccountPayload accountPayload);
}
