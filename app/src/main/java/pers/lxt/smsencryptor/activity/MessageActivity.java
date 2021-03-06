package pers.lxt.smsencryptor.activity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

import pers.lxt.smsencryptor.R;
import pers.lxt.smsencryptor.adapter.MessageAdapter;
import pers.lxt.smsencryptor.crypto.AESHelper;
import pers.lxt.smsencryptor.crypto.HMACHelper;
import pers.lxt.smsencryptor.crypto.RSAHelper;
import pers.lxt.smsencryptor.database.Contacts;

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
        ImageButton setting = (ImageButton) findViewById(R.id.setting);

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
                    Contacts contact = new Contacts(MessageActivity.this).select(phoneNum);
                    if(contact != null){
                        if(contact.getPublicKey()!=null&&contact.getPublicKey().length()>0){
                            SharedPreferences preferences = getSharedPreferences("key", Context.MODE_PRIVATE);
                            if(System.currentTimeMillis()>contact.getExpire()){
                                String sessionKey = AESHelper.genKey();
                                long expire = System.currentTimeMillis()+86400000;
                                contact.setSessionKey(sessionKey);
                                contact.setExpire(expire);
                                contact.update();
                                try {   //先获取明文的认证码，然后将组合消息签名，然后用会话密钥加密，然后再加密会话密钥
                                    String hmac = HMACHelper.sign(message, sessionKey);
                                    String clip = expire+","+message+","+hmac;
                                    clip = RSAHelper.sign(clip,preferences.getString("private_key",null));
                                    clip = AESHelper.encrypt(clip,sessionKey);
                                    sessionKey = RSAHelper.encrypt(sessionKey,contact.getPublicKey());
                                    message = "pers.lxt.smsencryptor,"+sessionKey+","+clip;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Toast.makeText(MessageActivity.this,"加密失败",Toast.LENGTH_SHORT).show();
                                }
                            }else{
                                if(contact.getSessionKey()!=null&&contact.getSessionKey().length()>0){
                                    try {
                                        String hmac = HMACHelper.sign(message, contact.getSessionKey());
                                        String clip = ","+message+","+hmac;
                                        clip = RSAHelper.sign(clip,preferences.getString("private_key",null));
                                        clip = AESHelper.encrypt(clip,contact.getSessionKey());
                                        message = "pers.lxt.smsencryptor,,"+clip;
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        Toast.makeText(MessageActivity.this,"加密失败",Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        }
                    }
                    Intent intent = new Intent(ACTION_SMS_SENT);
                    intent.putExtra("address",phoneNum);
                    intent.putExtra("body",input.getText().toString());
                    intent.putExtra("id",System.currentTimeMillis());
                    PendingIntent sent = PendingIntent.getBroadcast(MessageActivity.this,0,intent,PendingIntent.FLAG_ONE_SHOT);

                    //每66个字符加一个0或者1，0表示没有更多短信了，1表示后续还有短信
                    int i;
                    for(i = 0;i<message.length()-66;i+=66){
                        smsManager.sendTextMessage(phoneNum,null,message.substring(i, i+66)+"1",sent,null);
                    }
                    smsManager.sendTextMessage(phoneNum,null,message.substring(i, message.length())+"0",sent,null);

                    Log.i("发送短信",message);

                    input.setText("");
                }
            }
        });
        setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MessageActivity.this,ContactActivity.class);
                intent.putExtra("is_create",false);
                intent.putExtra("address",phoneNum);
                startActivity(intent);
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
