package com.stackroute.userauthenticationservice.service;
import com.stackroute.userauthenticationservice.model.UserCredential;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
public class JWTTokenGeneratorService implements TokenGeneratorService{

    @Value("${jwt.secret.key}")
    private String secretKey;
    @Override
    public Map<String, String> generateToken(UserCredential credential) {
        String token = Jwts.builder().setSubject(credential.getEmail())
                .setIssuer("ToysAppIssuer")
                .setIssuedAt(new Date())
                .signWith(SignatureAlgorithm.HS256,secretKey)
                .compact();
        return Map.of("token",token);
    }
}