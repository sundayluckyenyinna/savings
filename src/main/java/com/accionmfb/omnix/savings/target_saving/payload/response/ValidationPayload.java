package com.accionmfb.omnix.savings.target_saving.payload.response;

import lombok.Data;

@Data
public class ValidationPayload
{
    private boolean error;
    private String response;
    private String plainTextPayload;
}
