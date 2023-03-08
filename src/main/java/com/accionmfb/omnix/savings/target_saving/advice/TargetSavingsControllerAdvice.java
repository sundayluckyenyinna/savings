package com.accionmfb.omnix.savings.target_saving.advice;

import com.accionmfb.omnix.savings.target_saving.constant.ResponseCodes;
import com.accionmfb.omnix.savings.target_saving.dto.ErrorResponse;
import com.accionmfb.omnix.savings.target_saving.dto.Response;
import com.accionmfb.omnix.savings.target_saving.exception.InvalidTokenException;
import com.accionmfb.omnix.savings.target_saving.exception.UserNotFoundException;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@RestControllerAdvice(basePackages = "com.accionmfb.omnix.savings.target_saving.controller")
public class TargetSavingsControllerAdvice
{
    @Autowired
    private Gson gson;

    @ExceptionHandler(InvalidTokenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public void handleInvalidTokenException(InvalidTokenException exception, HttpServletResponse response)
            throws IOException
    {
        Response errorResponse = ErrorResponse.getInstance();
        errorResponse.setResponseCode(ResponseCodes.INVALID_TOKEN.getResponseCode());
        errorResponse.setResponseMessage(exception.getMessage());

        PrintWriter responseWriter = response.getWriter();
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.FORBIDDEN.value());
        responseWriter.write(gson.toJson(errorResponse));
    }

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public void handleInvalidTokenException(UserNotFoundException exception, HttpServletResponse response)
            throws IOException
    {
        Response errorResponse = ErrorResponse.getInstance();
        errorResponse.setResponseCode(ResponseCodes.INVALID_TOKEN.getResponseCode());
        errorResponse.setResponseMessage(exception.getMessage());

        PrintWriter responseWriter = response.getWriter();
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.BAD_GATEWAY.value());
        responseWriter.write(gson.toJson(errorResponse));
    }
}
