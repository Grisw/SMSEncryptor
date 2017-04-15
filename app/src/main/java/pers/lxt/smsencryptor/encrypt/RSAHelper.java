package pers.lxt.smsencryptor.encrypt;

import android.util.Base64;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

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

    public static class KeyPair{
        private String publicKey;
        private String privateKey;

        public KeyPair(java.security.KeyPair keyPair){
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
