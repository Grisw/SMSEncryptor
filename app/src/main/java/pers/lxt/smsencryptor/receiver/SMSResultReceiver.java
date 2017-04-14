package pers.lxt.smsencryptor.receiver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import pers.lxt.smsencryptor.activity.MessageActivity;

public class SMSResultReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (getResultCode()){
            case Activity.RESULT_OK:{
                ContentResolver contentResolver = context.getContentResolver();
                ContentValues values = new ContentValues();
                values.put("address",intent.getStringExtra("address"));
                values.put("read",1);
                values.put("type",2);
                values.put("date",System.currentTimeMillis());
                values.put("body",intent.getStringExtra("body"));
                contentResolver.insert(Uri.parse("content://sms/sent"),values);
            }break;
            default:{
                Toast.makeText(context,"短信发送失败！",Toast.LENGTH_SHORT).show();
            }break;
        }
    }
}
