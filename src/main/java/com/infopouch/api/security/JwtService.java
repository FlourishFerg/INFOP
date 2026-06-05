package com.infopouch.api.security;

import com.infopouch.api.modules.users.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class JwtService {

  @Value("${jwt.secret}")
  private String secretKey;

  @Value("${jwt.access-expiration}")
  private long accessExpiration;

  @Value("${jwt.refresh-expiration}")
  private long refreshExpiration;

  private Key signingKey;

  /** Validate JWT configuration during startup. */
  @PostConstruct
  protected void init() {
    if (secretKey == null || secretKey.trim().length() < 32) {
      throw new IllegalArgumentException("JWT secret must be at least 32 characters long.");
    }

    /*
     * Supports either:
     * - Base64 encoded secrets
     * - Plain text secrets
     */
    try {
      byte[] keyBytes = Decoders.BASE64.decode(secretKey);
      this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    } catch (Exception ex) {
      log.warn("JWT secret is not Base64 encoded. Falling back to raw string secret.");
      this.signingKey = Keys.hmacShaKeyFor(secretKey.getBytes());
    }
  }

  /** Generate Access Token */
  public String generateAccessToken(User user) {
    Map<String, Object> claims = new HashMap<>();

    // FIX: Changed .getRoleName() to .name() for the Enum conversion
    claims.put("role", user.getRole().name());
    claims.put("token_type", "ACCESS");

    return generateToken(claims, user.getEmail(), accessExpiration);
  }

  /** Generate Refresh Token */
  public String generateRefreshToken(User user) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("token_type", "REFRESH");

    return generateToken(claims, user.getEmail(), refreshExpiration);
  }

  /** Core JWT Builder */
  private String generateToken(Map<String, Object> extraClaims, String subject, long expiration) {
    return Jwts.builder()
        .setClaims(extraClaims)
        .setSubject(subject)
        .setIssuedAt(new Date(System.currentTimeMillis()))
        .setExpiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(signingKey, SignatureAlgorithm.HS256)
        .compact();
  }

  /** Extract Username (Email) */
  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  /** Generic Claim Extractor */
  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  /** Validate Token */
  public boolean isTokenValid(String token, UserDetails userDetails) {
    final String username = extractUsername(token);
    return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
  }

  /** Validate Token Type */
  public boolean isRefreshToken(String token) {
    String tokenType = extractClaim(token, claims -> claims.get("token_type", String.class));
    return "REFRESH".equals(tokenType);
  }

  /** Check Expiration */
  private boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  /** Extract Expiration Date */
  private Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  /** Parse Claims */
  private Claims extractAllClaims(String token) {
    return Jwts.parser()
        .verifyWith((javax.crypto.SecretKey) signingKey)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }
}
