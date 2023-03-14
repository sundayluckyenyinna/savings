package com.accionmfb.omnix.savings.target_saving.security;
import com.accionmfb.omnix.savings.target_saving.payload.response.GenericPayload;
import com.accionmfb.omnix.savings.target_saving.payload.response.ValidationPayload;


/**
 *
 * @author dofoleta
 */
public interface  PgpService {
    public boolean generateKeyPairRSA(String userId, String privateKeyPassword, String publicKeyFileName, String privateKeyFileName);
    public String encryptString(String stringToEncrypt, String recipientPublicKeyFile);
    public String decryptString(String stringToDecrypt, String myPrivateKeyFile, String myPrivateKeyPassword);
    
    public ValidationPayload validateRequest(GenericPayload genericRequestPayload);
}
