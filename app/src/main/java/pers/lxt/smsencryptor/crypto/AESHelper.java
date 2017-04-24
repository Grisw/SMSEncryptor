package pers.lxt.smsencryptor.crypto;

import android.util.Base64;

import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by MissingNo on 2017/4/16.
 */

public class AESHelper {

    public static String genKey(){
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(128);
            SecretKey secretKey = generator.generateKey();
            return Base64.encodeToString(secretKey.getEncoded(),Base64.DEFAULT);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String encrypt(String content, String key) throws Exception{
        SecretKeySpec keySpec = new SecretKeySpec(Base64.decode(key,Base64.DEFAULT),"AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec iv = new IvParameterSpec("514335188@qq.com".getBytes());
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv);
        byte[] result = cipher.doFinal(content.getBytes("utf-8"));
        return Base64.encodeToString(result,Base64.DEFAULT);
    }

    public static String decrypt(String content, String key) throws Exception{
        SecretKeySpec keySpec = new SecretKeySpec(Base64.decode(key,Base64.DEFAULT),"AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec iv = new IvParameterSpec("514335188@qq.com".getBytes());
        cipher.init(Cipher.DECRYPT_MODE, keySpec, iv);
        byte[] result = cipher.doFinal(Base64.decode(content,Base64.DEFAULT));
        return new String(result,"utf-8");
    }
}
