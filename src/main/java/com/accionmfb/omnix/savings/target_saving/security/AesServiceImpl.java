/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.accionmfb.omnix.savings.target_saving.security;

import com.accionmfb.omnix.savings.target_saving.constant.ResponseCodes;
import com.accionmfb.omnix.savings.target_saving.payload.response.ExceptionResponse;
import com.accionmfb.omnix.savings.target_saving.payload.response.GenericPayload;
import com.accionmfb.omnix.savings.target_saving.payload.response.ValidationPayload;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dofoleta
 */
@Service
public class AesServiceImpl implements AesService {
    
    @Autowired
    MessageSource messageSource;
    
    @Autowired
    Gson gson;
    
    @Value("${security.aes.encryption.key}")
    private String aesEncryptionKey;

    @Override
    public String encryptString(String textToEncrypt, String encryptionKey) {
//        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        try {

            byte[] key = encryptionKey.trim().getBytes("UTF-8");
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");

            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            System.out.println(Base64.getEncoder().encodeToString(cipher.doFinal(textToEncrypt.trim().getBytes("UTF-8"))));

            return Base64.getEncoder().encodeToString(cipher.doFinal(textToEncrypt.trim().getBytes("UTF-8")));

        } catch (UnsupportedEncodingException | InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException ex) {
            return ex.getMessage();
        }
    }

    @Override
    public String decryptString(String textToDecrypt, String encryptionKey) {
        try {

            byte[] key = encryptionKey.trim().getBytes("UTF-8");
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");

            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            String decryptedResponse = new String(cipher.doFinal(Base64.getDecoder().decode(textToDecrypt.trim())));
            String[] splitString = decryptedResponse.split(":");
            StringJoiner rawString = new StringJoiner(":");
            for (String str : splitString) {
                rawString.add(str.trim());
            }
            return rawString.toString();
        } catch (UnsupportedEncodingException | InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException ex) {
            return ex.getMessage();
        }
    }

    @Override
    public String encryptFlutterString(final String strToEncrypt, final String secret) {
        try {
            byte[] key = secret.getBytes("UTF-8");
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");

            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder()
                    .encodeToString(cipher.doFinal(strToEncrypt.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            System.out.println("Error while encrypting: " + e.toString());
        } catch (InvalidKeyException ex) {
            Logger.getLogger(AesServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
     public String decryptFlutterString(final String textToDecrypt, final String secret){
        try {
            byte[] key = secret.getBytes("UTF-8");
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(textToDecrypt)));
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            System.out.println("Error while decrypting: " + e.toString());
        } catch (UnsupportedEncodingException | IllegalBlockSizeException | BadPaddingException ex) {
            Logger.getLogger(AesServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public ValidationPayload validateRequest(GenericPayload genericRequestPayload) {

//        LoggingUtil.debugInfo("Validating Encrypted Request: " + gson.toJson(genericRequestPayload), this.getClass(), LogMode.INFO.name());
        String encryptedRequest = genericRequestPayload.getRequest();
        String decryptedRequest;
//        String errorMessage;

        ValidationPayload validatorPayload = new ValidationPayload();
        try {
            decryptedRequest = decryptFlutterString(encryptedRequest, aesEncryptionKey);
            if (decryptedRequest == null) {
                validatorPayload.setError(true);

                ExceptionResponse exResponse = new ExceptionResponse();
                exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
                exResponse.setResponseMessage(messageSource.getMessage("appMessages.encryption", new Object[0], Locale.ENGLISH));
                String exceptionJson = gson.toJson(exResponse);

                GenericPayload responsePayload = new GenericPayload();
                responsePayload.setResponse(encryptFlutterString(exceptionJson, aesEncryptionKey));

                validatorPayload.setResponse(gson.toJson(responsePayload));
            } else {
                validatorPayload.setError(false);
                validatorPayload.setResponse("SUCCESS");
                validatorPayload.setPlainTextPayload(decryptedRequest);
            }
        } catch (NoSuchMessageException ex) {
            validatorPayload.setError(true);
            String errorMessage = ex.getMessage();
            ExceptionResponse exResponse = new ExceptionResponse();
            exResponse.setResponseCode(ResponseCodes.FORMAT_EXCEPTION.getResponseCode());
            exResponse.setResponseMessage(errorMessage);
            String exceptionJson = gson.toJson(exResponse);

            GenericPayload responsePayload = new GenericPayload();
            responsePayload.setResponse(encryptFlutterString(exceptionJson, aesEncryptionKey));

//            LoggingUtil.exceptionInfo(ex, this.getClass(), LogMode.DEBUG.name());
            return validatorPayload;
        }
        return validatorPayload;
    }
}
