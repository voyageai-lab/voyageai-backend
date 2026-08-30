package com.voyageai.voyageaibackend.util;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AES-256-GCM encryption for storing sensitive values (API keys) at rest.
 * The secret is configurable via {@code voyageai.api-key-secret}.
 * Each encryption generates a random 12-byte IV, prepended to the ciphertext.
 */
@Component
public class ApiKeyEncryptor {

  private static final String ALGORITHM = "AES/GCM/NoPadding";
  private static final int GCM_TAG_BITS = 128;
  private static final int IV_BYTES = 12;

  private final SecretKeySpec keySpec;

  public ApiKeyEncryptor(
      @Value("${voyageai.api-key-secret:ch4ng3-th1s-s3cr3t-1n-pr0d}") String secret
  ) {
    byte[] keyBytes = new byte[32];
    byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
    System.arraycopy(raw, 0, keyBytes, 0, Math.min(raw.length, 32));
    this.keySpec = new SecretKeySpec(keyBytes, "AES");
  }

  public String encrypt(String plaintext) {
    try {
      byte[] iv = new byte[IV_BYTES];
      new SecureRandom().nextBytes(iv);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
      byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

      byte[] combined = new byte[IV_BYTES + ciphertext.length];
      System.arraycopy(iv, 0, combined, 0, IV_BYTES);
      System.arraycopy(ciphertext, 0, combined, IV_BYTES, ciphertext.length);

      return Base64.getEncoder().encodeToString(combined);
    } catch (Exception e) {
      throw new IllegalStateException("Encryption failed", e);
    }
  }

  public String decrypt(String encrypted) {
    try {
      byte[] combined = Base64.getDecoder().decode(encrypted);
      byte[] iv = new byte[IV_BYTES];
      System.arraycopy(combined, 0, iv, 0, IV_BYTES);

      byte[] ciphertext = new byte[combined.length - IV_BYTES];
      System.arraycopy(combined, IV_BYTES, ciphertext, 0, ciphertext.length);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
      return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("Decryption failed", e);
    }
  }

  /**
   * Masks an API key for display: "sk-proj-...abcd"
   */
  public static String mask(String apiKey) {
    if (apiKey == null || apiKey.length() < 8) return "****";
    return apiKey.substring(0, 7) + "..." + apiKey.substring(apiKey.length() - 4);
  }
}
