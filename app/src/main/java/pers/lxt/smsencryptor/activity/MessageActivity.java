package pers.lxt.smsencryptor.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import pers.lxt.smsencryptor.R;
import pers.lxt.smsencryptor.adapter.MessageAdapter;

public class MessageActivity extends AppCompatActivity {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        String phoneNum = getIntent().getStringExtra("phone_num");

        messages = (RecyclerView) findViewById(R.id.messages);
        ImageButton back = (ImageButton) findViewById(R.id.back);
        TextView title = (TextView) findViewById(R.id.phone_num);

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
