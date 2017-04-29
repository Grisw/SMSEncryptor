package pers.lxt.smsencryptor.crypto;

import android.util.Base64;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by MissingNo on 2017/4/25.
 */

public class HMACHelper {

    public static String sign(String data, String key) throws Exception {
        SecretKey secretKey = new SecretKeySpec(Base64.decode(key,Base64.DEFAULT), "HmacSHA256");
        Mac mac = Mac.getInstance(secretKey.getAlgorithm());
        mac.init(secretKey);
        byte[] bytes = mac.doFinal(data.getBytes("utf-8"));
        return Base64.encodeToString(bytes,Base64.DEFAULT);
    }


}
