package pers.lxt.smsencryptor.activity;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import pers.lxt.smsencryptor.R;
import pers.lxt.smsencryptor.adapter.MessageAdapter;

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
                    Intent intent = new Intent(ACTION_SMS_SENT);
                    intent.putExtra("address",phoneNum);
                    intent.putExtra("body",message);
                    PendingIntent sent = PendingIntent.getBroadcast(MessageActivity.this,0,intent,PendingIntent.FLAG_ONE_SHOT);
                    smsManager.sendTextMessage(phoneNum,null,message,sent,null);
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
