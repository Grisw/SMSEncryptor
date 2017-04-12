package pers.lxt.smsencryptor.receiver;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsMessage;

public class SMSReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Object[] pdus = (Object[]) intent.getExtras().get("pdus");
        ContentResolver contentResolver = context.getContentResolver();
        for(int i = 0;i<pdus.length;i++){
            SmsMessage msg = SmsMessage.createFromPdu((byte[]) pdus[i]);
            ContentValues values = new ContentValues();
            values.put("date", msg.getTimestampMillis());
            values.put("read", 0);
            values.put("type", 1);
            values.put("address",msg.getOriginatingAddress());
            values.put("body", msg.getMessageBody());
            contentResolver.insert(Uri.parse("content://sms/inbox"), values);
        }
    }
}
