package pers.lxt.smsencryptor.activity;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import pers.lxt.smsencryptor.R;
import pers.lxt.smsencryptor.database.Database;

public class CreateContactActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_contact);

        ImageButton back = (ImageButton) findViewById(R.id.back);
        Button createBtn = (Button) findViewById(R.id.create);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        createBtn.setOnClickListener(new View.OnClickListener() {
            private EditText addressEdt = (EditText) findViewById(R.id.address);
            private EditText nameEdt = (EditText) findViewById(R.id.name);
            private EditText publicKeyEdt = (EditText) findViewById(R.id.public_key);

            @Override
            public void onClick(View v) {
                String address = addressEdt.getText().toString();
                String name = nameEdt.getText().toString();
                String publicKey = publicKeyEdt.getText().toString();

                if(address.length() == 0){
                    Toast.makeText(CreateContactActivity.this,"地址不能为空",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(name.length()==0){
                    name = null;
                }
                if(publicKey.length()==0){
                    publicKey = null;
                }

                SQLiteDatabase db = Database.getInstance(CreateContactActivity.this).getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("address",address);
                values.put("name",name);
                values.put("public_key",publicKey);
                db.insert("contacts",null,values);
                db.close();
                Intent intent = new Intent(CreateContactActivity.this,MessageActivity.class);
                intent.putExtra("phone_num",address);
                startActivity(intent);
                finish();
            }
        });
    }
}
