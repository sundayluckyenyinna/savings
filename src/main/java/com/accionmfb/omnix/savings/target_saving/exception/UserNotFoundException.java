package com.accionmfb.omnix.savings.target_saving.exception;


public class UserNotFoundException extends RuntimeException
{
    public UserNotFoundException(){
        super();
    }

    public UserNotFoundException(String message){
        super(message);
    }


}
