package pers.lxt.smsencryptor.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by MissingNo on 2017/4/14.
 */

public class Database extends SQLiteOpenHelper {

    private static final String DB_NAME = "smsencryptor.db";
    private static final int VERSION = 1;

    private static Database instance;
    public static Database getInstance(Context context){
        if(instance == null){
            instance = new Database(context);
        }
        return instance;
    }

    private Database(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table contacts(address varchar(20) not null, name varchar(30), public_key varchar(60),primary key(address));");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}