package com.accionmfb.omnix.savings.target_saving.validation;

import com.accionmfb.omnix.savings.target_saving.exception.InvalidTokenException;
import com.accionmfb.omnix.savings.target_saving.exception.UserNotFoundException;
import com.accionmfb.omnix.savings.target_saving.jwt.JwtTokenUtil;
import com.accionmfb.omnix.savings.target_saving.model.AppUser;
import com.accionmfb.omnix.savings.target_saving.repository.GenericRepository;
import com.accionmfb.omnix.savings.target_saving.service.GenericService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.nio.file.attribute.UserPrincipalNotFoundException;


/**
 * This class encapsulates the validation process of the incoming client requests after
 * the first stage of authentication from the API gateway.
 * The second stage validation of the client request is done automatically in this class
 * and communicates closely to the controller. The two important validation process here
 * are:
 * 1) Token validation and authentication by roles and channels.
 * 2) Payload validation by 'hash' value comparison.
 */
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

    public void validateToken(String token, String role)
    {
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

    public void validatePayload(Object payload, String hash, String token){
        String encryptionKey = jwtTokenUtil.getEncryptionKeyFromToken(token);
        String rawString = genericService.getJoinedPayloadValues(payload);
        String decryptedString = genericService.decryptString(hash, encryptionKey);
        if( !rawString.equalsIgnoreCase(decryptedString) ){
            String message = genericService.getMessageOfRequest("hash.failed");
            throw new RuntimeException(message);
        }
    }

}
