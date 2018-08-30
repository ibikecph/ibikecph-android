package dk.kk.ibikecphlib.persist;

import android.content.Context;

import io.realm.Realm;

abstract public class DataSource {

    protected Realm realm;

    public DataSource(Context context) {
        this.realm = Realm.getDefaultInstance();
    }
}
