package ru.ele638.test.sbercounter;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

public class SMSService extends IntentService {

    SMSReceiver receiver;
    SQLHelper dbhelper;

    final private static String TAG = "SmsServieTag";

    private static SMSService instance = null;


    public static boolean isServiceDown() {
        return instance == null;
    }

    public static SMSService getInstance() {
        if (instance == null) {
            instance = new SMSService();
        }
        return instance;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        instance = this;
        dbhelper = new SQLHelper(this);
        if (!SMSReceiver.isReceiverStarted()) {
            receiver = new SMSReceiver();
            registerReceiver(receiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
        }
        if (MainActivity.messages != null) {
            MainActivity.service = this;
            MainActivity.messages.clear();
            MainActivity.messages = getAllMessages();
            MainActivity.rvAdapter.setMessages(MainActivity.messages);
            MainActivity.rvAdapter.notifyDataSetChanged();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service Destroyed");
        if (receiver != null) unregisterReceiver(receiver);
    }

    public void processIncomeSMS(String message) {
        Log.d(TAG, "Got SMS from 900: " + message);

        HashMap<String, Integer> operations = dbhelper.getAllOpers();

        Pattern cardNumber = compile("^([A-Za-z]+\\d+)");
        Pattern dateTime = compile("(\\d+.\\d+.\\d+ \\d+.\\d+)");
        Pattern operTypeSpend = compile("([п,П]окупка|[с,С]писание|[в,В]ыдача|[о,О]плата)");
        Pattern operTypeArrive = compile("([з,З]ачисление|отмена покупки)");

        Pattern summ = compile("(\\d*.\\d*)р");
        Pattern place = compile("\\d*.\\d*р (.*) Баланс:");
        Pattern balance = compile("Баланс: (\\d*.\\d*)р");
        boolean isArrive = false;

        Message newMessage = new Message();
        Matcher matcher = cardNumber.matcher(message);

        try {
            if (matcher.find())
                newMessage.CARD_NUMBER = matcher.group(1);
            matcher = dateTime.matcher(message);
            if (matcher.find()) {
                try {
                    newMessage.DATETIME = new SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault()).parse(matcher.group(1));
                } catch (ParseException e) {
                    e.printStackTrace();
                    newMessage.DATETIME = null;
                }
            }
            matcher = operTypeArrive.matcher(message);
            if (matcher.find())
                isArrive = true;
            matcher = operTypeSpend.matcher(message);
            if (matcher.find())
                isArrive = false;
            newMessage.OPERATION_TYPE = (isArrive ? 2 : 1);
            matcher = summ.matcher(message);
            if (matcher.find())
                newMessage.SUMM = Double.parseDouble(matcher.group(1)) * (isArrive ? 1.0 : -1.0);
            matcher = place.matcher(message);
            if (matcher.find())
                newMessage.PLACE = matcher.group(1);
            matcher = balance.matcher(message);
            if (matcher.find())
                newMessage.BALANCE = Double.parseDouble(matcher.group(1));

            if (dbhelper != null) dbhelper.addValue(newMessage);
        } catch (Exception e) {

        }
    }


    public ArrayList<Message> getAllMessages() {
        ArrayList<Message> messages = new ArrayList<>();
        dbhelper.getAllMessages(messages);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            messages.sort(new Comparator<Message>() {
                @Override
                public int compare(Message message, Message t1) {
                    return (message.DATETIME == null || t1.DATETIME == null) ?
                            0 :
                            t1.DATETIME.compareTo(message.DATETIME);
                }
            });
        }
        return messages;
    }

    public void dropDatabase() {
        dbhelper.dropDatabase();
    }

    public void getLocalMessages() {
        String[] projection = {"address", "body"};
        String selection = "address = \'900\'";

        Cursor cursor = getContentResolver().query(
                Uri.parse("content://sms/inbox"), projection, selection, null, null);
        if (cursor.moveToFirst()) {
            do {
                processIncomeSMS(cursor.getString(cursor.getColumnIndex("body")));

            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    public Integer getLocalMessagesCount(){
        String[] projection = {"address", "body"};
        String selection = "address = \'900\'";

        Cursor cursor = getContentResolver().query(
                Uri.parse("content://sms/inbox"), projection, selection, null, null);
        Integer count = cursor.getCount();
        cursor.close();
        return count;
    }
}
