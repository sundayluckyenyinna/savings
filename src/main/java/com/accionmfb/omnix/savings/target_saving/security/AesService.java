
package com.accionmfb.omnix.savings.target_saving.security;
import com.accionmfb.omnix.savings.target_saving.payload.response.GenericPayload;
import com.accionmfb.omnix.savings.target_saving.payload.response.ValidationPayload;

/**
 *
 * @author dofoleta
 */
public interface AesService {
    
    public String encryptString(String textToEncrypt, String encryptionKey);
    public String decryptString(String textToDecrypt, String encryptionKey);
    
     public String encryptFlutterString(String strToEncrypt, String secret) ;
     public String decryptFlutterString(final String textToDecrypt, final String encryptionKey);
     
     public ValidationPayload validateRequest(GenericPayload genericRequestPayload);
}
