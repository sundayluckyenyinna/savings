package com.accionmfb.omnix.savings.target_saving.security;
import com.accionmfb.omnix.savings.target_saving.constant.ResponseCodes;
import com.accionmfb.omnix.savings.target_saving.payload.response.ExceptionResponse;
import com.accionmfb.omnix.savings.target_saving.payload.response.GenericPayload;
import com.accionmfb.omnix.savings.target_saving.payload.response.ValidationPayload;
import com.didisoft.pgp.*;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dofoleta
 */
@Service
public class PgpServiceImpl implements PgpService {

    @Value("${security.pgp.encryption.privateKey}")
    private String myPrivateKeyFile;
    @Value("${security.pgp.encryption.password}")
    private String myPrivateKeyPassword;
    @Value("${security.pgp.encryption.publicKey}")
    private String recipientPublicKeyFile;

    @Autowired
    MessageSource messageSource;

    @Autowired
    Gson gson;

    @Override
    public boolean generateKeyPairRSA(String userId, String privateKeyPassword, String publicKeyFileName, String privateKeyFileName) {
        boolean armor = false;
        boolean isGenerated = false;
        if (publicKeyFileName.equals("")) {
            return isGenerated;
        }
        if (privateKeyFileName.equals("")) {
            return isGenerated;
        }
        try {
            String keyAlgorithm = KeyAlgorithm.RSA;

            // preferred hashing algorithms
            String[] hashingAlgorithms = new String[]{HashAlgorithm.SHA1, HashAlgorithm.SHA256, HashAlgorithm.SHA384,
                HashAlgorithm.SHA512, HashAlgorithm.MD5};

            // preferred compression algorithms
            String[] compressions = new String[]{CompressionAlgorithm.ZIP, CompressionAlgorithm.ZLIB, CompressionAlgorithm.UNCOMPRESSED};

            // preferred symmetric key algorithms
            String[] cyphers = new String[]{CypherAlgorithm.CAST5, CypherAlgorithm.AES_128, CypherAlgorithm.AES_192,
                CypherAlgorithm.AES_256, CypherAlgorithm.TWOFISH};

            int keySizeInBytes = 2048;

            // expiration date, pass 0 for no expiration
            long expiresAfterDays = 0;

            PGPKeyPair keypair = PGPKeyPair.generateKeyPair(keySizeInBytes, userId, keyAlgorithm, privateKeyPassword,
                    compressions, hashingAlgorithms, cyphers, expiresAfterDays);

            keypair.exportPrivateKey(privateKeyFileName, armor);
            keypair.exportPublicKey(publicKeyFileName, armor);
            isGenerated = true;

        } catch (IOException | PGPException ex) {
            Logger.getLogger(PgpServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return isGenerated;
    }

    @Override
    public String encryptString(String stringToEncrypt, String recipientPublicKeyFile) {
        // create an instance of the library
        PGPLib pgp = new PGPLib();

        String encryptedString = "";

        InputStream publicEncryptionKeyStream = null;
        try {

            publicEncryptionKeyStream = new FileInputStream(recipientPublicKeyFile);
            // encrypt
            encryptedString = pgp.encryptString(stringToEncrypt, publicEncryptionKeyStream);
            if (encryptedString != null) {
                String temp = encryptedString, messageToDecrypt="";
                String[] items = temp.split("Java 3.2\r\n\r\n");
                if (items.length == 2) {
                    items = items[1].split("\r\n-----END");
                    messageToDecrypt = items[0];
                    encryptedString=messageToDecrypt;
                }
            }

        } catch (IOException | PGPException ex) {
            Logger.getLogger(PgpServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (publicEncryptionKeyStream != null) {
                try {
                    publicEncryptionKeyStream.close();
                } catch (IOException iOException) {
                }
            }
        }

        return encryptedString;
    }

    @Override
    public String decryptString(String stringToDecrypt, String myPrivateKeyFile, String myPrivateKeyPassword) {

        try {
            // create an instance of the library
            PGPLib pgp = new PGPLib();

            String decryptedString = pgp.decryptString(stringToDecrypt, myPrivateKeyFile, myPrivateKeyPassword);

            return decryptedString;
        } catch (IOException | PGPException ex) {
            Logger.getLogger(PgpServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public ValidationPayload validateRequest(GenericPayload genericRequestPayload) {

//        LoggingUtil.debugInfo("Validating Encrypted Request: " + gson.toJson(genericRequestPayload), this.getClass(), LogMode.INFO.name());
        String encryptedRequest = genericRequestPayload.getRequest();
        String decryptedRequest;
//        String errorMessage;

        ValidationPayload validatorPayload = new ValidationPayload();
        try {
            decryptedRequest = decryptString(encryptedRequest, myPrivateKeyFile, myPrivateKeyPassword);
            if (decryptedRequest == null) {
                validatorPayload.setError(true);

                ExceptionResponse exResponse = new ExceptionResponse();
                exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
                exResponse.setResponseMessage(messageSource.getMessage("appMessages.encryption", new Object[0], Locale.ENGLISH));
                String exceptionJson = gson.toJson(exResponse);

                GenericPayload responsePayload = new GenericPayload();
                responsePayload.setResponse(encryptString(exceptionJson, recipientPublicKeyFile));

                validatorPayload.setResponse(gson.toJson(responsePayload));
//                LoggingUtil.debugInfo("Decryption Error: " + message, this.getClass(), LogMode.DEBUG.name());
            } else {
                validatorPayload.setError(false);
                validatorPayload.setResponse("SUCCESS");
                validatorPayload.setPlainTextPayload(decryptedRequest);
//                LoggingUtil.debugInfo("Decryption Success: " + decryptedRequest, this.getClass(), LogMode.INFO.name());
            }
        } catch (NoSuchMessageException ex) {
            validatorPayload.setError(true);
            String errorMessage = ex.getMessage();
            ExceptionResponse exResponse = new ExceptionResponse();
            exResponse.setResponseCode(ResponseCodes.FORMAT_EXCEPTION.getResponseCode());
            exResponse.setResponseMessage(errorMessage);
            String exceptionJson = gson.toJson(exResponse);

            GenericPayload responsePayload = new GenericPayload();
            responsePayload.setResponse(encryptString(exceptionJson, recipientPublicKeyFile));

//            LoggingUtil.exceptionInfo(ex, this.getClass(), LogMode.DEBUG.name());
            return validatorPayload;
        }
        return validatorPayload;
    }

//    public static void main(String[] args) {
//        String userId = "com.accionmfb.omnix.mobile-proxy";
//        String password = "accionmfb-password";
//        String publicFilePath = "C:\\Users\\seyinna\\Documents\\NetBeansProjects\\MobileProxy\\src\\main\\java\\com\\accionmfb\\omnix\\mobile_proxy\\security\\public.asc";
//        String privateFilePath = "C:\\Users\\seyinna\\Documents\\NetBeansProjects\\MobileProxy\\src\\main\\java\\com\\accionmfb\\omnix\\mobile_proxy\\security\\private.asc";
//        EncryptionServiceImpl encryptionService = new EncryptionServiceImpl();
//        encryptionService.generateKeyPairRSA(userId, password, publicFilePath, privateFilePath);
//    }
}
