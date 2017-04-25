package pers.lxt.smsencryptor.activity;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import pers.lxt.smsencryptor.R;
import pers.lxt.smsencryptor.database.Contacts;

public class ContactActivity extends AppCompatActivity {

    private static final int RESULT_LOAD_IMAGE = 0;
    private static final int RESULT_CAMERA_SCAN = 1;

    private TextView publicKeyTxv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);

        ImageButton back = (ImageButton) findViewById(R.id.back);
        Button createBtn = (Button) findViewById(R.id.create);
        Button fromImgBtn = (Button) findViewById(R.id.get_pky_img);
        Button fromCameraBtn = (Button) findViewById(R.id.get_pky_camera);
        publicKeyTxv = (TextView) findViewById(R.id.public_key);

        if(!getIntent().getBooleanExtra("is_create",true)){
            createBtn.setText("修改");
            String addr = getIntent().getStringExtra("address");
            findViewById(R.id.address).setEnabled(false);
            ((EditText) findViewById(R.id.address)).setText(addr);
            Contacts contact = new Contacts(this).select(addr);
            if(contact != null){
                ((EditText) findViewById(R.id.name)).setText(contact.getName());
                publicKeyTxv.setText(contact.getPublicKey());
            }
        }

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        createBtn.setOnClickListener(new View.OnClickListener() {
            private EditText addressEdt = (EditText) findViewById(R.id.address);
            private EditText nameEdt = (EditText) findViewById(R.id.name);

            @Override
            public void onClick(View v) {
                String address = addressEdt.getText().toString();
                String name = nameEdt.getText().toString();
                String publicKey = publicKeyTxv.getText().toString();

                if(address.length() == 0){
                    Toast.makeText(ContactActivity.this,"地址不能为空",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(name.length()==0){
                    name = null;
                }
                if(publicKey.length()==0){
                    publicKey = null;
                }

                Contacts contact = new Contacts(ContactActivity.this).select(address);
                if(contact == null){
                    contact = new Contacts(ContactActivity.this);
                    contact.setAddress(address);
                    contact.setName(name);
                    contact.setPublicKey(publicKey);
                    contact.insert();
                }else{
                    contact.setName(name);
                    contact.setPublicKey(publicKey);
                    contact.update();
                }
                if(getIntent().getBooleanExtra("is_create",true)){
                    Intent intent = new Intent(ContactActivity.this,MessageActivity.class);
                    intent.putExtra("phone_num",address);
                    startActivity(intent);
                }
                finish();
            }
        });
        fromImgBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });
        fromCameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(ContactActivity.this, CaptureActivity.class), RESULT_CAMERA_SCAN);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case RESULT_LOAD_IMAGE:{
                if(resultCode == RESULT_OK && data != null){
                    Uri selectedImage = data.getData();
                    String[] filePathColumn = { MediaStore.Images.Media.DATA };

                    Cursor cursor = getContentResolver().query(selectedImage,
                            filePathColumn, null, null, null);
                    if(cursor!=null&&cursor.moveToFirst()){
                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        String picturePath = cursor.getString(columnIndex);
                        cursor.close();

                        Bitmap bmp = BitmapFactory.decodeFile(picturePath);
                        int[] pixels = new int[bmp.getWidth()*bmp.getHeight()];
                        bmp.getPixels(pixels,0,bmp.getWidth(),0,0,bmp.getWidth(),bmp.getHeight());
                        RGBLuminanceSource source = new RGBLuminanceSource(bmp.getWidth(),bmp.getHeight(),pixels);
                        BinaryBitmap bitmap1 = new BinaryBitmap(new HybridBinarizer(source));
                        QRCodeReader reader = new QRCodeReader();
                        try {
                            Result result = reader.decode(bitmap1);
                            publicKeyTxv.setText(result.getText());
                        } catch (NotFoundException | ChecksumException | FormatException e) {
                            e.printStackTrace();
                            Toast.makeText(this,"读取二维码失败",Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }break;
            case RESULT_CAMERA_SCAN:{
                if(resultCode == RESULT_OK && data != null){
                    Bundle bundle = data.getExtras();
                    if (bundle != null) {
                        String result = bundle.getString("result");
                        publicKeyTxv.setText(result);
                    }
                }
            }break;
        }
    }
}
