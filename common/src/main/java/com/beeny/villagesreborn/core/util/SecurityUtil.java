package com.beeny.villagesreborn.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Security utilities for encryption and key management
 * Provides AES-GCM encryption for sensitive configuration data
 */
public class SecurityUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityUtil.class);
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    
    /**
     * Encrypt a plaintext string using AES-GCM
     */
    public static String encrypt(String plaintext) {
        try {
            SecretKey key = getOrGenerateKey();
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);
            
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);
            
            byte[] encryptedData = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            
            // Combine IV and encrypted data
            byte[] encryptedWithIv = new byte[GCM_IV_LENGTH + encryptedData.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedData, 0, encryptedWithIv, GCM_IV_LENGTH, encryptedData.length);
            
            return Base64.getEncoder().encodeToString(encryptedWithIv);
            
        } catch (Exception e) {
            LOGGER.error("Encryption failed", e);
            throw new RuntimeException("Failed to encrypt data", e);
        }
    }
    
    /**
     * Decrypt an encrypted string using AES-GCM
     */
    public static String decrypt(String encryptedData) {
        try {
            SecretKey key = getOrGenerateKey();
            
            byte[] decodedData = Base64.getDecoder().decode(encryptedData);
            
            // Extract IV and encrypted data
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[decodedData.length - GCM_IV_LENGTH];
            
            System.arraycopy(decodedData, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(decodedData, GCM_IV_LENGTH, encrypted, 0, encrypted.length);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);
            
            byte[] decryptedData = cipher.doFinal(encrypted);
            
            return new String(decryptedData, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            LOGGER.error("Decryption failed", e);
            throw new RuntimeException("Failed to decrypt data", e);
        }
    }
    
    /**
     * Generate a new AES key for encryption
     */
    public static String generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(256);
            SecretKey key = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            LOGGER.error("Key generation failed", e);
            throw new RuntimeException("Failed to generate encryption key", e);
        }
    }
    
    /**
     * Get encryption key from environment or generate a new one
     */
    private static SecretKey getOrGenerateKey() {
        String keyEnv = System.getenv("VILLAGESREBORN_ENCRYPTION_KEY");
        
        if (keyEnv != null && !keyEnv.trim().isEmpty()) {
            byte[] keyBytes = Base64.getDecoder().decode(keyEnv.trim());
            return new SecretKeySpec(keyBytes, ALGORITHM);
        }
        
        // Development fallback - generate a deterministic key
        LOGGER.warn("No encryption key found in environment, using development key");
        byte[] keyBytes = "VillagesRebornDevKey123456789012".getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }
    
    /**
     * Securely clear sensitive data from memory
     */
    public static void clearSensitiveData(char[] data) {
        if (data != null) {
            for (int i = 0; i < data.length; i++) {
                data[i] = '\0';
            }
        }
    }
    
    /**
     * Validate that a string is properly encrypted
     */
    public static boolean isEncrypted(String data) {
        return data != null && data.startsWith("ENC(") && data.endsWith(")");
    }
}