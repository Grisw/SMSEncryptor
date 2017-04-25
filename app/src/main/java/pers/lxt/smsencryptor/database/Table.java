package pers.lxt.smsencryptor.database;

import android.content.Context;
import android.support.annotation.Nullable;

/**
 * Created by MissingNo on 2017/4/24.
 */

public abstract class Table {

    protected Context context;

    public Table(Context context){
        this.context = context;
    }

    public abstract boolean update();

    public abstract boolean insert();

    @Nullable
    public abstract Table select(String id);

}
