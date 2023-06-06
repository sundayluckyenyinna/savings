package com.accionmfb.omnix.savings.target_saving.validation;

import com.accionmfb.omnix.savings.target_saving.constant.ResponseCodes;
import com.accionmfb.omnix.savings.target_saving.exception.InvalidTokenException;
import com.accionmfb.omnix.savings.target_saving.exception.UserNotFoundException;
import com.accionmfb.omnix.savings.target_saving.jwt.JwtTokenUtil;
import com.accionmfb.omnix.savings.target_saving.model.AppUser;
import com.accionmfb.omnix.savings.target_saving.payload.response.ExceptionResponse;
import com.accionmfb.omnix.savings.target_saving.payload.response.ValidationPayload;
import com.accionmfb.omnix.savings.target_saving.repository.GenericRepository;
import com.accionmfb.omnix.savings.target_saving.service.GenericService;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import javax.validation.constraints.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * This class encapsulates the validation process of the incoming client requests after
 * the first stage of authentication from the API gateway.
 * The second stage validation of the client request is done automatically in this class
 * and communicates closely to the controller. The two important validation process here
 * are:
 * 1) Token validation and authentication by roles and channels.
 * 2) Payload validation by 'hash' value comparison.
 */

@Slf4j
@Component
public class TargetSavingsValidator
{
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private GenericService genericService;

    @Autowired
    private GenericRepository genericRepository;

    private static final Gson gson = new Gson();
    private static final Class<?>[] JAVAX_VALIDATION_CLASS_ANNOTATION = new Class[]{
            AssertFalse.class, AssertTrue.class, Pattern.class,
            NotEmpty.class, NotBlank.class, NotNull.class, Null.class
    };

    // Validate the token of the application user.
    public void validateToken(String token, String role) {
        String message;
        if(!jwtTokenUtil.isJwtValid(token)){
             message = genericService.getMessageOfRequest("invalidtoken");
            throw new InvalidTokenException(message);
        }
        if(!jwtTokenUtil.userHasRole(token, role)) {
            message = genericService.getMessageOfRequest("norole");
            throw new UserNotFoundException(message);
        }

        String username = jwtTokenUtil.getUsernameFromToken(token);
        AppUser appUser = genericRepository.getAppUserUsingUsername(username);
        if(appUser == null){
            message = genericService.getMessageOfRequest("norole");
            throw new UserNotFoundException(message);
        }
    }


    // Validate the hashing algorithm of the application user.
    @Deprecated
    public void validatePayload(Object payload, String hash, String token){
        String encryptionKey = jwtTokenUtil.getEncryptionKeyFromToken(token);
        String rawString = genericService.getJoinedPayloadValues(payload);
        String decryptedString = genericService.decryptString(hash, encryptionKey);
        if( !rawString.equalsIgnoreCase(decryptedString) ){
            String message = genericService.getMessageOfRequest("hash.failed");
            throw new RuntimeException(message);
        }
    }


    /**
     * This method performs validation of the passed in model using the javax.validation.constraints
     *  package. It is better used compared to the verbose response provided by the spring framework.
     * @param payload: Object
     */
    public ValidationPayload doModelValidation(Object payload){
        ValidationPayload validationPayload = new ValidationPayload();
        validationPayload.setError(false);
        validationPayload.setResponse(Strings.EMPTY);
        validationPayload.setPlainTextPayload(Strings.EMPTY);

        List<Field> fields = List.of(payload.getClass().getDeclaredFields());
        List<String> errorMessageList = new ArrayList<>();

        fields.forEach(field -> {
            field.setAccessible(true);
            Object fieldValue = null;
            try {
                fieldValue = field.get(payload);
            } catch (IllegalAccessException e) {
                log.error("Error occurred while trying to get field value for model validation. Reason: {}", e.getMessage());
                throw new RuntimeException(e);
            }

            List<Annotation> annotations = List.of(field.getDeclaredAnnotations());
            for (Annotation annotation : annotations) {
                Class<?> annotationClass = annotation.annotationType();
                if (annotationClass.isAssignableFrom(AssertFalse.class)) {
                    if(fieldValue instanceof Boolean && (Boolean) fieldValue == Boolean.TRUE) {
                        errorMessageList.add(field.getAnnotation(AssertFalse.class).message());
                    }
                }

                else if (annotationClass.isAssignableFrom(AssertTrue.class)) {
                    if(fieldValue instanceof Boolean && (Boolean) fieldValue == Boolean.FALSE) {
                        errorMessageList.add(field.getAnnotation(AssertTrue.class).message());
                    }
                }

                else if (annotationClass.isAssignableFrom(Pattern.class)){
                    Pattern pattern = field.getAnnotation(Pattern.class);
                    String regex = pattern.regexp();
                    if(fieldValue == null || !java.util.regex.Pattern.matches(regex, String.valueOf(fieldValue))){
                        errorMessageList.add(field.getAnnotation(Pattern.class).message());
                    }
                }

                else if (annotationClass.isAssignableFrom(NotNull.class)){
                    if (fieldValue == null){
                        errorMessageList.add(field.getAnnotation(NotNull.class).message());
                    }
                }

                else if (annotationClass.isAssignableFrom(Null.class)){
                    if(fieldValue != null){
                        errorMessageList.add(field.getAnnotation(Null.class).message());
                    }
                }

                else if (annotationClass.isAssignableFrom(NotEmpty.class)){
                    if(fieldValue == null || String.valueOf(fieldValue).isEmpty()) {
                        errorMessageList.add(field.getAnnotation(NotEmpty.class).message());
                    }
                }

                else if (annotationClass.isAssignableFrom(NotBlank.class)) {
                    if(fieldValue == null || String.valueOf(fieldValue).isBlank()) {
                        errorMessageList.add(field.getAnnotation(NotBlank.class).message());
                    }
                }

            };
        });

        if(!errorMessageList.isEmpty()){
            String completeErrorMessage = String.join(", ", errorMessageList);
            ExceptionResponse exceptionResponse = new ExceptionResponse();
            exceptionResponse.setResponseCode(ResponseCodes.FAILED_MODEL.getResponseCode());
            exceptionResponse.setResponseMessage(completeErrorMessage);

            validationPayload.setError(true);
            validationPayload.setResponse(gson.toJson(exceptionResponse));
            validationPayload.setPlainTextPayload(gson.toJson(exceptionResponse));
            return validationPayload;
        }

        return validationPayload;
    }

}
