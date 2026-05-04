package com.tlias.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire}")
    private Long expire;

    private static String STATIC_SECRET;
    private static Long STATIC_EXPIRE;

    @PostConstruct
    public void init() {
        STATIC_SECRET = secret;
        STATIC_EXPIRE = expire;
    }

    public static String generateToken(Integer empId, String username) {
        return JWT.create()
                .withClaim("empId", empId)
                .withClaim("username", username)
                .withExpiresAt(new Date(System.currentTimeMillis() + STATIC_EXPIRE))
                .sign(Algorithm.HMAC256(STATIC_SECRET));
    }

    public static DecodedJWT parseToken(String token) {
        return JWT.require(Algorithm.HMAC256(STATIC_SECRET))
                .build()
                .verify(token);
    }

    public static Integer getEmpId(String token) {
        return parseToken(token).getClaim("empId").asInt();
    }
}
