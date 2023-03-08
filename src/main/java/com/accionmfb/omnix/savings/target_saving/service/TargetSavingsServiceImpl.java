package com.accionmfb.omnix.savings.target_saving.service;

import com.accionmfb.omnix.savings.target_saving.dto.Response;
import com.accionmfb.omnix.savings.target_saving.payload.request.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TargetSavingsServiceImpl implements TargetSavingsService
{
    @Autowired
    private TargetSavingsSetService targetSavingsSetService;

    @Autowired
    private TargetSavingsTerminateService targetSavingsTerminateService;

    @Autowired
    private TargetSavingMissedService targetSavingMissedService;

    @Autowired
    private TargetSavingsDetailsService targetSavingsDetailsService;

    @Autowired
    private TargetSavingsListService targetSavingsListService;

    @Autowired
    private TargetSavingsAccountService targetSavingsAccountService;


    @Override
    public Response
    processSetTargetSavings(String token, TargetSavingsRequestPayload requestPayload) {
        return targetSavingsSetService.processSetTargetSavings(token, requestPayload);
    }

    @Override
    public Response
    processTerminateTargetSavings(String token, TargetSavingTerminationRequestPayload requestPayload){
        return targetSavingsTerminateService.processTargetSavingsTermination(requestPayload, token);
    }

    @Override
    public Response
    processMissedTargetSavings(String token, TargetSavingsMissedRequestPayload requestPayload){
        return targetSavingMissedService.processTargetSavingsCatchup(requestPayload, token);
    }

    @Override
    public Response
    processTargetServiceDetails(String token, TargetSavingsDetailsRequestPayload requestPayload)
    {
        return targetSavingsDetailsService.processTargetSavingsDetails(requestPayload, token);
    }

    @Override
    public Response getAllTargetSavings(String token){
        return targetSavingsListService.getAllTargetSavings(token);
    }

    @Override
    public Response getAllTargetSavingsByAccount(String token, TargetSavingsAccountPayload accountPayload){
        return targetSavingsAccountService.getAllTargetSavingsByAccountNumber(accountPayload, token);
    }
}
