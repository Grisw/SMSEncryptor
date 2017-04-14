package pers.lxt.smsencryptor.receiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import pers.lxt.smsencryptor.R;
import pers.lxt.smsencryptor.activity.MainActivity;
import pers.lxt.smsencryptor.activity.MessageActivity;

public class SMSReceiver extends BroadcastReceiver {

    private static int notificationID = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        Object[] pdus = (Object[]) intent.getExtras().get("pdus");
        if(pdus!=null){
            ContentResolver contentResolver = context.getContentResolver();
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            for (Object pdu : pdus) {
                SmsMessage msg = SmsMessage.createFromPdu((byte[]) pdu);
                ContentValues values = new ContentValues();
                values.put("date", msg.getTimestampMillis());
                values.put("read", 0);
                values.put("type", 1);
                values.put("address", msg.getOriginatingAddress());
                values.put("body", msg.getMessageBody());
                contentResolver.insert(Uri.parse("content://sms/inbox"), values);

                Intent launchIntent = new Intent(context, MessageActivity.class);
                launchIntent.putExtra("phone_num",msg.getOriginatingAddress());
                PendingIntent pendingIntent = PendingIntent.getActivity(context,0,launchIntent,PendingIntent.FLAG_UPDATE_CURRENT);
                Notification notification = new Notification.Builder(context)
                                            .setContentTitle("信息："+msg.getOriginatingAddress())
                                            .setContentText(msg.getMessageBody())
                                            .setWhen(System.currentTimeMillis())
                                            .setContentIntent(pendingIntent)
                                            .setSmallIcon(R.mipmap.ic_launcher)
                                            .setAutoCancel(true)
                                            .build();
                notificationManager.notify(notificationID++,notification);
            }
        }
    }
}
