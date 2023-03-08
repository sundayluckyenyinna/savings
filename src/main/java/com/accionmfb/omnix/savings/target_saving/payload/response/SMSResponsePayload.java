package com.accionmfb.omnix.savings.target_saving.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SMSResponsePayload
{
    private String responseCode;
    private String responseMessage;
    private String responseDescription;
    private String mobileNumber;
    private String message;
    private String smsFor;

}
