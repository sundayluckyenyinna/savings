package com.accionmfb.omnix.savings.target_saving.service;

import com.accionmfb.omnix.savings.target_saving.constant.ResponseCodes;
import com.accionmfb.omnix.savings.target_saving.dto.PayloadResponse;
import com.accionmfb.omnix.savings.target_saving.dto.Response;
import com.accionmfb.omnix.savings.target_saving.jwt.JwtTokenUtil;
import com.accionmfb.omnix.savings.target_saving.model.TargetSavings;
import com.accionmfb.omnix.savings.target_saving.repository.TargetSavingsRepository;
import com.accionmfb.omnix.savings.target_saving.util.TargetSavingsUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class TargetSavingsListService
{

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private GenericService genericService;

    @Autowired
    private TargetSavingsRepository targetSavingsRepository;


    public Response getAllTargetSavings(String token)
    {
        String username = jwtTokenUtil.getUsernameFromToken(token);
        String channel = jwtTokenUtil.getChannelFromToken(token);
        String internalRequestId = TargetSavingsUtils.generateRequestId();

        // Log the request
        genericService.generateLog("Target Savings List", token, username +" : " + channel, "API Request", "DEBUG", internalRequestId);

        // Find all the target savings from the repository
        List<TargetSavings> targetSavingsList = targetSavingsRepository.findAllTargetSavings();

        PayloadResponse response = PayloadResponse.getInstance();
        response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
        response.setResponseData(targetSavingsList);

        return response;
    }
}
