package pers.lxt.smsencryptor.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import pers.lxt.smsencryptor.R;

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

                //TODO 从contacts表中添加条目
            }
        });
    }
}
