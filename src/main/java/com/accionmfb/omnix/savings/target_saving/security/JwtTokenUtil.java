package com.accionmfb.omnix.savings.target_saving.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 *
 * @author dofoleta
 */
@Component(value = "securityJwtToken")
public class JwtTokenUtil implements Serializable {

//    @Autowired
//    Environment env;
    @Autowired
    AesService aesService;

    @Value("${security.omnix.encryption.key}")
    private String omnixEncryptionKey;

    @Value("${security.jwt.signing.key}")
    private String jwtSigningKey;

    ObjectMapper objectMapper;

    JwtTokenUtil() {
        objectMapper = new ObjectMapper();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser().setSigningKey(jwtSigningKey).parseClaimsJws(token).getBody();
        return claims.getSubject();
    }

    public String getChannelFromToken(String token) {
        Claims claims = Jwts.parser().setSigningKey(jwtSigningKey).parseClaimsJws(token).getBody();
        return (String) claims.get("Channel");
    }

    public String getIPFromToken(String token) {
        Claims claims = Jwts.parser().setSigningKey(jwtSigningKey).parseClaimsJws(token).getBody();
        return (String) claims.get("IP");
    }

    public String getEncryptionKeyFromToken(String token) {
        Claims claims = Jwts.parser().setSigningKey(jwtSigningKey).parseClaimsJws(token).getBody();
        String encryptionKey = (String) claims.get("auth");
        return aesService.decryptString(encryptionKey, omnixEncryptionKey);
    }

    public boolean userHasRole(String token, String role) {
        Claims claims = Jwts.parser().setSigningKey(jwtSigningKey).parseClaimsJws(token).getBody();
        String roles = (String) claims.get("roles").toString();
        return roles.contains(role);
    }

}
