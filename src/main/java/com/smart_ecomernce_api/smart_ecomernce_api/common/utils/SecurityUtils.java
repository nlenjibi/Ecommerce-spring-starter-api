package com.smart_ecomernce_api.smart_ecomernce_api.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * SecurityUtils provides security-related utility methods
 * Handles password hashing, salt generation, and other security operations
 */
public class SecurityUtils {
    private static final Logger logger = LoggerFactory.getLogger(SecurityUtils.class);
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private SecurityUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Hash password using SHA-256
     * @param password Plain text password
     * @return Hashed password as hex string
     * @throws SecurityException if hashing fails
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = md.digest(password.getBytes());
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Error hashing password", e);
            throw new SecurityException("Error hashing password", e);
        }
    }

    /**
     * Hash password with salt (more secure)
     * @param password Plain text password
     * @param salt Salt value
     * @return Hashed password
     * @throws SecurityException if hashing fails
     */
    public static String hashPasswordWithSalt(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(salt.getBytes());
            byte[] hash = md.digest(password.getBytes());
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Error hashing password with salt", e);
            throw new SecurityException("Error hashing password with salt", e);
        }
    }

    /**
     * Generate random salt for password hashing
     * @return Base64 encoded salt
     */
    public static String generateSalt() {
        byte[] salt = new byte[16];
        SECURE_RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Generate secure random token
     * @param length Token length
     * @return Random token string
     */
    public static String generateToken(int length) {
        byte[] token = new byte[length];
        SECURE_RANDOM.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    /**
     * Verify password against hash
     * @param password Plain text password
     * @param hash Stored hash
     * @return true if password matches
     */
    public static boolean verifyPassword(String password, String hash) {
        String hashedInput = hashPassword(password);
        return hashedInput.equals(hash);
    }

    /**
     * Verify password with salt
     * @param password Plain text password
     * @param salt Salt used in original hash
     * @param hash Stored hash
     * @return true if password matches
     */
    public static boolean verifyPasswordWithSalt(String password, String salt, String hash) {
        String hashedInput = hashPasswordWithSalt(password, salt);
        return hashedInput.equals(hash);
    }

    /**
     * Sanitize input to prevent SQL injection
     * @param input User input
     * @return Sanitized input
     */
    public static String sanitizeInput(String input) {
        if (input == null) return null;

        // Remove potentially dangerous characters
        return input.replaceAll("[';\"\\\\]", "").trim();
    }

    /**
     * Validate session token
     * @param token Session token
     * @return true if valid format
     */
    public static boolean isValidToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        // Check token format and length
        return token.length() >= 32 && token.matches("[A-Za-z0-9_-]+");
    }

    /**
     * Convert byte array to hex string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Mask sensitive data for logging
     * @param data Sensitive data
     * @return Masked data
     */
    public static String maskSensitiveData(String data) {
        if (data == null || data.length() <= 4) {
            return "****";
        }
        return data.substring(0, 2) + "****" + data.substring(data.length() - 2);
    }

    /**
     * Check if string contains SQL injection patterns
     * @param input User input
     * @return true if suspicious
     */
    public static boolean containsSQLInjection(String input) {
        if (input == null) return false;

        String lowerInput = input.toLowerCase();
        String[] sqlKeywords = {"select", "insert", "update", "delete", "drop", "union", "exec", "--", "/*"};

        for (String keyword : sqlKeywords) {
            if (lowerInput.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generate secure session ID
     * @return Session ID
     */
    public static String generateSessionId() {
        return generateToken(32);
    }
}

