package pers.lxt.smsencryptor.receiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.telephony.SmsMessage;

import java.util.HashMap;
import java.util.Map;

import pers.lxt.smsencryptor.R;
import pers.lxt.smsencryptor.activity.MessageActivity;
import pers.lxt.smsencryptor.database.Database;
import pers.lxt.smsencryptor.crypto.AESHelper;
import pers.lxt.smsencryptor.crypto.RSAHelper;

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
            SQLiteDatabase db = Database.getInstance(context).getWritableDatabase();
            if(messageBody[1].length()>0&&messageBody[2].length()>0){
                Cursor cursor1 = db.query("key",new String[]{"private_key"},"id = 1",null,null,null,null);
                if(cursor1!=null&&cursor1.moveToNext()){
                    String privateKey = cursor1.getString(0);
                    try {
                        String sessionKey = RSAHelper.decrypt(messageBody[1],privateKey);

                        ContentValues values = new ContentValues();
                        values.put("expire",Long.parseLong(messageBody[2]));
                        values.put("session_key",sessionKey);
                        db.update("contacts",values,"address = ?",new String[]{address});
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    cursor1.close();
                }
            }
            Cursor cursor = db.query("contacts",new String[]{"session_key"},"address = ?",new String[]{address},null,null,null);
            if(cursor!=null&&cursor.moveToNext()){
                String sessionKey = cursor.getString(0);
                try {
                    result = AESHelper.decrypt(messageBody[3],sessionKey);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                cursor.close();
            }
        }
        return result;
    }
}
