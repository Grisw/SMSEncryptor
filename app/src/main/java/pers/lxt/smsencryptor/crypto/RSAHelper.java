package pers.lxt.smsencryptor.crypto;

import android.util.Base64;

import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

/**
 * Created by MissingNo on 2017/4/15.
 */

public class RSAHelper {

    public static KeyPair genKey(){
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(1024);
            return new KeyPair(generator.generateKeyPair());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String encrypt(String sessionKey, String publicKey) throws Exception{
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.decode(publicKey,Base64.DEFAULT));
        RSAPublicKey rsaPublicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(keySpec);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey);
        byte[] result = cipher.doFinal(sessionKey.getBytes("ascii"));
        return Base64.encodeToString(result,Base64.DEFAULT);
    }

    public static String sign(String content, String privateKey) throws Exception{
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.decode(privateKey,Base64.DEFAULT));
        RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, rsaPrivateKey);
        byte[] result = cipher.doFinal(content.getBytes("utf-8"));
        return Base64.encodeToString(result,Base64.DEFAULT);
    }

    public static String decodeSign(String contentBase64, String publicKey) throws Exception{
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.decode(publicKey,Base64.DEFAULT));
        RSAPublicKey rsaPublicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(keySpec);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, rsaPublicKey);
        byte[] result = cipher.doFinal(Base64.decode(contentBase64,Base64.DEFAULT));
        return new String(result,"utf-8");
    }

    public static String decrypt(String sessionKey, String privateKey) throws Exception{
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.decode(privateKey,Base64.DEFAULT));
        RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);
        byte[] result = cipher.doFinal(Base64.decode(sessionKey,Base64.DEFAULT));
        return new String(result,"ascii");
    }

    public static class KeyPair{
        private String publicKey;
        private String privateKey;

        KeyPair(java.security.KeyPair keyPair){
            publicKey = Base64.encodeToString(keyPair.getPublic().getEncoded(),Base64.DEFAULT);
            privateKey = Base64.encodeToString(keyPair.getPrivate().getEncoded(),Base64.DEFAULT);
        }


        public String getPublicKey() {
            return publicKey;
        }

        public String getPrivateKey() {
            return privateKey;
        }
    }
}
