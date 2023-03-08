package com.accionmfb.omnix.savings.target_saving.service;

import com.accionmfb.omnix.savings.target_saving.payload.request.CustomerDetailsRequestPayload;
import com.accionmfb.omnix.savings.target_saving.payload.request.TargetSavingsRequestPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ServiceUtil
{
    @Autowired
    private GenericService genericService;

}
