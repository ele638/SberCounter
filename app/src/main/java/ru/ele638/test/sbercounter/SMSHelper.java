package ru.ele638.test.sbercounter;

import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

public class SMSHelper {

    final private static String TAG = "SmsServieTag";


    public static void processIncomeSMS(SQLHelper dbHelper, String message) {
        Log.d(TAG, "Got SMS from 900: " + message);

        //HashMap<String, Integer> operations = dbHelper.getAllOpers();

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

            dbHelper.addValue(newMessage);
            if (MainActivity.getRvAdapter() != null) {
                addMessage(MainActivity.getRvAdapter(), newMessage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void getAllMessages(SQLHelper dbHelper, RVAdapter adapter) {
        ArrayList<Message> messages = RVAdapter.getMessages();
        dbHelper.getAllMessages(messages);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (messages != null) {
                messages.sort(new Comparator<Message>() {
                    @Override
                    public int compare(Message message, Message t1) {
                        return (message.DATETIME == null || t1.DATETIME == null) ?
                                0 :
                                t1.DATETIME.compareTo(message.DATETIME);
                    }
                });
            }
        }
        adapter.notifyDataSetChanged();
    }

    public static void addMessage(RVAdapter adapter, Message message) {
        ArrayList<Message> messages = RVAdapter.getMessages();
        messages.add(0, message);
        adapter.notifyItemInserted(0);
    }

    public static void dropDatabase(SQLHelper dbHelper) {
        dbHelper.dropDatabase();
    }

    public static void getLocalMessages(SQLHelper dbHelper) {
        String[] projection = {"address", "body"};
        String selection = "address = \'900\'";

        Cursor cursor = dbHelper.getContext().getContentResolver().query(
                Uri.parse("content://sms/inbox"), projection, selection, null, null);
        if (cursor.moveToFirst()) {
            do {
                processIncomeSMS(dbHelper, cursor.getString(cursor.getColumnIndex("body")));

            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    public static Integer getLocalMessagesCount(SQLHelper dbHelper) {
        String[] projection = {"address", "body"};
        String selection = "address = \'900\'";

        Cursor cursor = dbHelper.getContext().getContentResolver().query(
                Uri.parse("content://sms/inbox"), projection, selection, null, null);
        Integer count = cursor.getCount();
        cursor.close();
        return count;
    }
}
