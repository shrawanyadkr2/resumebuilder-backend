package com.shrawan.resumebuilder.util;

import java.security.Key;
import java.util.*;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.xml.crypto.Data;


@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public String generateToken(String userId){
        Date now = new Date();
        Date expirydate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(now)
                .setExpiration(expirydate)
                .signWith(getSigningkey())
                .compact();

    }

    private Key getSigningkey() {

        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }


    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(getSigningkey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return  claims.getSubject();

    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(getSigningkey())
                    .build()
                    .parseClaimsJws(token); // 👈 parseClaimsJws (with 's')
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(getSigningkey())
                    .build()
                    .parseClaimsJws(token) // 👈 parseClaimsJws (with 's')
                    .getBody();
            return claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }
}
