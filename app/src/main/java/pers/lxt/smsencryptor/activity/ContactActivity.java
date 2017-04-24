package pers.lxt.smsencryptor.activity;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
import pers.lxt.smsencryptor.database.Database;

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
            SQLiteDatabase db = Database.getInstance(ContactActivity.this).getReadableDatabase();
            Cursor cursor = db.query("contacts",new String[]{"name","public_key"},"address = ?",new String[]{addr},null,null,null);
            if(cursor!=null && cursor.moveToNext()){
                ((EditText) findViewById(R.id.name)).setText(cursor.getString(cursor.getColumnIndex("name")));
                publicKeyTxv.setText(cursor.getString(cursor.getColumnIndex("public_key")));
                cursor.close();
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

                SQLiteDatabase db = Database.getInstance(ContactActivity.this).getWritableDatabase();
                Cursor cursor = db.query("contacts",new String[]{"address"},"address = ?",new String[]{address},null,null,null);
                if(cursor!=null&&cursor.moveToNext()){
                    ContentValues values = new ContentValues();
                    values.put("name",name);
                    values.put("public_key",publicKey);
                    db.update("contacts",values,"address = ?",new String[]{address});
                    cursor.close();
                }else{
                    ContentValues values = new ContentValues();
                    values.put("address",address);
                    values.put("name",name);
                    values.put("public_key",publicKey);
                    db.insert("contacts",null,values);
                }
                db.close();
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
