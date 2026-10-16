package com.moneykeeper.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-CBC 加解密工具
 * 与前端 crypto-js 的 AES-CBC 模式兼容
 */
public class AesUtil {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int KEY_SIZE = 32;  // 256 bits
    private static final int IV_SIZE = 16;   // 128 bits

    private AesUtil() {}

    /**
     * AES 加密
     * @param plainText  明文
     * @param base64Key  Base64 编码的 AES 密钥
     * @param base64Iv   Base64 编码的 IV
     * @return Base64 编码的密文
     */
    public static String encrypt(String plainText, String base64Key, String base64Iv) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            byte[] ivBytes = Base64.getDecoder().decode(base64Iv);

            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("AES encryption failed", e);
        }
    }

    /**
     * AES 解密
     * @param base64CipherText Base64 编码的密文
     * @param base64Key        Base64 编码的 AES 密钥
     * @param base64Iv         Base64 编码的 IV
     * @return 明文
     */
    public static String decrypt(String base64CipherText, String base64Key, String base64Iv) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            byte[] ivBytes = Base64.getDecoder().decode(base64Iv);
            byte[] cipherBytes = Base64.getDecoder().decode(base64CipherText);

            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] decrypted = cipher.doFinal(cipherBytes);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES decryption failed", e);
        }
    }

    /**
     * 生成随机 AES-256 密钥（Base64 编码）
     */
    public static String generateKey() {
        byte[] key = new byte[KEY_SIZE];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    /**
     * 生成随机 IV（Base64 编码）
     */
    public static String generateIv() {
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);
        return Base64.getEncoder().encodeToString(iv);
    }
}
