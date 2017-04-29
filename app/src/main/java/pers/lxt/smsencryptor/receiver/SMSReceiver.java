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
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import pers.lxt.smsencryptor.R;
import pers.lxt.smsencryptor.activity.MessageActivity;
import pers.lxt.smsencryptor.crypto.AESHelper;
import pers.lxt.smsencryptor.crypto.HMACHelper;
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
                        Log.i("收到短信来自:"+msg.getOriginatingAddress(),msg.getMessageBody());
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
        Log.i("收到短信来自:"+address,message);
        String[] messageBody = message.split(",");
        String result = message;
        if(messageBody.length==3 && messageBody[2].length()>0){
            Contacts contact = new Contacts(context).select(address);
            if(contact != null){
                SharedPreferences preferences = context.getSharedPreferences("key",Context.MODE_PRIVATE);
                String privateKey = preferences.getString("private_key",null);
                if(privateKey != null){
                    try {
                        String sessionKey;
                        if(messageBody[1].length()>0){
                            sessionKey = RSAHelper.decrypt(messageBody[1],privateKey);
                        }else{
                            sessionKey = contact.getSessionKey();
                        }

                        if(sessionKey!=null && sessionKey.length()>0){
                            String clip = AESHelper.decrypt(messageBody[2], sessionKey);
                            clip = RSAHelper.decodeSign(clip,contact.getPublicKey());
                            String[] clips = clip.split(",");
                            if(clips.length == 3){
                                if(HMACHelper.sign(clips[1],sessionKey).equals(clips[2])){
                                    if(clips[0]!=null&&clips[0].length()>0){
                                        contact.setSessionKey(sessionKey);
                                        contact.setExpire(Long.parseLong(clips[0]));
                                        contact.update();
                                    }
                                    result = clips[1];
                                }else{
                                    result = "**这条消息很有可能被篡改，谨慎阅读！**" + clips[1];
                                }
                            }else{
                                result = "**这条加密消息被不明人物破坏了**";
                            }
                        }else{
                            contact.setExpire(0L);
                            contact.update();
                            result = "**与对方密钥的同步失败了，可以向对方发送一条消息来重新同步**";
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        contact.setExpire(0L);
                        contact.update();
                        result = "**解密过程中出现异常，可能消息遭到破坏,可以发送消息请对方重新发送**";
                    }
                }else{
                    result = "**你还没有生成密钥，这条消息无法解密（也不知道对面是怎么发给你的**";
                }
            }else{
                result = "**该用户没有在你的列表中登录，这条消息无法解密**";
            }
        }
        return result;
    }
}
