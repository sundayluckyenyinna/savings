package com.accionmfb.omnix.savings.target_saving.exception;

public class InvalidTokenException extends RuntimeException
{
    public InvalidTokenException(){
        super();
    }

    public InvalidTokenException(String message){
        super(message);
    }
}
