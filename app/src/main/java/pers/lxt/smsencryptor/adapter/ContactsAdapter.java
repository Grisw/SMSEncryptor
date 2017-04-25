package pers.lxt.smsencryptor.adapter;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import pers.lxt.smsencryptor.R;
import pers.lxt.smsencryptor.activity.MessageActivity;
import pers.lxt.smsencryptor.database.Contacts;

/**
 * Created by MissingNo on 2017/4/9.
 */

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactsViewHolder> {

    private static final int NOTIFY_ITEM_CHANGED = 0;

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case NOTIFY_ITEM_CHANGED:{
                    List<PhoneNumPair> buffer = (List<PhoneNumPair>) msg.obj;

                    for(PhoneNumPair pair : buffer) {
                        int pos = contacts.indexOf(pair);
                        if(pos != -1){
                            contacts.get(pos).isNotify = pair.isNotify;
                            notifyItemChanged(pos);
                            if(pair.isNotify){
                                contacts.add(0,contacts.remove(pos));
                                notifyItemMoved(pos,0);
                            }
                        }else{
                            contacts.add(pair);
                            notifyItemInserted(contacts.size()-1);
                        }
                    }
                }break;
            }
            return false;
        }
    });

    private Context context;
    private List<PhoneNumPair> contacts;
    private ContentResolver contentResolver;
    private ContentObserver contentObserver;

    public ContactsAdapter(Context context){
        super();
        contacts = new ArrayList<>();
        contentResolver = context.getContentResolver();
        contentObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                getSmsInfo();
            }
        };
        this.context = context;
    }

    private void getSmsInfo() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<PhoneNumPair> buffer = new ArrayList<>();
                String[] projection = new String[] { "address", "person", "read" };
                Cursor cursor = contentResolver.query(Uri.parse("content://sms/"), projection, null, null,
                        "read, date desc");
                if (cursor != null) {
                    for(int i = 0;i<contacts.size();i++){
                        contacts.get(i).isNotify = false;
                    }
                    int nameColumn = cursor.getColumnIndex("person");
                    int phoneNumberColumn = cursor.getColumnIndex("address");
                    int readColumn = cursor.getColumnIndex("read");
                    while (cursor.moveToNext()) {
                        String phoneNum = cursor.getString(phoneNumberColumn);
                        String name = cursor.getString(nameColumn);
                        int read = cursor.getInt(readColumn);

                        PhoneNumPair pair = new PhoneNumPair(name, phoneNum, read == 0);
                        int pos = buffer.indexOf(pair);
                        if(pos != -1){
                            buffer.get(pos).isNotify |= read == 0;
                        }else{
                            buffer.add(pair);
                        }
                    }
                    cursor.close();
                }

                List<Contacts> contacts = Contacts.getAllContacts(context);
                for(Contacts contact : contacts){
                    PhoneNumPair pair = new PhoneNumPair(contact.getName(), contact.getAddress(), false);
                    if(!buffer.contains(pair)){
                        buffer.add(pair);
                    }
                }
                if(buffer.size()>0)
                    handler.obtainMessage(NOTIFY_ITEM_CHANGED,buffer).sendToTarget();
            }
        }).start();
    }

    @Override
    public ContactsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ContactsViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact,parent,false));
    }

    @Override
    public void onBindViewHolder(ContactsViewHolder holder, int position) {
        final PhoneNumPair pair = contacts.get(position);
        holder.setNotify(pair.isNotify);
        holder.setPhoneNum(pair.name == null?pair.phoneNum:pair.name);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), MessageActivity.class);
                intent.putExtra("phone_num",pair.phoneNum);
                v.getContext().startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public void load(){
        getSmsInfo();
        contentResolver.registerContentObserver(Uri.parse("content://sms/"), true, contentObserver);
    }

    public void unload(){
        contentResolver.unregisterContentObserver(contentObserver);
    }

    private class PhoneNumPair{
        String phoneNum;
        String name;
        boolean isNotify;

        PhoneNumPair(String name, String phoneNum, boolean isNotify){
            this.name=name;
            this.isNotify = isNotify;
            this.phoneNum = phoneNum;
        }

        @Override
        public boolean equals(Object obj) {
            if(obj instanceof PhoneNumPair){
                return phoneNum.equals(((PhoneNumPair) obj).phoneNum);
            }else{
                return false;
            }
        }
    }

    class ContactsViewHolder extends RecyclerView.ViewHolder{

        private TextView phoneNum;
        private ImageView notify;

        ContactsViewHolder(View itemView) {
            super(itemView);
            phoneNum = (TextView) itemView.findViewById(R.id.phone_num);
            notify = (ImageView) itemView.findViewById(R.id.notify);
        }

        void setPhoneNum(String phoneNum){
            this.phoneNum.setText(phoneNum);
        }

        void setNotify(boolean isNotify){
            this.notify.setVisibility(isNotify?View.VISIBLE:View.INVISIBLE);
        }
    }
}
