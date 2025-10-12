package com.voyageai.voyageaibackend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Utility class for JWT token operations.
 * Handles token generation, validation, and claims extraction.
 */
@Component
public class JwtUtil {

  /**
   * Secret key for JWT signing (should be externalized in production).
   */
  @Value("${jwt.secret:mySecretKeyForJWT123456789012345678901234567890}")
  private String secret;

  /**
   * Token expiration time in milliseconds (default: 24 hours).
   */
  @Value("${jwt.expiration:86400000}")
  private Long expiration;

  /**
   * Generates a JWT token for the given email.
   *
   * @param email the user's email address
   * @return the generated JWT token
   */
  public String generateToken(String email) {
    Map<String, Object> claims = new HashMap<>();
    return createToken(claims, email);
  }

  /**
   * Generates a JWT token with custom claims.
   *
   * @param email the user's email address
   * @param userId the user's ID
   * @return the generated JWT token
   */
  public String generateToken(String email, Long userId) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId);
    return createToken(claims, email);
  }

  /**
   * Creates a JWT token with specified claims and subject.
   *
   * @param claims additional claims to include in the token
   * @param subject the subject (typically user email)
   * @return the generated JWT token
   */
  private String createToken(Map<String, Object> claims, String subject) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + expiration);

    return Jwts.builder()
        .setClaims(claims)
        .setSubject(subject)
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .signWith(getSigningKey(), SignatureAlgorithm.HS256)
        .compact();
  }

  /**
   * Extracts the email (subject) from the token.
   *
   * @param token the JWT token
   * @return the email address
   */
  public String extractEmail(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  /**
   * Extracts the user ID from the token.
   *
   * @param token the JWT token
   * @return the user ID
   */
  public Long extractUserId(String token) {
    return extractClaim(token, claims -> claims.get("userId", Long.class));
  }

  /**
   * Extracts the expiration date from the token.
   *
   * @param token the JWT token
   * @return the expiration date
   */
  public Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  /**
   * Extracts a specific claim from the token.
   *
   * @param token the JWT token
   * @param claimsResolver function to extract the desired claim
   * @param <T> the type of the claim
   * @return the extracted claim value
   */
  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  /**
   * Extracts all claims from the token.
   *
   * @param token the JWT token
   * @return all claims
   */
  private Claims extractAllClaims(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(getSigningKey())
        .build()
        .parseClaimsJws(token)
        .getBody();
  }

  /**
   * Checks if the token has expired.
   *
   * @param token the JWT token
   * @return true if expired, false otherwise
   */
  private Boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  /**
   * Validates the token against a user email.
   *
   * @param token the JWT token
   * @param email the user's email
   * @return true if valid, false otherwise
   */
  public Boolean validateToken(String token, String email) {
    final String tokenEmail = extractEmail(token);
    return (tokenEmail.equals(email) && !isTokenExpired(token));
  }

  /**
   * Validates the token (without checking against a specific user).
   *
   * @param token the JWT token
   * @return true if valid and not expired, false otherwise
   */
  public Boolean validateToken(String token) {
    try {
      return !isTokenExpired(token);
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Gets the signing key for JWT operations.
   *
   * @return the signing key
   */
  private Key getSigningKey() {
    byte[] keyBytes = secret.getBytes();
    return Keys.hmacShaKeyFor(keyBytes);
  }
}

