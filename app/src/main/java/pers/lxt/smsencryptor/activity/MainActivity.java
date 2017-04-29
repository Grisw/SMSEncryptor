package pers.lxt.smsencryptor.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Telephony;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import pers.lxt.smsencryptor.R;
import pers.lxt.smsencryptor.adapter.ContactsAdapter;

public class MainActivity extends AppCompatActivity {

    public static final String[] PERMISSIONS = {
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CAMERA
    };

    private ContactsAdapter contactsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView contacts = (RecyclerView) findViewById(R.id.contacts);
        ImageButton createMsg = (ImageButton) findViewById(R.id.create_msg);
        ImageButton settingBtn = (ImageButton) findViewById(R.id.setting);

        contacts.setLayoutManager(new LinearLayoutManager(this));
        contactsAdapter = new ContactsAdapter(this);
        contacts.setAdapter(contactsAdapter);
        contacts.setItemAnimator(new DefaultItemAnimator());
        createMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,ContactActivity.class);
                intent.putExtra("is_create",true);
                startActivity(intent);
            }
        });
        settingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this,SettingActivity.class));
            }
        });

        String currentPn = getPackageName();//获取当前程序包名
        String defaultSmsApp = Telephony.Sms.getDefaultSmsPackage(this);//获取手机当前设置的默认短信应用的包名
        if (!defaultSmsApp.equals(currentPn)) {
            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, currentPn);
            startActivity(intent);
            Toast.makeText(this,"请先设置为默认短信应用",Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        contactsAdapter.unload();
    }

    @Override
    protected void onResume() {
        super.onResume();

        contactsAdapter.load();

        for(String permission : PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED){
                Toast.makeText(this,"应用缺少权限，请先到设置中开启相关权限",Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
