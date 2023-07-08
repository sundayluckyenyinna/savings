package com.accionmfb.omnix.savings.target_saving.dto;

import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;

@Getter
@Setter
public abstract class Response
{
    protected String responseCode;
    protected String responseMessage;

    public Response(){
        this.responseCode = Strings.EMPTY;
        this.responseMessage = Strings.EMPTY;
    }

    public Response(String responseCode, String responseMessage){
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
    }

    @Override
    public String toString() {
        return "Response{" +
                "responseCode='" + responseCode + '\'' +
                ", responseMessage='" + responseMessage + '\'' +
                '}';
    }
}
