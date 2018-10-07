package ru.ele638.test.sbercounter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class SQLHelper extends SQLiteOpenHelper {

    static final String TAG = "SMSSQLHelper";

    private static final String FILENAME = "sberCount.db";

    //messages table
    private static final String TABLE_NAME = "messages";
    static final String ID = "id";
    static final String CARD_NUMBER = "card_number";
    static final String DATETIME = "datetime";
    static final String OPERATION_TYPE = "oper_type_id";
    static final String SUMM = "summ";
    static final String PLACE = "place";
    static final String BALANCE = "balance";

    //oper_type table
    private static final String OP_TABLE_NAME = "oper_type";
    private static final String OP_ID = "id";
    private static final String OP_NAME = "name";
    private static final String OP_VALUES = "(\'списание\'), (\'пополнение\')";


    SQLHelper(Context ctx) {
        super(ctx, FILENAME, null, 3, null);
        getReadableDatabase();
        Log.d(TAG, "Started");
    }

    private void recreateDB(SQLiteDatabase db){
        db.execSQL(String.format("DROP TABLE IF EXISTS %s", OP_TABLE_NAME));
        db.execSQL(String.format("CREATE TABLE %s (" +
                        "%s INTEGER PRIMARY KEY, " +
                        "%s TEXT);",
                OP_TABLE_NAME,
                OP_ID,
                OP_NAME));
        db.execSQL(String.format("INSERT INTO %s (\'%s\') VALUES %s;",
                OP_TABLE_NAME, OP_NAME, OP_VALUES));
        db.execSQL(String.format("DROP TABLE IF EXISTS %s", TABLE_NAME)); //TEST
        db.execSQL(String.format("CREATE TABLE %s (" +
                        "%s INTEGER PRIMARY KEY, " +
                        "%s TEXT, " +
                        "%s DATETIME," +
                        "%s INTEGER, " +
                        "%s REAL, " +
                        "%s TEXT, " +
                        "%s REAL, " +
                        "FOREIGN KEY(%s) REFERENCES %s(%s));",
                TABLE_NAME, ID, CARD_NUMBER, DATETIME, OPERATION_TYPE,
                SUMM, PLACE, BALANCE, OPERATION_TYPE, OP_TABLE_NAME, OP_ID));
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Helper started");
        recreateDB(db);
        Log.d(TAG, "CREATED TABLES");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        onCreate(db);
    }

    public HashMap<String, Integer> getAllOpers() {
        SQLiteDatabase db = getReadableDatabase();
        HashMap<String, Integer> out = new HashMap<>();
        if (db != null) {
            Cursor cursor = db.query(OP_TABLE_NAME, null, null, null, null, null, null);
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                do {
                    out.put(cursor.getString(cursor.getColumnIndex(OP_NAME)), cursor.getInt(cursor.getColumnIndex(OP_ID)));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return out;
    }

    public void getAllMessages(ArrayList<Message> messages) {
        SQLiteDatabase db = getReadableDatabase();
        if (db != null && messages != null) {
            Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, null);
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                do {
                    messages.add(new Message(cursor));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
    }

    public void addValue(Message s) {
        String value = "";
        if (s.DATETIME != null ) value = new SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault()).format(s.DATETIME);
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(CARD_NUMBER, s.CARD_NUMBER);
        values.put(DATETIME, value);
        values.put(OPERATION_TYPE, s.OPERATION_TYPE);
        values.put(SUMM, s.SUMM);
        values.put(PLACE, s.PLACE);
        values.put(BALANCE, s.BALANCE);
        db.insert(TABLE_NAME, null, values);
        Log.d(TAG, "Added row to database");
    }

    public void dropDatabase() {
        SQLiteDatabase db = getWritableDatabase();
        recreateDB(db);
    }
}
