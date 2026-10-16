package com.moneykeeper.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA 加解密工具
 *
 * 密钥对首次启动时生成并持久化到文件 ~/.moneykeeper/rsa_keypair.dat，
 * 后续启动从文件加载，确保重启后公钥不变。
 */
public class RsaUtil {

    private static final Logger log = LoggerFactory.getLogger(RsaUtil.class);

    private static final String ALGORITHM = "RSA/ECB/PKCS1Padding";
    private static final int KEY_SIZE = 2048;

    private static final String KEY_DIR = System.getProperty("user.home") + File.separator + ".moneykeeper";
    private static final String KEY_FILE = KEY_DIR + File.separator + "rsa_keypair.dat";

    private final KeyPair keyPair;

    public RsaUtil() {
        File file = new File(KEY_FILE);
        if (file.exists()) {
            KeyPair loaded = loadKeyPair(file);
            if (loaded != null) {
                this.keyPair = loaded;
                log.info("RSA key pair loaded from: {}", KEY_FILE);
                return;
            }
            log.warn("Failed to load RSA key pair from file, will generate new one.");
        }
        this.keyPair = generateKeyPair();
        saveKeyPair(this.keyPair, file);
        log.info("RSA key pair generated and saved to: {}", KEY_FILE);
    }

    public String getPublicKeyPem() {
        String base64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        StringBuilder sb = new StringBuilder();
        sb.append("-----BEGIN PUBLIC KEY-----\n");
        for (int i = 0; i < base64.length(); i += 64) {
            sb.append(base64, i, Math.min(i + 64, base64.length()));
            sb.append('\n');
        }
        sb.append("-----END PUBLIC KEY-----");
        return sb.toString();
    }

    public String decrypt(String base64CipherText) {
        try {
            byte[] cipherBytes = Base64.getDecoder().decode(base64CipherText);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            byte[] decrypted = cipher.doFinal(cipherBytes);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("RSA decryption failed", e);
        }
    }

    private KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(KEY_SIZE, new SecureRandom());
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("RSA key pair generation failed", e);
        }
    }

    private void saveKeyPair(KeyPair kp, File file) {
        try {
            Files.createDirectories(Paths.get(KEY_DIR));
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                oos.writeObject(kp.getPrivate().getEncoded());
                oos.writeObject(kp.getPublic().getEncoded());
            }
        } catch (Exception e) {
            log.error("Failed to save RSA key pair", e);
        }
    }

    private KeyPair loadKeyPair(File file) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            byte[] privateKeyBytes = (byte[]) ois.readObject();
            byte[] publicKeyBytes = (byte[]) ois.readObject();
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
            PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
            return new KeyPair(publicKey, privateKey);
        } catch (Exception e) {
            log.error("Failed to load RSA key pair", e);
            return null;
        }
    }
}