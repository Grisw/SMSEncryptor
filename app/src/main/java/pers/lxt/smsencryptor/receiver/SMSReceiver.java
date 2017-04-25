package pers.lxt.smsencryptor.receiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.telephony.SmsMessage;

import java.util.HashMap;
import java.util.Map;

import pers.lxt.smsencryptor.R;
import pers.lxt.smsencryptor.activity.MessageActivity;
import pers.lxt.smsencryptor.crypto.AESHelper;
import pers.lxt.smsencryptor.crypto.RSAHelper;
import pers.lxt.smsencryptor.database.Contacts;

public class SMSReceiver extends BroadcastReceiver {

    private static int notificationID = 0;
    private static Map<String,StringBuilder> raw = new HashMap<>();

    @Override
    public void onReceive(Context context, Intent intent) {
        Object[] pdus = (Object[]) intent.getExtras().get("pdus");
        if(pdus!=null){
            for (Object pdu : pdus) {
                SmsMessage msg = SmsMessage.createFromPdu((byte[]) pdu);
                if(msg.getMessageBody().startsWith("pers.lxt.smsencryptor")){
                    if(msg.getMessageBody().endsWith("1")){
                        StringBuilder stringBuilder = new StringBuilder(msg.getMessageBody().substring(0,msg.getMessageBody().length()-1));
                        raw.put(msg.getOriginatingAddress(),stringBuilder);
                    }else{
                        String content = processMessage(context,msg.getOriginatingAddress(),msg.getMessageBody().substring(0,msg.getMessageBody().length()-1));
                        notifySMS(context,msg.getOriginatingAddress(),content,msg.getTimestampMillis());
                    }
                }else{
                    if(raw.containsKey(msg.getOriginatingAddress())){
                        if(msg.getMessageBody().endsWith("1")){
                            raw.get(msg.getOriginatingAddress()).append(msg.getMessageBody().substring(0,msg.getMessageBody().length()-1));
                        }else{
                            String content = processMessage(context,msg.getOriginatingAddress(),raw.remove(msg.getOriginatingAddress()).toString()+msg.getMessageBody().substring(0,msg.getMessageBody().length()-1));
                            notifySMS(context,msg.getOriginatingAddress(),content,msg.getTimestampMillis());
                        }
                    }else{
                        notifySMS(context,msg.getOriginatingAddress(),msg.getMessageBody(),msg.getTimestampMillis());
                    }
                }
            }
        }
    }

    private void notifySMS(Context context, String address, String content, long time){
        ContentResolver contentResolver = context.getContentResolver();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        ContentValues values = new ContentValues();
        values.put("date", time);
        values.put("read", 0);
        values.put("type", 1);
        values.put("address", address);
        values.put("body", content);
        contentResolver.insert(Uri.parse("content://sms/inbox"), values);

        Intent launchIntent = new Intent(context, MessageActivity.class);
        launchIntent.putExtra("phone_num",address);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,0,launchIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(context)
                .setContentTitle("信息："+address)
                .setContentText(content)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(true)
                .build();
        notificationManager.notify(notificationID++, notification);
    }

    private String processMessage(Context context, String address, String message){
        String[] messageBody = message.split(",");
        String result = message;
        if(messageBody.length==4){
            Contacts contact = new Contacts(context).select(address);
            if(messageBody[1].length()>0&&messageBody[2].length()>0){
                SharedPreferences preferences = context.getSharedPreferences("key",Context.MODE_PRIVATE);
                String privateKey = preferences.getString("private_key",null);
                if(privateKey != null){
                    try {
                        String sessionKey = RSAHelper.decrypt(messageBody[1],privateKey);

                        if(contact!=null){
                            contact.setSessionKey(sessionKey);
                            contact.setExpire(Long.parseLong(messageBody[2]));
                            contact.update();
                        }else{
                            contact = new Contacts(context);
                            contact.setAddress(address);
                            contact.setSessionKey(sessionKey);
                            contact.setExpire(Long.parseLong(messageBody[2]));
                            contact.insert();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            if(contact != null){
                String sessionKey = contact.getSessionKey();
                try {
                    result = AESHelper.decrypt(messageBody[3],sessionKey);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }
}
