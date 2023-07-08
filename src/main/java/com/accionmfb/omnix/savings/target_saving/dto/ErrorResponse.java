package com.accionmfb.omnix.savings.target_saving.dto;


import java.io.Serializable;


public class ErrorResponse extends Response implements Serializable
{
    private ErrorResponse(){
        super();
    }

    public static ErrorResponse getInstance(){
        return new ErrorResponse();
    }

    public ErrorResponse(String responseCode, String responseMessage){
        super(responseCode, responseMessage);
    }

    public String toString() {
        return "Response{" +
                "responseCode='" + responseCode + '\'' +
                ", responseMessage='" + responseMessage + '\'' +
                '}';
    }
}
