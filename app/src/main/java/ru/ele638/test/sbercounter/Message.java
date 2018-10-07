package ru.ele638.test.sbercounter;

import android.database.Cursor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Message {

    public Integer ID;
    public String CARD_NUMBER;
    public Date DATETIME;
    public Integer OPERATION_TYPE;
    public Double SUMM;
    public String PLACE;
    public Double BALANCE;

    public Message(String CARD_NUMBER, Date DATETIME, Integer OPERATION_TYPE, Double SUMM, String PLACE, Double BALANCE) {
        this.CARD_NUMBER = CARD_NUMBER;
        this.DATETIME = DATETIME;
        this.OPERATION_TYPE = OPERATION_TYPE;
        this.SUMM = SUMM;
        this.PLACE = PLACE;
        this.BALANCE = BALANCE;
    }

    public Message() {
    }

    public Message(Cursor cursor) {
        this.ID = cursor.getInt(cursor.getColumnIndexOrThrow(SQLHelper.ID));
        this.CARD_NUMBER = cursor.getString(cursor.getColumnIndexOrThrow(SQLHelper.CARD_NUMBER));
        try {
            this.DATETIME = new SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault()).parse(cursor.getString(cursor.getColumnIndex(SQLHelper.DATETIME)));
        } catch (ParseException e) {
            this.DATETIME = null;
        }
        this.OPERATION_TYPE = cursor.getInt(cursor.getColumnIndex(SQLHelper.OPERATION_TYPE));
        this.SUMM = cursor.getDouble(cursor.getColumnIndex(SQLHelper.SUMM));
        this.PLACE = cursor.getString(cursor.getColumnIndex(SQLHelper.PLACE));
        this.BALANCE = cursor.getDouble(cursor.getColumnIndex(SQLHelper.BALANCE));
    }
}
