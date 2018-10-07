package ru.ele638.test.sbercounter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

public class SMSReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsReceiverTag";

    private static SMSReceiver instance = null;
    private SMSService service;

    public static boolean isReceiverStarted() {
        return instance != null;
    }

    public SMSReceiver() {
        super();
        instance = this;
        service = SMSService.getInstance();
        Log.d(TAG, "Receiver created");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        SmsMessage[] msgs;
        if (bundle != null) {

            String str = "";
            String sender = "";
            Object[] pdus = (Object[]) bundle.get("pdus");
            assert pdus != null;
            msgs = new SmsMessage[pdus.length];
            for (int i = 0; i < msgs.length; i++) {
                msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                sender = msgs[i].getOriginatingAddress();
                if (sender.equals("900"))
                    str += msgs[i].getMessageBody();
            }
            if (str.length() != 0 && sender.equals("900")) {
                service.processIncomeSMS(str);
            }
        }
    }
}
