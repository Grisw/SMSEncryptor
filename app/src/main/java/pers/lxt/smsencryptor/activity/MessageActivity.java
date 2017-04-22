package pers.lxt.smsencryptor.activity;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import pers.lxt.smsencryptor.R;
import pers.lxt.smsencryptor.adapter.MessageAdapter;
import pers.lxt.smsencryptor.database.Database;
import pers.lxt.smsencryptor.encrypt.AESHelper;
import pers.lxt.smsencryptor.encrypt.RSAHelper;

public class MessageActivity extends AppCompatActivity {

    public static final String ACTION_SMS_SENT = "pers.lxt.smsencryptor.SMS_SENT";

    public static final int SCROLL_RECYCLERVIEW = 0;

    public Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case SCROLL_RECYCLERVIEW:{
                    int pos = msg.arg1;

                    messages.scrollToPosition(pos);
                }break;
            }
            return false;
        }
    });

    private RecyclerView messages;
    private String phoneNum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        phoneNum = getIntent().getStringExtra("phone_num");

        messages = (RecyclerView) findViewById(R.id.messages);
        final ImageButton back = (ImageButton) findViewById(R.id.back);
        TextView title = (TextView) findViewById(R.id.phone_num);
        ImageButton send = (ImageButton) findViewById(R.id.send);

        messages.setLayoutManager(new LinearLayoutManager(this));
        MessageAdapter messageAdapter = new MessageAdapter(this, phoneNum);
        messages.setAdapter(messageAdapter);
        messages.setItemAnimator(new DefaultItemAnimator());
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        title.setText(phoneNum);
        send.setOnClickListener(new View.OnClickListener() {
            private EditText input = (EditText) findViewById(R.id.message);
            private SmsManager smsManager = SmsManager.getDefault();

            @Override
            public void onClick(View v) {
                String message = input.getText().toString();
                if(message.length() > 0){
                    SQLiteDatabase db = Database.getInstance(MessageActivity.this).getWritableDatabase();
                    Cursor cursor = db.query("contacts",new String[]{"public_key","session_key","expire"},"address = ?",new String[]{phoneNum},null,null,null);
                    if(cursor!=null&&cursor.moveToNext()){
                        long expire = cursor.getLong(cursor.getColumnIndex("expire"));
                        String publicKey = cursor.getString(cursor.getColumnIndex("public_key"));
                        String sessionKey = cursor.getString(cursor.getColumnIndex("session_key"));
                        if(publicKey!=null&&publicKey.length()>0){
                            if(System.currentTimeMillis()>expire){
                                sessionKey = AESHelper.genKey();
                                expire = System.currentTimeMillis()+86400000;
                                ContentValues values = new ContentValues();
                                values.put("expire",expire);
                                values.put("session_key",sessionKey);
                                try {
                                    message = AESHelper.encrypt(message,sessionKey);
                                    sessionKey = RSAHelper.encrypt(sessionKey,publicKey);
                                    message = "pers.lxt.smsencryptor,"+sessionKey+","+expire+","+message;
                                    db.update("contacts",values,"address = ?",new String[]{phoneNum});
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Toast.makeText(MessageActivity.this,"加密失败",Toast.LENGTH_SHORT).show();
                                }
                            }else{
                                if(sessionKey!=null&&sessionKey.length()>0){
                                    try {
                                        message = AESHelper.encrypt(message,sessionKey);
                                        message = "pers.lxt.smsencryptor,,,"+message;
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        Toast.makeText(MessageActivity.this,"加密失败",Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        }
                        cursor.close();
                    }
                    Intent intent = new Intent(ACTION_SMS_SENT);
                    intent.putExtra("address",phoneNum);
                    intent.putExtra("body",input.getText().toString());
                    intent.putExtra("id",System.currentTimeMillis());
                    PendingIntent sent = PendingIntent.getBroadcast(MessageActivity.this,0,intent,PendingIntent.FLAG_ONE_SHOT);

                    int i;
                    for(i = 0;i<message.length()-66;i+=66){
                        smsManager.sendTextMessage(phoneNum,null,message.substring(i, i+66)+"1",sent,null);
                    }
                    smsManager.sendTextMessage(phoneNum,null,message.substring(i, message.length())+"0",sent,null);

                    input.setText("");
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        ((MessageAdapter)messages.getAdapter()).unload();
    }

    @Override
    protected void onResume() {
        super.onResume();

        ((MessageAdapter)messages.getAdapter()).load();
    }
}
