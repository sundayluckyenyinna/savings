package com.accionmfb.omnix.savings.target_saving.dto;

import java.io.Serializable;

public class PayloadResponse extends Response implements Serializable
{
    private Object responseData;

    private PayloadResponse(){
        super();
    }

    public static PayloadResponse getInstance(){
        return new PayloadResponse();
    }

    public PayloadResponse(String responseCode, String responseMessage, Object responseData){
        super(responseCode, responseMessage);
        this.responseData = responseData;
    }

    public Object getResponseData() {
        return responseData;
    }

    public void setResponseData(Object responseData) {
        this.responseData = responseData;
    }
}
