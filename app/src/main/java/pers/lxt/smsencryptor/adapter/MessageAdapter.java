package pers.lxt.smsencryptor.adapter;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import pers.lxt.smsencryptor.R;
import pers.lxt.smsencryptor.activity.MessageActivity;

/**
 * Created by MissingNo on 2017/4/10.
 */

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private static final int NOTIFY_ITEM_INSERTED = 0;

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(android.os.Message msg) {
            switch (msg.what){
                case NOTIFY_ITEM_INSERTED:{
                    List<Message> buffer = (List<Message>) msg.obj;

                    int start = msgs.size();
                    msgs.addAll(buffer);
                    notifyItemRangeInserted(start, buffer.size());
                    context.handler.obtainMessage(MessageActivity.SCROLL_RECYCLERVIEW,msgs.size()-1,0).sendToTarget();
                }break;
            }
            return false;
        }
    });

    private static final int LEFT = 0;
    private static final int RIGHT = 1;

    private List<Message> msgs;
    private ContentResolver contentResolver;
    private ContentObserver contentObserver;
    private String phoneNum;
    private MessageActivity context;

    public MessageAdapter(MessageActivity context, String phoneNum){
        super();
        msgs = new ArrayList<>();
        this.phoneNum = phoneNum;
        this.context = context;
        contentResolver = context.getContentResolver();
        contentObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                getSmsInfo();
            }
        };
    }

    private void getSmsInfo() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String[] projection = new String[] { "_id", "body", "date", "type" };
                Cursor cursor = contentResolver.query(Uri.parse("content://sms/"), projection, "address = ?", new String[]{phoneNum},
                        "date");
                if (cursor != null) {
                    int idColumn = cursor.getColumnIndex("_id");
                    int bodyColumn = cursor.getColumnIndex("body");
                    int dateColumn = cursor.getColumnIndex("date");
                    int typeColumn = cursor.getColumnIndex("type");
                    ContentValues contentValues = new ContentValues();
                    contentValues.put("read",1);
                    List<Message> buffer = new ArrayList<>();
                    while (cursor.moveToNext()) {
                        int id = cursor.getInt(idColumn);
                        String body = cursor.getString(bodyColumn);
                        Date date = new Date(cursor.getLong(dateColumn));
                        int type = cursor.getInt(typeColumn) == 1?LEFT:RIGHT;

                        Message msg = new Message(id,body,date,type);
                        if(!msgs.contains(msg)){
                            buffer.add(msg);
                            contentResolver.update(Uri.parse("content://sms/"),contentValues,"_id = ? and read = 0",new String[]{id+""});
                        }
                    }
                    cursor.close();
                    if(buffer.size()>0)
                        handler.obtainMessage(NOTIFY_ITEM_INSERTED, buffer).sendToTarget();
                }
            }
        }).start();
    }

    @Override
    public int getItemViewType(int position) {
        return msgs.get(position).position;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == LEFT){
            return new MessageViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_left,parent,false));
        }else if(viewType == RIGHT){
            return new MessageViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_right,parent,false));
        }else{
            return null;
        }
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        Message msg = msgs.get(position);
        holder.setMessage(msg.msg);
        holder.setTime(msg.time);
    }

    @Override
    public int getItemCount() {
        return msgs.size();
    }

    public void load(){
        getSmsInfo();
        contentResolver.registerContentObserver(Uri.parse("content://sms/"), true, contentObserver);
    }

    public void unload(){
        contentResolver.unregisterContentObserver(contentObserver);
    }

    private class Message{
        int id;
        String msg;
        Date time;
        int position;

        Message(int id, String msg,Date time,int position){
            this.id = id;
            this.position = position;
            this.msg = msg;
            this.time = time;
        }

        @Override
        public boolean equals(Object obj) {
            if(obj instanceof Message){
                return id == ((Message)obj).id;
            }else{
                return false;
            }
        }
    }

    class MessageViewHolder extends RecyclerView.ViewHolder{
        private TextView time;
        private TextView message;
        private DateFormat format;

        public MessageViewHolder(View itemView) {
            super(itemView);
            time = (TextView) itemView.findViewById(R.id.time);
            message = (TextView) itemView.findViewById(R.id.message);
            format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        }

        public void setMessage(String msg){
            message.setText(msg);
        }

        public void setTime(Date time){
            this.time.setText(format.format(time));
        }
    }
}
