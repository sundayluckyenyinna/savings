/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.savings.target_saving.jwt;

import com.accionmfb.omnix.savings.target_saving.service.GenericService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.io.Serializable;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * @author Enyinna
 */
@Component
public class JwtTokenUtil implements Serializable {

    @Autowired
    GenericService genericService;
    @Value("${omnix.encryption.key}")
    private String omnixEncryptionKey;
    @Autowired
    Environment env;
    @Value("${jwt.security.key}")
    private String jwtKey;

    ObjectMapper objectMapper;

    JwtTokenUtil() {
        objectMapper = new ObjectMapper();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser().setSigningKey(jwtKey).parseClaimsJws(token).getBody();
        return claims.getSubject();
    }

    public String getChannelFromToken(String token) {
        Claims claims = Jwts.parser().setSigningKey(jwtKey).parseClaimsJws(token).getBody();
        return (String) claims.get("Channel");
    }

    public String getUserCredentialFromToken(String token) {
        String channel = getChannelFromToken(token);
        return env.getProperty("omnix.channel.user." + channel.toLowerCase(Locale.ENGLISH));
    }

    public String getIPFromToken(String token) {
        Claims claims = Jwts.parser().setSigningKey(jwtKey).parseClaimsJws(token).getBody();
        return (String) claims.get("IP");
    }

    public String getEncryptionKeyFromToken(String token) {
        Claims claims = Jwts.parser().setSigningKey(jwtKey).parseClaimsJws(token).getBody();
        String encryptionKey = (String) claims.get("auth");
        return genericService.decryptString(encryptionKey, omnixEncryptionKey);
    }

    public boolean isJwtValid(String token){
        return Jwts.parser().setSigningKey(jwtKey).parseClaimsJws(token).getBody() != null;
    }

    public boolean userHasRole(String token, String role) {
        Claims claims = Jwts.parser().setSigningKey(jwtKey).parseClaimsJws(token).getBody();
        String roles = (String) claims.get("roles").toString();
        return roles.contains(role);
    }
}
