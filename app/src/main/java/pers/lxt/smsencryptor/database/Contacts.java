package pers.lxt.smsencryptor.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by MissingNo on 2017/4/24.
 */

public class Contacts extends Table {

    private static final String TABLE_NAME = "contacts";

    private String address;
    private String name;
    private String publicKey;
    private String sessionKey;
    private Long expire;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public Long getExpire() {
        return expire;
    }

    public void setExpire(Long expire) {
        this.expire = expire;
    }

    public Contacts(Context context) {
        super(context);
    }

    @Override
    public boolean update() {
        SQLiteDatabase database = Database.getWritableDatabase(context);
        ContentValues values = new ContentValues();
        if(name != null)
            values.put("name",name);
        if(publicKey != null)
            values.put("public_key",publicKey);
        if(sessionKey != null)
            values.put("session_key",sessionKey);
        if(expire != null)
            values.put("expire", expire);
        int rows = database.update(TABLE_NAME,values,"address = ?",new String[]{address});
        database.close();
        return rows == 1;
    }

    @Override
    public boolean insert() {
        if(address == null)
            return false;
        SQLiteDatabase database = Database.getWritableDatabase(context);
        ContentValues values = new ContentValues();
        values.put("address",address);
        if(name != null)
            values.put("name",name);
        if(publicKey != null)
            values.put("public_key",publicKey);
        if(sessionKey != null)
            values.put("session_key",sessionKey);
        if(expire != null)
            values.put("expire", expire);
        long rowID = database.insert(TABLE_NAME,null,values);
        database.close();
        return rowID != -1;
    }

    @Override
    public Contacts select(String id) {
        SQLiteDatabase database = Database.getReadableDatabase(context);
        Cursor cursor = database.query(TABLE_NAME,new String[]{"name","public_key","session_key","expire"},"address = ?",new String[]{id},null,null,null);
        if(cursor!=null && cursor.moveToFirst()){
            address = id;
            name = cursor.getString(cursor.getColumnIndex("name"));
            publicKey = cursor.getString(cursor.getColumnIndex("public_key"));
            sessionKey = cursor.getString(cursor.getColumnIndex("session_key"));
            expire = cursor.getLong(cursor.getColumnIndex("expire"));
            cursor.close();
            database.close();
            return this;
        }else{
            database.close();
            return null;
        }
    }

    public static List<Contacts> getAllContacts(Context context){
        List<Contacts> list = new ArrayList<>();
        SQLiteDatabase database = Database.getReadableDatabase(context);
        Cursor cursor = database.query(TABLE_NAME, new String[]{"address","name"},null,null,null,null,null);
        if(cursor!=null){
            int phoneNumberColumn = cursor.getColumnIndex("address");
            int nameColumn = cursor.getColumnIndex("name");
            while(cursor.moveToNext()){
                String phoneNum = cursor.getString(phoneNumberColumn);
                String name = cursor.getString(nameColumn);

                Contacts contact = new Contacts(context);
                contact.setAddress(phoneNum);
                contact.setName(name);
                list.add(contact);
            }
            cursor.close();
        }
        database.close();
        return list;
    }
}
