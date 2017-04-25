package pers.lxt.smsencryptor.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import pers.lxt.smsencryptor.R;
import pers.lxt.smsencryptor.crypto.RSAHelper;

public class SettingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        ImageButton backBtn = (ImageButton) findViewById(R.id.back);
        Button genBtn = (Button) findViewById(R.id.generate_key);

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        genBtn.setOnClickListener(new View.OnClickListener() {
            private ImageView pkyImg = (ImageView) findViewById(R.id.public_key);

            @Override
            public void onClick(View v) {
                RSAHelper.KeyPair keyPair = RSAHelper.genKey();
                if(keyPair==null){
                    Toast.makeText(SettingActivity.this,"生成密钥失败",Toast.LENGTH_SHORT).show();
                    return;
                }
                SharedPreferences preferences = getSharedPreferences("key", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("public_key",keyPair.getPublicKey());
                editor.putString("private_key",keyPair.getPrivateKey());
                editor.apply();

                try {
                    int width = pkyImg.getWidth();
                    int height = pkyImg.getWidth();
                    Bitmap bmp = getQRCode(keyPair.getPublicKey(),width,height);
                    pkyImg.setImageBitmap(bmp);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(SettingActivity.this,"生成二维码失败",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus){
            ImageView pkyImg = (ImageView) findViewById(R.id.public_key);
            SharedPreferences preferences = getSharedPreferences("key", Context.MODE_PRIVATE);
            String publicKey = preferences.getString("public_key", null);
            if(publicKey != null){
                int width = pkyImg.getWidth();
                int height = pkyImg.getWidth();

                try {
                    Bitmap bmp = getQRCode(publicKey,width,height);
                    pkyImg.setImageBitmap(bmp);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(SettingActivity.this,"获取二维码失败",Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private Bitmap getQRCode(String content, int width, int height) throws Exception{
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix matrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE,width,height);
        int[] pixels = new int[width*height];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (matrix.get(j, i)) {
                    pixels[i * width + j] = 0x00000000;
                } else {
                    pixels[i * width + j] = 0xffffffff;
                }
            }
        }
        return Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.RGB_565);
    }
}
